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

final class Pairs {

    static final class LongPair {
        public long first;
        public long second;

        public LongPair(long first, long second) {
            this.first = first;
            this.second = second;
        }
    }

    static final class LongIntPair {
        public long first;
        public int second;

        public LongIntPair(long first, int second) {
            this.first = first;
            this.second = second;
        }
    }

    static final class PairWithInt<U> {
        public U first;
        public int second;

        public PairWithInt(U first, int second) {
            this.first = first;
            this.second = second;
        }
    }

    static final class Pair<U, V> {
        public U first;
        public V second;

        public Pair(U first, V second) {
            this.first = first;
            this.second = second;
        }
    }

    static final class Triple<U> {
        public U first;
        public U second;
        public U third;

        public Triple(U first, Pair<U, U> second) {
            this.first = first;
            this.second = second.first;
            this.third = second.second;
        }
    }

}
