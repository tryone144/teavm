package org.teavm.debugging;

import org.teavm.common.RecordArray;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class SourceLocationIterator {
    private DebugInformation debugInformation;
    private int lineIndex;
    private int fileIndex;
    private GeneratedLocation location;
    private int fileId = -1;
    private int line = -1;
    private boolean endReached;

    SourceLocationIterator(DebugInformation debugInformation) {
        this.debugInformation = debugInformation;
        read();
    }

    public boolean isEndReached() {
        return endReached;
    }

    private void read() {
        if (lineIndex >= debugInformation.lineMapping.size()) {
            nextFileRecord();
        } else if (fileIndex >= debugInformation.fileMapping.size()) {
            nextLineRecord();
        } else if (fileIndex < debugInformation.fileMapping.size() &&
                lineIndex < debugInformation.lineMapping.size()) {
            RecordArray.Record fileRecord = debugInformation.fileMapping.get(fileIndex++);
            RecordArray.Record lineRecord = debugInformation.lineMapping.get(lineIndex++);
            GeneratedLocation fileLoc = DebugInformation.key(fileRecord);
            GeneratedLocation lineLoc = DebugInformation.key(lineRecord);
            int cmp = fileLoc.compareTo(lineLoc);
            if (cmp < 0) {
                nextFileRecord();
            } else if (cmp > 0) {
                nextLineRecord();
            } else {
                nextFileRecord();
                nextLineRecord();
            }
        } else {
            endReached = true;
        }
    }

    private void nextFileRecord() {
        RecordArray.Record record = debugInformation.fileMapping.get(fileIndex++);
        location = DebugInformation.key(record);
        fileId = record.get(2);
    }

    private void nextLineRecord() {
        RecordArray.Record record = debugInformation.lineMapping.get(lineIndex++);
        location = DebugInformation.key(record);
        line = record.get(2);
    }

    public void next() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        read();
    }

    public GeneratedLocation getLocation() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        return location;
    }

    public int getFileNameId() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        return fileId;
    }

    public String getFileName() {
        int fileId = getFileNameId();
        return fileId >= 0 ? debugInformation.getFileName(fileId) : null;
    }

    public int getLine() {
        if (isEndReached()) {
            throw new IllegalStateException("End already reached");
        }
        return line;
    }
}
