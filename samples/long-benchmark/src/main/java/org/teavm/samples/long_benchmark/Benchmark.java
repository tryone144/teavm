/*
 *  Copyright 2023 Bernd Busse.
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
package org.teavm.samples.long_benchmark;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.function.*;
import org.teavm.jso.browser.Performance;

abstract class Benchmark {
    public int iterations = 1;
    private double scale = 1;
    private String range = "s";

    private double[] runTiming;
    private double total;

    private static DecimalFormat intFmt = new DecimalFormat("#,##0");
    private static DecimalFormat fmt = new DecimalFormat("#0.0##");

    public Benchmark(int runs, int iterations, double scale) {
        this.iterations = iterations;
        this.scale = scale / 1000.0;
        this.runTiming = new double[runs];

        if (scale < 1e3) {
            this.range = "s";
        } else if (scale < 1e6) {
            this.range = "ms";
        } else if (scale < 1e9) {
            this.range = "μs";
        } else {
            this.range = "ns";
        }
    }

    private void reportStats() {
        int runs = runTiming.length;

        double mean = total / (double) runs;
        double mse = 0;
        for (int r = 0; r < runs; ++r) {
            double err = runTiming[r] - mean;
            mse += err * err;
        }
        double deviation = Math.sqrt(mse / ((double) runs - 1.0));
        double ciDeviation = 1.96 * deviation / Math.sqrt(runs);

        double meanPerOp = scale * mean / (double) iterations;
        double msePerOp = 0;
        for (int r = 0; r < runs; ++r) {
            double err = scale * runTiming[r] / (double) iterations - meanPerOp;
            msePerOp += err * err;
        }
        double deviationPerOp = Math.sqrt(msePerOp / ((double) runs - 1.0));
        double ciDeviationPerOp = 1.96 * deviationPerOp / Math.sqrt(runs);

        BenchRunner.report("Avg over " + runs + " runs: "
               + fmt.format(mean) + "±" + fmt.format(ciDeviation) + " milliseconds"
               + " => " + fmt.format(meanPerOp) + "±" + fmt.format(ciDeviationPerOp) + range + "/op"
               + " (±" + fmt.format(ciDeviationPerOp / meanPerOp * 100.0) + "%)");
    }

    private void run(String title, IntConsumer runner) {
        BenchRunner.report("Test " + title + " (" + intFmt.format(iterations).replace(",", "_") + " iterations)...");

        // reset cumulative total
        total = 0;

        // do warm up run
        runner.accept(iterations);

        // measured runs
        for (int r = 0; r < runTiming.length; ++r) {
            double start = Performance.now();
            runner.accept(iterations);
            double end = Performance.now();

            runTiming[r] = end - start;
            total += runTiming[r];
        }

        reportStats();
        try {
            Thread.sleep(10);
        } catch (Exception e) {
            //
        } finally {
            Thread.yield();
        }
    }


    // LongSupplier
    public void run(String title, LongSupplier benchmark) {
        // initialize benchmark output buffer
        long[] results = new long[iterations];

        run(title, iterations -> {
            for (int i = 0; i < iterations; ++i) {
                results[i] = benchmark.getAsLong();
            }
        });
    }

    // IntToLongFunction
    public void run(String title, IntSupplier init, IntToLongFunction benchmark) {
        // initialize benchmark inputs
        int[] inputs = new int[iterations];
        for (int i = 0; i < iterations; ++i) {
            inputs[i] = init.getAsInt();
        }

        // initialize benchmark output buffer
        long[] results = new long[iterations];

        run(title, iterations -> {
            for (int i = 0; i < iterations; ++i) {
                results[i] = benchmark.applyAsLong(inputs[i]);
            }
        });
    }

    // DoubleToLongFunction
    public void run(String title, DoubleSupplier init, DoubleToLongFunction benchmark) {
        // initialize benchmark inputs
        double[] inputs = new double[iterations];
        for (int i = 0; i < iterations; ++i) {
            inputs[i] = init.getAsDouble();
        }

        // initialize benchmark output buffer
        long[] results = new long[iterations];

        run(title, iterations -> {
            for (int i = 0; i < iterations; ++i) {
                results[i] = benchmark.applyAsLong(inputs[i]);
            }
        });
    }

    // LongUnaryOperator
    public void run(String title, LongSupplier init, LongUnaryOperator benchmark) {
        // initialize benchmark inputs
        long[] inputs = new long[iterations];
        for (int i = 0; i < iterations; ++i) {
            inputs[i] = init.getAsLong();
        }

        // initialize benchmark output buffer
        long[] results = new long[iterations];

        run(title, iterations -> {
            for (int i = 0; i < iterations; ++i) {
                results[i] = benchmark.applyAsLong(inputs[i]);
            }
        });
    }

    // LongToIntFunction
    public void run(String title, LongSupplier init, LongToIntFunction benchmark) {
        // initialize benchmark inputs
        long[] inputs = new long[iterations];
        for (int i = 0; i < iterations; ++i) {
            inputs[i] = init.getAsLong();
        }

        // initialize benchmark output buffer
        int[] results = new int[iterations];

        run(title, iterations -> {
            for (int i = 0; i < iterations; ++i) {
                results[i] = benchmark.applyAsInt(inputs[i]);
            }
        });
    }

    // LongToDoubleFunction
    public void run(String title, LongSupplier init, LongToDoubleFunction benchmark) {
        // initialize benchmark inputs
        long[] inputs = new long[iterations];
        for (int i = 0; i < iterations; ++i) {
            inputs[i] = init.getAsLong();
        }

        // initialize benchmark output buffer
        double[] results = new double[iterations];

        run(title, iterations -> {
            for (int i = 0; i < iterations; ++i) {
                results[i] = benchmark.applyAsDouble(inputs[i]);
            }
        });
    }

    // Predicate<T>
    public <T> void run(String title, IntFunction<T[]> init, Predicate<T> benchmark) {
        // initialize benchmark inputs
        T[] inputs = init.apply(iterations);

        // initialize benchmark output buffer
        boolean[] results = new boolean[iterations];

        run(title, iterations -> {
            for (int i = 0; i < iterations; ++i) {
                results[i] = benchmark.test(inputs[i]);
            }
        });
    }

    // ToIntFunction<T>
    public <T> void run(String title, IntFunction<T[]> init, ToIntFunction<T> benchmark) {
        // initialize benchmark inputs
        T[] inputs = init.apply(iterations);

        // initialize benchmark output buffer
        int[] results = new int[iterations];

        run(title, iterations -> {
            for (int i = 0; i < iterations; ++i) {
                results[i] = benchmark.applyAsInt(inputs[i]);
            }
        });
    }

    // ToDoubleFunction<T>
    public <T> void run(String title, IntFunction<T[]> init, ToDoubleFunction<T> benchmark) {
        // initialize benchmark inputs
        T[] inputs = init.apply(iterations);

        // initialize benchmark output buffer
        double[] results = new double[iterations];

        run(title, iterations -> {
            for (int i = 0; i < iterations; ++i) {
                results[i] = benchmark.applyAsDouble(inputs[i]);
            }
        });
    }

    // ToLongFunction<T>
    public <T> void run(String title, IntFunction<T[]> init, ToLongFunction<T> benchmark) {
        // initialize benchmark inputs
        T[] inputs = init.apply(iterations);

        // initialize benchmark output buffer
        long[] results = new long[iterations];

        run(title, iterations -> {
            for (int i = 0; i < iterations; ++i) {
                results[i] = benchmark.applyAsLong(inputs[i]);
            }
        });
    }

    // DoubleFunction<T>
    public <T> void run(String title, DoubleSupplier init, Class<T> cl, DoubleFunction<T> benchmark) {
        // initialize benchmark inputs
        double[] inputs = new double[iterations];
        for (int i = 0; i < iterations; ++i) {
            inputs[i] = init.getAsDouble();
        }

        // initialize benchmark output buffer
        @SuppressWarnings("unchecked")
        T[] results = (T[]) Array.newInstance(cl, iterations);

        run(title, iterations -> {
            for (int i = 0; i < iterations; ++i) {
                results[i] = benchmark.apply(inputs[i]);
            }
        });
    }

    // LongFunction<T>
    public <T> void run(String title, LongSupplier init, Class<T> cl, LongFunction<T> benchmark) {
        // initialize benchmark inputs
        long[] inputs = new long[iterations];
        for (int i = 0; i < iterations; ++i) {
            inputs[i] = init.getAsLong();
        }

        // initialize benchmark output buffer
        @SuppressWarnings("unchecked")
        T[] results = (T[]) Array.newInstance(cl, iterations);

        run(title, iterations -> {
            for (int i = 0; i < iterations; ++i) {
                results[i] = benchmark.apply(inputs[i]);
            }
        });
    }

    // Function<T, R>
    public <T, R> void run(String title, IntFunction<T[]> init, Class<R> cl, Function<T, R> benchmark) {
        // initialize benchmark inputs
        T[] inputs = init.apply(iterations);

        // initialize benchmark output buffer
        @SuppressWarnings("unchecked")
        R[] results = (R[]) Array.newInstance(cl, iterations);

        run(title, iterations -> {
            for (int i = 0; i < iterations; ++i) {
                results[i] = benchmark.apply(inputs[i]);
            }
        });
    }
}
