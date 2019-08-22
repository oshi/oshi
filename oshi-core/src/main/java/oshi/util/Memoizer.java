/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A memoized function stores the output corresponding to some set of specific
 * inputs. Subsequent calls with remembered inputs return the remembered result
 * rather than recalculating it.
 */
public final class Memoizer {

    /**
     * Store a supplier in a delegate function to be computed only once, or
     * after ttl has expired
     * 
     * @param original
     *            The {@link java.util.function.Supplier} to memoize
     * @param ttlNanos
     *            Time in nanoseconds to retain calculation
     * @return A memoized version of the supplier
     */
    public static <T> Supplier<T> memoize(Supplier<T> original, long ttlNanos) {
        // Adapted from Guava's ExpiringMemoizingSupplier
        return new Supplier<T>() {
            final Supplier<T> delegate = original;
            volatile T value;
            volatile long expirationNanos;

            @Override
            public T get() {
              long nanos = expirationNanos;
              long now = System.nanoTime();
              if (nanos == 0 || now - nanos >= 0) {
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

    public static <T> Supplier<T> memoize2(Supplier<T> original) {
        return memoize(original, Integer.MAX_VALUE);
    }

    /**
     * Store a supplier in a delegate function to be computed only once.
     * 
     * @param original
     *            The {@link java.util.function.Supplier} to memoize
     * @return A memoized version of the supplier
     */
    public static <T> Supplier<T> memoize(Supplier<T> original) {
        // Method copied from:
        // https://stackoverflow.com/questions/35331327/
        // does-java-8-have-cached-support-for-suppliers/35335467#35335467
        return new Supplier<T>() {
            Supplier<T> delegate = this::firstTime;
            boolean initialized;
            public T get() {
                return delegate.get();
            }

            private synchronized T firstTime() {
                if (!initialized) {
                    T value = original.get();
                    delegate = () -> value;
                    initialized = true;
                }
                return delegate.get();
            }
        };
    }

    /*
     * Methods suggested but don't work
     */
    public static <I, O> Function<I, O> memoize1(Function<I, O> f) {
        return new Function<I, O>() {
            private volatile O value;

            public O apply(I i) {
                O value = this.value;
                if (value == null) {
                    synchronized (this) {
                        value = this.value;
                        if (value == null) {
                            this.value = value = f.apply(i);
                            if (value == null) {
                                throw new AssertionError("Internal error. Function must not return null");
                            }
                        }
                    }
                }
                return value;
            }
        };
    }

    public static <T> Supplier<T> memoize1(Supplier<T> s) {
        return () -> memoize1(i -> s.get()).apply(null);
    }

    public static void main(String[] args) {

        Supplier<Long> nanoTime = memoize(System::nanoTime);

        System.out.println("Delegate version from SO post:");
        long base = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            System.out.println(i + ": " + (nanoTime.get() - base));
        }

        nanoTime = memoize1(System::nanoTime);
        System.out.println("DCL version from PR comments:");
        base = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            System.out.println(i + ": " + (nanoTime.get() - base));
        }

        nanoTime = memoize(System::nanoTime, 100_000);
        System.out.println("Expiring version 100,000 ns:");
        base = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            System.out.println(i + ": " + (nanoTime.get() - base));
        }

        nanoTime = memoize2(System::nanoTime);
        System.out.println("Expiring version using max int:");
        base = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            System.out.println(i + ": " + (nanoTime.get() - base));
        }

    }
}
