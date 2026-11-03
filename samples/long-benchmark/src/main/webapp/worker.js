let $rt_stdoutBuffer = "";
function $rt_putStdoutCustom(msg) {
    let index = 0;
    while (true) {
        let next = msg.indexOf('\n', index);
        if (next < 0) {
            $rt_stdoutBuffer += msg;
            break;
        }
        let line = $rt_stdoutBuffer + msg.substring(index, next);
        postMessage({ type: "stdout", data: line });
        $rt_stdoutBuffer = "";
        index = next + 1;
    }
}
this.$rt_putStdoutCustom = $rt_putStdoutCustom;

importScripts("js/long-benchmark.js");
console.log("Worker started");

self.onmessage = () => {
    console.log("Run benchmark...");
    main();
    console.log("Benchmark finished");
    postMessage({ type: "done" });
};
