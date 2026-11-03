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

import org.teavm.samples.long_benchmark.Pairs.*;

public final class LongBenchmark extends Benchmark {
    public static final int ITERATIONS = 1_000_000;
    public static final double SCALE = 1e9;

    LongBenchmark(int runs) {
        super(runs, ITERATIONS, SCALE);
    }

    static long testSmallLongLiteral() {
        long value = 127107580L; // 27 bit literal
        return value;
    }

    static long testLongLiteral() {
        long value = 8919268947643919751L; // 63 bit literal
        return value;
    }

    static long testLongFromInt(int in) {
        long value = (long) in;
        return value;
    }

    static int testLongToInt(long in) {
        int value = (int) in;
        return value;
    }

    static int testLongToIntHigh(long in) {
        int value = (int) (in >> 32);
        return value;
    }

    static long testLongFromDouble(double in) {
        long value = (long) in;
        return value;
    }

    static double testLongToDouble(long in) {
        double value = (double) in;
        return value;
    }

    static boolean testLongEqual(LongPair in) {
        long a = in.first;
        long b = in.second;
        boolean value = a != b; // compiler optimizes this to a == b ? 0 : 1
        return value;
    }

    static boolean testLongNotEqual(LongPair in) {
        long a = in.first;
        long b = in.second;
        boolean value = a == b; // compiler optimizes this to a != b ? 0 : 1
        return value;
    }

    static boolean testLongGreaterThan(LongPair in) {
        long a = in.first;
        long b = in.second;
        boolean value = a <= b; // compiler optimizes this to a > b ? 0 : 1
        return value;
    }

    static boolean testLongGreaterThanEqual(LongPair in) {
        long a = in.first;
        long b = in.second;
        boolean value = a < b; // compiler optimizes this to a >= b ? 0 : 1
        return value;
    }

    static boolean testLongLessThan(LongPair in) {
        long a = in.first;
        long b = in.second;
        boolean value = a >= b; // compiler optimizes this to a < b ? 0 : 1
        return value;
    }

    static boolean testLongLessThanEqual(LongPair in) {
        long a = in.first;
        long b = in.second;
        boolean value = a > b; // compiler optimizes this to a <= b ? 0 : 1
        return value;
    }

    static int testLongCompare(LongPair in) {
        long a = in.first;
        long b = in.second;
        int value = (a < b) || (a > b) ? 1 : 0;
        return value;
    }

    static int testLongCompareUnsigned(LongPair in) {
        long a = in.first;
        long b = in.second;
        int value = Long.compareUnsigned(a, b);
        return value;
    }

    static long testLongAdd(LongPair in) {
        long a = in.first;
        long b = in.second;
        long value = a + b;
        return value;
    }

    static long testLongNegate(long in) {
        long value = -in;
        return value;
    }

    static long testLongSubtract(LongPair in) {
        long a = in.first;
        long b = in.second;
        long value = a - b;
        return value;
    }

    static long testLongMultiply(LongPair in) {
        long a = in.first;
        long b = in.second;
        long value = a * b;
        return value;
    }

    static long testLongDivide(LongPair in) {
        long a = in.first;
        long b = in.second;
        long value = a / b;
        return value;
    }

    static long testLongDivideUnsigned(LongPair in) {
        long a = in.first;
        long b = in.second;
        long value = Long.divideUnsigned(a, b);
        return value;
    }

    static long testLongRemainder(LongPair in) {
        long a = in.first;
        long b = in.second;
        long value = a % b;
        return value;
    }

    static long testLongRemainderUnsigned(LongPair in) {
        long a = in.first;
        long b = in.second;
        long value = Long.remainderUnsigned(a, b);
        return value;
    }

    static long testLongAnd(LongPair in) {
        long a = in.first;
        long b = in.second;
        long value = a & b;
        return value;
    }

    static long testLongOr(LongPair in) {
        long a = in.first;
        long b = in.second;
        long value = a | b;
        return value;
    }

    static long testLongXor(LongPair in) {
        long a = in.first;
        long b = in.second;
        long value = a ^ b;
        return value;
    }

    static long testLongShiftLeft(LongIntPair in) {
        long a = in.first;
        int b = in.second;
        long value = a << b;
        return value;
    }

    static long testLongShiftLeftConstant(long in) {
        long value = in << 23;
        return value;
    }

    static long testLongShiftRight(LongIntPair in) {
        long a = in.first;
        int b = in.second;
        long value = a >> b;
        return value;
    }

    static long testLongShiftRightConstant(long in) {
        long value = in >> 23;
        return value;
    }

    static long testLongShiftRightUnsigned(LongIntPair in) {
        long a = in.first;
        int b = in.second;
        long value = a >>> b;
        return value;
    }

    static long testLongShiftRightUnsignedConstant(long in) {
        long value = in >>> 23;
        return value;
    }

    static long testLongNot(long in) {
        long value = ~in;
        return value;
    }

    static long testUnsignedIntToLong(int in) {
        long value = in & 0xFFFFFFFFL;
        return value;
    }
}
