/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.util;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * A memoized function stores the output corresponding to some set of specific
 * inputs. Subsequent calls with remembered inputs return the remembered result
 * rather than recalculating it.
 */
@ThreadSafe
public final class Memoizer {

    private static final Supplier<Long> defaultExpirationNanos = memoize(Memoizer::queryExpirationConfig,
            TimeUnit.MINUTES.toNanos(1));

    private Memoizer() {
    }

    private static long queryExpirationConfig() {
        return TimeUnit.MILLISECONDS.toNanos(GlobalConfig.get("oshi.util.memoizer.expiration", 300));
    }

    /**
     * Default exipiration of memoized values in nanoseconds, which will refresh
     * after this time elapses. Update by setting {@link GlobalConfig} property
     * <code>oshi.util.memoizer.expiration</code> to a value in milliseconds.
     *
     * @return The number of nanoseconds to keep memoized values before refreshing
     */
    public static long defaultExpiration() {
        return defaultExpirationNanos.get();
    }

    /**
     * Store a supplier in a delegate function to be computed once, and only again
     * after time to live (ttl) has expired.
     *
     * @param <T>
     *            The type of object supplied
     * @param original
     *            The {@link java.util.function.Supplier} to memoize
     * @param ttlNanos
     *            Time in nanoseconds to retain calculation. If negative, retain
     *            indefinitely.
     * @return A memoized version of the supplier
     */
    public static <T> Supplier<T> memoize(Supplier<T> original, long ttlNanos) {
        // Adapted from Guava's ExpiringMemoizingSupplier
        return new Supplier<T>() {
            final Supplier<T> delegate = original;
            volatile T value; // NOSONAR squid:S3077
            volatile long expirationNanos;

            @Override
            public T get() {
                long nanos = expirationNanos;
                long now = System.nanoTime();
                if (nanos == 0 || (ttlNanos >= 0 && now - nanos >= 0)) {
                    synchronized (this) {
                        if (nanos == expirationNanos) { // recheck for lost race
                            T t = delegate.get();
                            value = t;
                            nanos = now + ttlNanos;
                            expirationNanos = (nanos == 0) ? 1 : nanos;
                            return t;
                        }
                    }
                }
                return value;
            }
        };
    }

    /**
     * Store a supplier in a delegate function to be computed only once.
     *
     * @param <T>
     *            The type of object supplied
     * @param original
     *            The {@link java.util.function.Supplier} to memoize
     * @return A memoized version of the supplier
     */
    public static <T> Supplier<T> memoize(Supplier<T> original) {
        return memoize(original, -1L);
    }
}
