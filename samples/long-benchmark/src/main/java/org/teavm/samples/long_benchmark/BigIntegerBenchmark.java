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

import java.math.BigInteger;
import org.teavm.samples.long_benchmark.Pairs.*;

public final class BigIntegerBenchmark {

    public static final class Easy extends Benchmark {
        public static final int ITERATIONS = 100_000;
        public static final double SCALE = 1e6;

        Easy(int runs) {
            super(runs, ITERATIONS, SCALE);
        }

        static double testDoubleValue(BigInteger in) {
            double value = in.doubleValue();
            return value;
        }

        static BigInteger testAdd(Pair<BigInteger, BigInteger> in) {
            BigInteger a = in.first;
            BigInteger b = in.second;
            BigInteger value = a.add(b);
            return value;
        }

        static BigInteger testSubtract(Pair<BigInteger, BigInteger> in) {
            BigInteger a = in.first;
            BigInteger b = in.second;
            BigInteger value = a.subtract(b);
            return value;
        }
    }

    public static final class Medium extends Benchmark {
        public static final int ITERATIONS = 2_000;
        public static final double SCALE = 1e6;

        Medium(int runs) {
            super(runs, ITERATIONS, SCALE);
        }

        static String testToString(BigInteger in) {
            String value = in.toString();
            return value;
        }

        static BigInteger testFromString(String in) {
            BigInteger value = new BigInteger(in, 10);
            return value;
        }

        static BigInteger testMultiply(Pair<BigInteger, BigInteger> in) {
            BigInteger a = in.first;
            BigInteger b = in.second;
            BigInteger value = a.multiply(b);
            return value;
        }

        static BigInteger testSqrt(BigInteger in) {
            BigInteger value = in.sqrt();
            return value;
        }

        static BigInteger testDivide(Pair<BigInteger, BigInteger> in) {
            BigInteger a = in.first;
            BigInteger b = in.second;
            BigInteger value = a.divide(b);
            return value;
        }

        static BigInteger testRemainder(Pair<BigInteger, BigInteger> in) {
            BigInteger a = in.first;
            BigInteger b = in.second;
            BigInteger value = a.remainder(b);
            return value;
        }

        static BigInteger[] testDivideAndRemainder(Pair<BigInteger, BigInteger> in) {
            BigInteger a = in.first;
            BigInteger b = in.second;
            BigInteger[] value = a.divideAndRemainder(b);
            return value;
        }
    }

    public static final class Heavy extends Benchmark {
        public static final int ITERATIONS = 250;
        public static final double SCALE = 1e3;

        Heavy(int runs) {
            super(runs, ITERATIONS, SCALE);
        }

        static BigInteger testPow(PairWithInt<BigInteger> in) {
            BigInteger a = in.first;
            int b = in.second;
            BigInteger value = a.pow(b);
            return value;
        }

        static BigInteger testModInverse(Pair<BigInteger, BigInteger> in) {
            BigInteger a = in.first;
            BigInteger b = in.second;
            BigInteger value = a.modInverse(b);
            return value;
        }

        static BigInteger testModPow(Triple<BigInteger> in) {
            BigInteger a = in.first;
            BigInteger b = in.second;
            BigInteger c = in.third;
            BigInteger value = a.modPow(c, b); // a ^ c mod b, c should be half as long as a
            return value;
        }
    }

}
