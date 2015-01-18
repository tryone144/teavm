/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.eclipse;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.core.*;
import org.teavm.callgraph.CallGraph;
import org.teavm.callgraph.CallGraphNode;
import org.teavm.callgraph.CallSite;
import org.teavm.diagnostics.Problem;
import org.teavm.diagnostics.ProblemTextConsumer;
import org.teavm.model.*;
import org.teavm.tooling.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMProjectBuilder extends IncrementalProjectBuilder {
    private static final int TICKS_PER_PROFILE = 10000;
    private URL[] classPath;
    private IContainer[] sourceContainers;
    private IContainer[] classFileContainers;
    private SourceFileProvider[] sourceProviders;
    private Set<IProject> usedProjects = new HashSet<>();
    private static Map<TeaVMProfile, Set<String>> profileClasses = new WeakHashMap<>();
    private static Pattern newLinePattern = Pattern.compile("\\r|\\n|\\r\\n");

    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        TeaVMProjectSettings projectSettings = getProjectSettings();
        projectSettings.load();
        TeaVMProfile profiles[] = getEnabledProfiles(projectSettings);
        monitor.beginTask("Running TeaVM", profiles.length * TICKS_PER_PROFILE);
        try {
            prepareClassPath();
            removeMarkers();
            ClassLoader classLoader = new URLClassLoader(classPath, TeaVMProjectBuilder.class.getClassLoader());
            for (TeaVMProfile profile : profiles) {
                SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, TICKS_PER_PROFILE);
                buildProfile(kind, subMonitor, profile, classLoader);
            }

        } finally {
            monitor.done();
            sourceContainers = null;
            classFileContainers = null;
            classPath = null;
            usedProjects.clear();
        }
        return !usedProjects.isEmpty() ? usedProjects.toArray(new IProject[0]) : null;
    }

    private TeaVMProfile[] getEnabledProfiles(TeaVMProjectSettings settings) {
        TeaVMProfile[] profiles = settings.getProfiles();
        int sz = 0;
        for (int i = 0; i < profiles.length; ++i) {
            TeaVMProfile profile = profiles[i];
            if (profile.isEnabled()) {
                profiles[sz++] = profile;
            }
        }
        return Arrays.copyOf(profiles, sz);
    }

    private void buildProfile(int kind, IProgressMonitor monitor, TeaVMProfile profile, ClassLoader classLoader)
            throws CoreException {
        if ((kind == AUTO_BUILD || kind == INCREMENTAL_BUILD) && !shouldBuild(profile)) {
            return;
        }
        IStringVariableManager varManager = VariablesPlugin.getDefault().getStringVariableManager();
        TeaVMTool tool = new TeaVMTool();
        tool.setClassLoader(classLoader);
        tool.setDebugInformationGenerated(profile.isDebugInformationGenerated());
        tool.setSourceMapsFileGenerated(profile.isSourceMapsGenerated());
        tool.setSourceFilesCopied(profile.isSourceFilesCopied());
        String targetDir = profile.getTargetDirectory();
        tool.setTargetDirectory(new File(varManager.performStringSubstitution(targetDir, false)));
        tool.setTargetFileName(profile.getTargetFileName());
        tool.setMinifying(profile.isMinifying());
        tool.setRuntime(mapRuntime(profile.getRuntimeMode()));
        tool.setMainClass(profile.getMainClass());
        tool.getProperties().putAll(profile.getProperties());
        tool.setIncremental(profile.isIncremental());
        String cacheDir = profile.getCacheDirectory();
        tool.setCacheDirectory(!cacheDir.isEmpty() ?
                new File(varManager.performStringSubstitution(cacheDir, false)) : null);
        for (ClassHolderTransformer transformer : instantiateTransformers(profile, classLoader)) {
            tool.getTransformers().add(transformer);
        }
        for (Map.Entry<String, String> entry : profile.getClassAliases().entrySet()) {
            ClassAlias classAlias = new ClassAlias();
            classAlias.setClassName(entry.getKey());
            classAlias.setAlias(entry.getValue());
            tool.getClassAliases().add(classAlias);
        }
        for (SourceFileProvider provider : sourceProviders) {
            tool.addSourceFileProvider(provider);
        }
        tool.setProgressListener(new TeaVMEclipseProgressListener(this, monitor, TICKS_PER_PROFILE));
        try {
            monitor.beginTask("Running TeaVM", 10000);
            tool.generate();
            if (!tool.wasCancelled()) {
                putMarkers(tool.getDependencyInfo().getCallGraph(), tool.getProblemProvider().getProblems());
                setClasses(profile, classesToResources(tool.getClasses()));
                refreshTarget(tool.getTargetDirectory());
            }
            if (!monitor.isCanceled()) {
                monitor.done();
            }
        } catch (TeaVMToolException e) {
            throw new CoreException(TeaVMEclipsePlugin.makeError(e));
        }
    }

    private void refreshTarget(File targetDirectory) {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IContainer[] targetContainers = workspaceRoot.findContainersForLocationURI(targetDirectory.toURI());
        for (final IContainer container : targetContainers) {
            if (container.exists()) {
                Job job = new Job("Refreshing target directory") {
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            container.refreshLocal(IContainer.DEPTH_INFINITE, monitor);
                        } catch (CoreException e) {
                            TeaVMEclipsePlugin.logError(e);
                            return TeaVMEclipsePlugin.makeError(e);
                        }
                        return Status.OK_STATUS;
                    }
                };
                job.schedule();
             }
         }
    }

    private RuntimeCopyOperation mapRuntime(TeaVMRuntimeMode runtimeMode) {
        switch (runtimeMode) {
            case MERGE:
                return RuntimeCopyOperation.MERGED;
            case SEPARATE:
                return RuntimeCopyOperation.SEPARATE;
            default:
                return RuntimeCopyOperation.NONE;
        }
    }

    private Set<String> classesToResources(Collection<String> classNames) {
        Set<String> resourcePaths = new HashSet<>();
        for (String className : classNames) {
            for (IContainer clsContainer : classFileContainers) {
                IResource res = clsContainer.findMember(className.replace('.', '/') + ".class");
                if (res != null) {
                    resourcePaths.add(res.getFullPath().toString());
                    usedProjects.add(res.getProject());
                }
            }
        }
        return resourcePaths;
    }

    private boolean shouldBuild(TeaVMProfile profile) throws CoreException {
        Collection<String> classes = getClasses(profile);
        if (classes.isEmpty()) {
            return true;
        }
        for (IProject project : getRelatedProjects()) {
            IResourceDelta delta = getDelta(project);
            if (delta != null && shouldBuild(classes, delta)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldBuild(Collection<String> classes, IResourceDelta delta) {
        if (classes.contains(delta.getResource().getFullPath().toString())) {
            return true;
        }
        for (IResourceDelta child : delta.getAffectedChildren()) {
            if (shouldBuild(classes, child)) {
                return true;
            }
        }
        return false;
    }

    private Collection<String> getClasses(TeaVMProfile profile) {
        Set<String> classes;
        synchronized (profileClasses) {
            classes = profileClasses.get(profile);
        }
        return classes != null ? new HashSet<>(classes) : new HashSet<String>();
    }

    private void setClasses(TeaVMProfile profile, Collection<String> classes) {
        profileClasses.put(profile, new HashSet<>(classes));
    }

    private void removeMarkers() throws CoreException {
        for (IProject project : getProject().getWorkspace().getRoot().getProjects()) {
            IMarker[] markers = project.findMarkers(TeaVMEclipsePlugin.PROBLEM_MARKER_ID, true,
                    IResource.DEPTH_INFINITE);
            for (IMarker marker : markers) {
                String projectName = (String)marker.getAttribute(TeaVMEclipsePlugin.PROBLEM_MARKER_PROJECT_ATTRIBUTE);
                if (projectName.equals(getProject().getName())) {
                    marker.delete();
                }
            }
        }
        getProject().deleteMarkers(TeaVMEclipsePlugin.CONFIG_MARKER_ID, true, IResource.DEPTH_INFINITE);
    }

    private void putMarkers(CallGraph cg, List<Problem> problems) throws CoreException {
        for (Problem problem : problems) {
            putMarker(cg, problem);
        }
    }

    private void putMarker(CallGraph cg, Problem problem) throws CoreException {
        if (problem.getLocation() == null || problem.getLocation().getMethod() == null) {
            putMarkerAtDefaultLocation(problem);
            return;
        }
        CallGraphNode problemNode = cg.getNode(problem.getLocation().getMethod());
        if (problemNode == null) {
            putMarkerAtDefaultLocation(problem);
            return;
        }
        Set<CallSite> callSites = new HashSet<>();
        collectCallSites(callSites, problemNode);
        String messagePrefix = problemAsString(problem);
        boolean wasPut = false;
        for (CallSite callSite : callSites) {
            IResource resource = findResource(new CallLocation(callSite.getCaller().getMethod(),
                    callSite.getLocation()));
            if (resource == null) {
                continue;
            }
            wasPut = true;
            CallGraphNode node = problemNode;
            StringBuilder sb = new StringBuilder(messagePrefix);
            while (node != callSite.getCaller()) {
                sb.append(", used by ").append(getFullMethodName(node.getMethod()));
                Iterator<? extends CallSite> callerCallSites = node.getCallerCallSites().iterator();
                if (!callerCallSites.hasNext()) {
                    break;
                }
                node = callerCallSites.next().getCaller();
            }
            putMarker(resource, callSite.getLocation(), callSite.getCaller().getMethod(), sb.toString());
        }
        IResource resource = findResource(problem.getLocation());
        if (resource != null) {
            putMarker(resource, problem.getLocation().getSourceLocation(), problem.getLocation().getMethod(),
                    messagePrefix);
            wasPut = true;
        }
        if (!wasPut) {
            putMarkerAtDefaultLocation(problem);
        }
    }

    private void putMarker(IResource resource, InstructionLocation location, MethodReference method,
            String text) throws CoreException {
        IMarker marker = resource.createMarker(TeaVMEclipsePlugin.PROBLEM_MARKER_ID);
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
        marker.setAttribute(IMarker.MESSAGE, text);
        marker.setAttribute(TeaVMEclipsePlugin.PROBLEM_MARKER_PROJECT_ATTRIBUTE, getProject().getName());
        Integer lineNumber = location != null ? location.getLine() : null;
        if (lineNumber == null) {
            lineNumber = findMethodLocation(method, resource);
        }
        if (lineNumber == null) {
            lineNumber = 1;
        }
        marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
    }

    private IResource findResource(CallLocation location) {
        IResource resource = null;
        if (location.getSourceLocation() != null) {
            String resourceName = location.getSourceLocation().getFileName();
            for (IContainer container : sourceContainers) {
                resource = container.findMember(resourceName);
                if (resource != null) {
                    break;
                }
            }
        }
        if (resource == null) {
            String resourceName = location.getMethod().getClassName().replace('.', '/') + ".java";
            for (IContainer container : sourceContainers) {
                resource = container.findMember(resourceName);
                if (resource != null) {
                    break;
                }
            }
        }
        return resource;
    }

    private void collectCallSites(Set<CallSite> callSites, CallGraphNode node) {
        for (CallSite callSite : node.getCallerCallSites()) {
            if (callSites.add(callSite)) {
                collectCallSites(callSites, callSite.getCaller());
            }
        }
    }

    private void putMarkerAtDefaultLocation(Problem problem) throws CoreException {
        IMarker marker = getProject().createMarker(TeaVMEclipsePlugin.PROBLEM_MARKER_ID);
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
        marker.setAttribute(IMarker.MESSAGE, problemAsString(problem));
        marker.setAttribute(TeaVMEclipsePlugin.PROBLEM_MARKER_PROJECT_ATTRIBUTE, getProject().getName());
    }

    private Integer findMethodLocation(MethodReference methodRef, IResource resource) throws CoreException {
        if (resource.getType() != IResource.FILE) {
            return null;
        }
        IJavaElement rootElement = JavaCore.createCompilationUnitFrom((IFile)resource);
        if (rootElement.getElementType() != IJavaElement.COMPILATION_UNIT) {
            return null;
        }
        ICompilationUnit unit = (ICompilationUnit)rootElement;
        IType type = unit.getType(getSimpleClassName(methodRef.getClassName()));
        if (type == null) {
            return null;
        }
        for (IMethod method : type.getMethods()) {
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            for (String paramType : method.getParameterTypes()) {
                sb.append(Signature.getTypeErasure(paramType));
            }
            sb.append(')').append(Signature.getTypeErasure(method.getReturnType()));
            if (sb.toString().equals(methodRef.toString())) {
                return getLineNumber(method);
            }
        }
        return null;
    }

    private int getLineNumber(IMethod method) throws CoreException {
        int offset = method.getSourceRange().getOffset();
        Matcher matcher = newLinePattern.matcher(method.getCompilationUnit().getSource());
        int lineNumber = 1;
        while (matcher.find() && matcher.start() < offset) {
            ++lineNumber;
        }
        return lineNumber;
    }

    private String problemAsString(Problem problem) {
        final StringBuilder sb = new StringBuilder();
        problem.render(new ProblemTextConsumer() {
            @Override public void appendMethod(MethodReference method) {
                sb.append(getFullMethodName(method));
            }
            @Override public void appendLocation(InstructionLocation location) {
                sb.append(location);
            }
            @Override public void appendField(FieldReference field) {
                sb.append(getFullFieldName(field));
            }
            @Override public void appendClass(String cls) {
                sb.append(getSimpleClassName(cls));
            }
            @Override public void append(String text) {
                sb.append(text);
            }
        });
        return sb.toString();
    }


    private String getFullMethodName(MethodReference methodRef) {
        StringBuilder sb = new StringBuilder();
        sb.append(getSimpleClassName(methodRef.getClassName())).append('.').append(methodRef.getName()).append('(');
        if (methodRef.getDescriptor().parameterCount() > 0) {
            sb.append(getTypeName(methodRef.getDescriptor().parameterType(0)));
            for (int i = 1; i < methodRef.getDescriptor().parameterCount(); ++i) {
                sb.append(',').append(getTypeName(methodRef.getDescriptor().parameterType(i)));
            }
        }
        sb.append(')');
        return sb.toString();
    }

    private String getFullFieldName(FieldReference fieldRef) {
        return getSimpleClassName(fieldRef.getClassName()) + "." + fieldRef.getFieldName();
    }

    private String getTypeName(ValueType type) {
        int arrayDim = 0;
        while (type instanceof ValueType.Array) {
            ValueType.Array array = (ValueType.Array)type;
            type = array.getItemType();
        }
        StringBuilder sb = new StringBuilder();
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive)type).getKind()) {
                case BOOLEAN:
                    sb.append("boolean");
                    break;
                case BYTE:
                    sb.append("byte");
                    break;
                case CHARACTER:
                    sb.append("char");
                    break;
                case SHORT:
                    sb.append("short");
                    break;
                case INTEGER:
                    sb.append("int");
                    break;
                case LONG:
                    sb.append("long");
                    break;
                case FLOAT:
                    sb.append("float");
                    break;
                case DOUBLE:
                    sb.append("double");
                    break;
            }
        } else if (type instanceof ValueType.Object) {
            ValueType.Object cls = (ValueType.Object)type;
            sb.append(getSimpleClassName(cls.getClassName()));
        }
        while (arrayDim-- > 0) {
            sb.append("[]");
        }
        return sb.toString();
    }

    private String getSimpleClassName(String className) {
        int index = className.lastIndexOf('.');
        return className.substring(index + 1);
    }

    private TeaVMProjectSettings getProjectSettings() {
        return TeaVMEclipsePlugin.getDefault().getSettings(getProject());
    }

    private Set<IProject> getRelatedProjects() throws CoreException {
        Set<IProject> projects = new HashSet<>();
        Set<IProject> visited = new HashSet<>();
        Queue<IProject> queue = new ArrayDeque<>();
        queue.add(getProject());
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        while (!queue.isEmpty()) {
            IProject project = queue.remove();
            if (!visited.add(project) || !project.hasNature(JavaCore.NATURE_ID)) {
                continue;
            }
            projects.add(project);
            IJavaProject javaProject = JavaCore.create(project);
            for (IClasspathEntry entry : javaProject.getRawClasspath()) {
                if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                    project = (IProject)root.findMember(entry.getPath());
                    queue.add(project);
                }
            }
        }
        return projects;
    }

    private List<ClassHolderTransformer> instantiateTransformers(TeaVMProfile profile, ClassLoader classLoader)
            throws CoreException{
        List<ClassHolderTransformer> transformerInstances = new ArrayList<>();
        for (String transformerName : profile.getTransformers()) {
            Class<?> transformerRawType;
            try {
                transformerRawType = Class.forName(transformerName, true, classLoader);
            } catch (ClassNotFoundException e) {
                putConfigMarker("Transformer not found: " + transformerName);
                continue;
            }
            if (!ClassHolderTransformer.class.isAssignableFrom(transformerRawType)) {
                putConfigMarker("Transformer " + transformerName + " is not a subtype of " +
                        ClassHolderTransformer.class.getName());
                continue;
            }
            Class<? extends ClassHolderTransformer> transformerType = transformerRawType.asSubclass(
                    ClassHolderTransformer.class);
            Constructor<? extends ClassHolderTransformer> ctor;
            try {
                ctor = transformerType.getConstructor();
            } catch (NoSuchMethodException e) {
                putConfigMarker("Transformer " + transformerName + " has no default constructor");
                continue;
            }
            try {
                ClassHolderTransformer transformer = ctor.newInstance();
                transformerInstances.add(transformer);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                putConfigMarker("Error instantiating transformer " + transformerName);
                continue;
            }
        }
        return transformerInstances;
    }

    private void putConfigMarker(String message) throws CoreException {
        IMarker marker = getProject().createMarker(TeaVMEclipsePlugin.CONFIG_MARKER_ID);
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
        marker.setAttribute(IMarker.MESSAGE, message);
        marker.setAttribute(IMarker.LOCATION, getProject().getName() + " project");
    }

    private void prepareClassPath() throws CoreException {
        classPath = new URL[0];
        sourceContainers = new IContainer[0];
        sourceProviders = new SourceFileProvider[0];
        IProject project = getProject();
        if (!project.hasNature(JavaCore.NATURE_ID)) {
            return;
        }
        IJavaProject javaProject = JavaCore.create(project);
        PathCollector collector = new PathCollector();
        SourcePathCollector srcCollector = new SourcePathCollector();
        SourcePathCollector binCollector = new SourcePathCollector();
        SourceFileCollector sourceFileCollector = new SourceFileCollector();
        IWorkspaceRoot workspaceRoot = project.getWorkspace().getRoot();
        try {
            if (javaProject.getOutputLocation() != null) {
                IContainer container = (IContainer)workspaceRoot.findMember(javaProject.getOutputLocation());
                collector.addPath(container.getLocation());
                binCollector.addContainer(container);
            }
        } catch (MalformedURLException e) {
            TeaVMEclipsePlugin.logError(e);
        }
        Queue<IJavaProject> projectQueue = new ArrayDeque<>();
        projectQueue.add(javaProject);
        Set<IJavaProject> visitedProjects = new HashSet<>();
        while (!projectQueue.isEmpty()) {
            javaProject = projectQueue.remove();
            if (!visitedProjects.add(javaProject)) {
                continue;
            }
            IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);
            for (IClasspathEntry entry : entries) {
                switch (entry.getEntryKind()) {
                    case IClasspathEntry.CPE_LIBRARY:
                        try {
                            collector.addPath(entry.getPath());
                        } catch (MalformedURLException e) {
                            TeaVMEclipsePlugin.logError(e);
                        }
                        if (entry.getSourceAttachmentPath() != null) {
                            sourceFileCollector.addFile(entry.getSourceAttachmentPath());
                        }
                        break;
                    case IClasspathEntry.CPE_SOURCE:
                        if (entry.getOutputLocation() != null) {
                            try {
                                IResource res = workspaceRoot.findMember(entry.getOutputLocation());
                                if (res != null) {
                                    collector.addPath(res.getLocation());
                                }
                            } catch (MalformedURLException e) {
                                TeaVMEclipsePlugin.logError(e);
                            }
                        }
                        IContainer srcContainer = (IContainer)workspaceRoot.findMember(entry.getPath());
                        if (srcContainer != null) {
                            srcCollector.addContainer(srcContainer);
                            sourceFileCollector.addFile(srcContainer.getLocation());
                        }
                        break;
                    case IClasspathEntry.CPE_PROJECT: {
                        IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(entry.getPath());
                        IProject depProject = resource.getProject();
                        if (!depProject.hasNature(JavaCore.NATURE_ID)) {
                            break;
                        }
                        IJavaProject depJavaProject = JavaCore.create(depProject);
                        if (depJavaProject.getOutputLocation() != null) {
                            try {
                                IContainer container = (IContainer)workspaceRoot.findMember(
                                        depJavaProject.getOutputLocation());
                                collector.addPath(container.getLocation());
                                binCollector.addContainer(container);
                            } catch (MalformedURLException e) {
                                TeaVMEclipsePlugin.logError(e);
                            }
                        }
                        projectQueue.add(depJavaProject);
                        break;
                    }
                }
            }
        }
        classPath = collector.getUrls();
        sourceContainers = srcCollector.getContainers();
        classFileContainers = binCollector.getContainers();
        sourceProviders = sourceFileCollector.getProviders();
    }

    static class PathCollector {
        private Set<URL> urlSet = new HashSet<>();
        private List<URL> urls = new ArrayList<>();

        public void addPath(IPath path) throws MalformedURLException {
            File file = path.toFile();
            if (!file.exists()) {
                return;
            }
            if (file.isDirectory()) {
                file = new File(file.getAbsolutePath() + "/");
            } else {
                file = new File(file.getAbsolutePath());
            }
            URL url = file.toURI().toURL();
            if (urlSet.add(url)) {
                urls.add(url);
            }
        }

        public URL[] getUrls() {
            return urls.toArray(new URL[urls.size()]);
        }
    }

    static class SourceFileCollector {
        private Set<String> files = new HashSet<>();
        private List<SourceFileProvider> providers = new ArrayList<>();

        public void addFile(IPath path) {
            if (!files.add(path.toString())) {
                return;
            }
            File file = path.toFile();
            if (!file.exists()) {
                return;
            }
            if (file.isDirectory()) {
                providers.add(new DirectorySourceFileProvider(file));
            } else {
                providers.add(new JarSourceFileProvider(file));
            }
        }

        public SourceFileProvider[] getProviders() {
            return providers.toArray(new SourceFileProvider[providers.size()]);
        }
    }

    static class SourcePathCollector {
        private Set<IContainer> containerSet = new HashSet<>();
        private List<IContainer> containers = new ArrayList<>();

        public void addContainer(IContainer container) {
            if (containerSet.add(container)) {
                containers.add(container);
            }
        }

        public IContainer[] getContainers() {
            return containers.toArray(new IContainer[containers.size()]);
        }
    }
}
