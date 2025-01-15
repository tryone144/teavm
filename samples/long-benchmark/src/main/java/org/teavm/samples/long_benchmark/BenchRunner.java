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
import java.math.BigInteger;
import java.util.Random;
import java.util.function.*;
import org.teavm.samples.long_benchmark.Pairs.*;


public final class BenchRunner {

    private static long start = System.currentTimeMillis();
    private static final int RUNS = 100;

    private static final double longFactor = (double) 0x8000000000000000L;

    private static Random rnd = new Random();

    private BenchRunner() {
    }

    public static void main(String[] args) throws InterruptedException {
        report("Start main thread");
        report("");

        runBenchmark();

        report("Finished main thread");
        report("");
    }

    public static void report(String message) {
        var current = System.currentTimeMillis() - start;
        System.out.println("[" + Thread.currentThread().getName() + "]/" + current + ": " + message);
    }

    private static void runBenchmark() {
        IntSupplier getInt = () -> (int) (Math.random() * 0x80000000);
        LongSupplier getLong = () -> (long) (Math.random() * longFactor);
        DoubleSupplier getDouble = () -> Math.random() * 100.0;
        Supplier<BigInteger> getBigInt = () -> new BigInteger(512, rnd);
        Supplier<Pair<BigInteger, BigInteger>> getBigIntWithSmallerBigInt = () -> {
            return new Pair<BigInteger, BigInteger>(getBigInt.get(), new BigInteger(256, rnd));
        };

        IntFunction<String[]> getDoublesAsString = size -> {
            String[] arr = new String[size];
            for (int i = 0; i < size; ++i) {
                arr[i] = Double.toString(getDouble.getAsDouble());
            }
            return arr;
        };
        IntFunction<String[]> getBigIntsAsString = size -> {
            String[] arr = new String[size];
            for (int i = 0; i < size; ++i) {
                arr[i] = getBigInt.get().toString();
            }
            return arr;
        };

        IntFunction<LongPair[]> getLongPairs = size -> {
            LongPair[] arr = new LongPair[size];
            for (int i = 0; i < size; ++i) {
                arr[i] = new LongPair(getLong.getAsLong(), getLong.getAsLong());
            }
            return arr;
        };
        IntFunction<LongIntPair[]> getLongsWithShift = size -> {
            LongIntPair[] arr = new LongIntPair[size];
            for (int i = 0; i < size; ++i) {
                arr[i] = new LongIntPair(getLong.getAsLong(), (int) (Math.random() * 64.0));
            }
            return arr;
        };

        IntFunction<BigInteger[]> getBigInts = size -> {
            BigInteger[] arr = new BigInteger[size];
            for (int i = 0; i < size; ++i) {
                arr[i] = getBigInt.get();
            }
            return arr;
        };

        @SuppressWarnings("unchecked")
        final var bigIntPairClass = (Class<Pair<BigInteger, BigInteger>>) (Class<?>) Pair.class;
        @SuppressWarnings("unchecked")
        final var bigIntPairWithIntClass = (Class<PairWithInt<BigInteger>>) (Class<?>) PairWithInt.class;
        @SuppressWarnings("unchecked")
        final var bigIntTripleClass = (Class<Triple<BigInteger>>) (Class<?>) Triple.class;

        IntFunction<Pair<BigInteger, BigInteger>[]> getBigIntPairs = size -> {
            @SuppressWarnings("unchecked")
            Pair<BigInteger, BigInteger>[] arr = (Pair<BigInteger, BigInteger>[])
                    Array.newInstance(bigIntPairClass, size);
            for (int i = 0; i < size; ++i) {
                arr[i] = new Pair<BigInteger, BigInteger>(getBigInt.get(), getBigInt.get());
            }
            return arr;
        };
        IntFunction<Pair<BigInteger, BigInteger>[]> getBigIntsWithSmallerBigInt = size -> {
            @SuppressWarnings("unchecked")
            Pair<BigInteger, BigInteger>[] arr = (Pair<BigInteger, BigInteger>[])
                    Array.newInstance(bigIntPairClass, size);
            for (int i = 0; i < size; ++i) {
                arr[i] = getBigIntWithSmallerBigInt.get();
            }
            return arr;
        };
        IntFunction<PairWithInt<BigInteger>[]> getBigIntsWithExp = size -> {
            @SuppressWarnings("unchecked")
            PairWithInt<BigInteger>[] arr = (PairWithInt<BigInteger>[])
                    Array.newInstance(bigIntPairWithIntClass, size);
            for (int i = 0; i < size; ++i) {
                arr[i] = new PairWithInt<BigInteger>(getBigInt.get(), (int) (Math.random() * 16.0));
            }
            return arr;
        };
        IntFunction<Pair<BigInteger, BigInteger>[]> getBigIntsWithPrime = size -> {
            @SuppressWarnings("unchecked")
            Pair<BigInteger, BigInteger>[] arr = (Pair<BigInteger, BigInteger>[])
                    Array.newInstance(bigIntPairClass, size);
            for (int i = 0; i < size; ++i) {
                arr[i] = new Pair<BigInteger, BigInteger>(getBigInt.get(), new BigInteger(512, 64, rnd));
            }
            return arr;
        };
        IntFunction<Triple<BigInteger>[]> getBigIntsWithSmallerBigIntAndExp = size -> {
            @SuppressWarnings("unchecked")
            Triple<BigInteger>[] arr = (Triple<BigInteger>[]) Array.newInstance(bigIntTripleClass, size);
            for (int i = 0; i < size; ++i) {
                arr[i] = new Triple<BigInteger>(getBigInt.get(), getBigIntWithSmallerBigInt.get());
            }
            return arr;
        };

        Benchmark bench = new LongBenchmark(RUNS);
        bench.run("Long_fromInt with < 32bit literal", LongBenchmark::testSmallLongLiteral);
        report("");
        bench.run("Long_create with >= 32bit literal", LongBenchmark::testLongLiteral);
        report("");
        bench.run("Long_fromInt", getInt, LongBenchmark::testLongFromInt);
        report("");
        bench.run("Long_lo (to 32bit integer)", getLong, LongBenchmark::testLongToInt);
        report("");
        bench.run("Long_hi (to 32bit integer)", getLong, LongBenchmark::testLongToIntHigh);
        report("");
        bench.run("Long_fromNumber (64bit double)", getDouble, LongBenchmark::testLongFromDouble);
        report("");
        bench.run("Long_toNumber (64bit double)", getLong, LongBenchmark::testLongToDouble);
        report("");

        bench.run("Long_eq", getLongPairs, LongBenchmark::testLongEqual);
        report("");
        bench.run("Long_ne", getLongPairs, LongBenchmark::testLongNotEqual);
        report("");
        bench.run("Long_gt", getLongPairs, LongBenchmark::testLongGreaterThan);
        report("");
        bench.run("Long_ge", getLongPairs, LongBenchmark::testLongGreaterThanEqual);
        report("");
        bench.run("Long_lt", getLongPairs, LongBenchmark::testLongLessThan);
        report("");
        bench.run("Long_le", getLongPairs, LongBenchmark::testLongLessThanEqual);
        report("");
        bench.run("Long_compare", getLongPairs, LongBenchmark::testLongCompare);
        report("");
        bench.run("Long_ucompare", getLongPairs, LongBenchmark::testLongCompareUnsigned);
        report("");

        bench.run("Long_add", getLongPairs, LongBenchmark::testLongAdd);
        report("");
        bench.run("Long_neg", getLong, LongBenchmark::testLongNegate);
        report("");
        bench.run("Long_sub", getLongPairs, LongBenchmark::testLongSubtract);
        report("");

        bench.run("Long_mul", getLongPairs, LongBenchmark::testLongMultiply);
        report("");
        bench.run("Long_div", getLongPairs, LongBenchmark::testLongDivide);
        report("");
        bench.run("Long_udiv", getLongPairs, LongBenchmark::testLongDivideUnsigned);
        report("");
        bench.run("Long_rem", getLongPairs, LongBenchmark::testLongRemainder);
        report("");
        bench.run("Long_urem", getLongPairs, LongBenchmark::testLongRemainderUnsigned);
        report("");

        bench.run("Long_and", getLongPairs, LongBenchmark::testLongAnd);
        report("");
        bench.run("Long_or", getLongPairs, LongBenchmark::testLongOr);
        report("");
        bench.run("Long_xor", getLongPairs, LongBenchmark::testLongXor);
        report("");
        bench.run("Long_shl", getLongsWithShift, LongBenchmark::testLongShiftLeft);
        report("");
        bench.run("Long_shl (constant)", getLong, LongBenchmark::testLongShiftLeftConstant);
        report("");
        bench.run("Long_shr", getLongsWithShift, LongBenchmark::testLongShiftRight);
        report("");
        bench.run("Long_shr (constant)", getLong, LongBenchmark::testLongShiftRightConstant);
        report("");
        bench.run("Long_shru", getLongsWithShift, LongBenchmark::testLongShiftRightUnsigned);
        report("");
        bench.run("Long_shru (constant)", getLong, LongBenchmark::testLongShiftRightUnsignedConstant);
        report("");
        bench.run("Long_not", getLong, LongBenchmark::testLongNot);
        report("");

        bench.run("unsigned int -> long", getInt, LongBenchmark::testUnsignedIntToLong);
        report("");

        report("Run Classlib benchmarks...");

        bench = new ClasslibBenchmark(RUNS);
        bench.run("Long.toString", getLong, String.class, ClasslibBenchmark::testLongToString);
        report("");
        bench.run("Long.bitCount", getLong, ClasslibBenchmark::testLongBitCount);
        report("");
        bench.run("Double.parseDouble", getDoublesAsString, ClasslibBenchmark::testParseDouble);
        report("");
        bench.run("Double.toString", getDouble, String.class, ClasslibBenchmark::testDoubleToString);
        report("");
        bench.run("Double.toHexString", getDouble, String.class, ClasslibBenchmark::testDoubleToHexString);
        report("");

        report("Run BigInteger benchmarks...");

        bench = new BigIntegerBenchmark.Easy(RUNS);
        bench.run("BigInteger.doubleValue", getBigInts, BigIntegerBenchmark.Easy::testDoubleValue);
        report("");
        bench.run("BigInteger.add", getBigIntPairs, BigInteger.class, BigIntegerBenchmark.Easy::testAdd);
        report("");
        bench.run("BigInteger.subtract", getBigIntPairs, BigInteger.class, BigIntegerBenchmark.Easy::testSubtract);
        report("");

        bench = new BigIntegerBenchmark.Medium(RUNS);
        bench.run("BigInteger.toString", getBigInts, String.class, BigIntegerBenchmark.Medium::testToString);
        report("");
        bench.run("BigInteger.fromString", getBigIntsAsString, BigInteger.class,
                  BigIntegerBenchmark.Medium::testFromString);
        report("");
        bench.run("BigInteger.multiply", getBigIntPairs, BigInteger.class, BigIntegerBenchmark.Medium::testMultiply);
        report("");
        bench.run("BigInteger.sqrt", getBigInts, BigInteger.class, BigIntegerBenchmark.Medium::testSqrt);
        report("");
        bench.run("BigInteger.divide", getBigIntsWithSmallerBigInt, BigInteger.class,
                  BigIntegerBenchmark.Medium::testDivide);
        report("");
        bench.run("BigInteger.remainder", getBigIntsWithSmallerBigInt, BigInteger.class,
                  BigIntegerBenchmark.Medium::testRemainder);
        report("");
        bench.run("BigInteger.divideAndRemainder", getBigIntsWithSmallerBigInt, BigInteger[].class,
                  BigIntegerBenchmark.Medium::testDivideAndRemainder);
        report("");

        bench = new BigIntegerBenchmark.Heavy(RUNS);
        bench.run("BigInteger.pow", getBigIntsWithExp, BigInteger.class, BigIntegerBenchmark.Heavy::testPow);
        report("");
        bench.run("BigInteger.modInverse", getBigIntsWithPrime, BigInteger.class,
                  BigIntegerBenchmark.Heavy::testModInverse);
        report("");
        bench.run("BigInteger.modPow", getBigIntsWithSmallerBigIntAndExp, BigInteger.class,
                  BigIntegerBenchmark.Heavy::testModPow);
        report("");
    }
}
