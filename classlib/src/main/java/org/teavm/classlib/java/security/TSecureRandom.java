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
package org.teavm.classlib.java.security;

import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.util.TRandom;
import org.teavm.jso.crypto.Crypto;
import org.teavm.jso.typedarrays.Uint8Array;

public class TSecureRandom extends TRandom {

    /** stored instance for seed generation in getSeed() */
    private static TSecureRandom seedGenerator;

    public TSecureRandom() {
    }

    public TSecureRandom(@SuppressWarnings("unused") byte[] seed) {
    }

    public static TSecureRandom getInstance(String algorithm)
            throws TNoSuchAlgorithmException {
        if (!algorithm.equals("NativePRNG")
                && !algorithm.equals("NativePRNGBlocking")
                && !algorithm.equals("NativePRNGNonBlocking")) {
            throw new TNoSuchAlgorithmException();
        }
        return new TSecureRandom();
    }

    public String getAlgorithm() {
        return "unknown";
    }

    @Override
    public void setSeed(@SuppressWarnings("unused") long seed) {
    }

    public void setSeed(@SuppressWarnings("unused") byte[] seed) {
    }

    public void reseed() {
    }

    @Override
    protected int next(int bits) {
        int numBytes = (bits + 7) / 8;
        byte[] bytes = new byte[numBytes];
        nextBytes(bytes);

        int val = 0;
        for (int i = 0; i < numBytes; ++i) {
            val = (val << 8) | (bytes[i] & 0xFF);
        }

        return val >>> (numBytes * 8 - bits);
    }

    @Override
    public void nextBytes(byte[] bytes) {
        if (PlatformDetector.isJavaScript() && Crypto.isSupported()) {
            Uint8Array buffer = Uint8Array.create(bytes.length);
            Crypto.current().getRandomValues(buffer);

            for (int i = 0; i < bytes.length; ++i) {
                bytes[i] = (byte) buffer.get(i);
            }
        } else {
            // Fall back to generic random implementation
            super.nextBytes(bytes);
            // TODO: Implement wrapper to JavaScript for WASM backend
            // TODO: Implement proper randomness source in C backend
        }
    }

    public static byte[] getSeed(int numBytes) {
        if (seedGenerator == null) {
            seedGenerator = new TSecureRandom();
        }
        return seedGenerator.generateSeed(numBytes);
    }

    public byte[] generateSeed(int numBytes) {
        if (numBytes < 0) {
            throw new IllegalArgumentException();
        }

        byte[] bytes = new byte[numBytes];
        nextBytes(bytes);
        return bytes;
    }
}
