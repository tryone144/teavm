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

public final class ClasslibBenchmark extends Benchmark {
    public static final int ITERATIONS = 100_000;
    public static final double SCALE = 1e6;

    ClasslibBenchmark(int runs) {
        super(runs, ITERATIONS, SCALE);
    }

    static String testLongToString(long in) {
        String value = Long.toString(in);
        return value;
    }

    static int testLongBitCount(long in) {
        int value = Long.bitCount(in);
        return value;
    }

    static double testParseDouble(String in) {
        double value = Double.parseDouble(in);
        return value;
    }

    static String testDoubleToString(double in) {
        String value = Double.toString(in);
        return value;
    }

    static String testDoubleToHexString(double in) {
        String value = Double.toHexString(in);
        return value;
    }
}
