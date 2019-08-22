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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A memoized function stores the output corresponding to some set of specific
 * inputs. Subsequent calls with remembered inputs return the remembered result
 * rather than recalculating it.
 */
public final class Memoizer {

    private Memoizer() {
    }

    /**
     * Store the function in a thread-safe map to be computed only once.
     * 
     * @param f
     *            The {@link java.util.function.Function} to memoize
     * @return A memoized version of the function
     */
    public static <I, O> Function<I, O> memoize(Function<I, O> f) {
        ConcurrentMap<I, O> lookup = new ConcurrentHashMap<>();
        return input -> lookup.computeIfAbsent(input, f);
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
}
