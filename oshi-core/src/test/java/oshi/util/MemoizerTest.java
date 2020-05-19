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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class MemoizerTest {
    // We want enough threads that some of them are forced to wait
    private static final int numberOfThreads = Math.max(5, Runtime.getRuntime().availableProcessors() + 2);

    private ExecutorService ex;

    @Before
    public void before() {
        final ThreadPoolExecutor ex = new ThreadPoolExecutor(numberOfThreads, numberOfThreads, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        ex.allowCoreThreadTimeOut(false);
        ex.prestartAllCoreThreads();// make sure we don't lose refreshes in tests because of spending time to start
                                    // threads
        this.ex = ex;
    }

    @After
    public void after() throws InterruptedException {
        ex.shutdownNow();
        ex.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    @Test
    public void get() throws Throwable {
        // With max time limits these tests take a minute to run. But with no changes to
        // the memoizer it's simply testing overkill. Use a RNG to limit these tests
        // regularly but occasionally run the longer tests.
        int x = new Random().nextInt(50);
        // Set minimal defaults
        int refreshIters = 200; // ~ 2 seconds
        int noRefreshIters = 2_000; // ~ 2 seconds
        if (x == 0) {
            // 2% chance full length noRefresh
            noRefreshIters = 20_000; // ~ 20 seconds
        } else if (x == 1) {
            // 2% chance full length refresh
            refreshIters = 4000; // ~ 40 seconds
        } else if (x < 12) {
            // 20% chance longer noRerfesh
            noRefreshIters = 5_000; // ~ 5 seconds
        } else if (x < 22) {
            // 20% chance longer rerfesh
            refreshIters = 500; // ~ 5 seconds
        }
        // Do tests with no refresh.
        long iterationDurationNanos = TimeUnit.MILLISECONDS.toNanos(1);
        long ttlNanos = -1;
        for (int r = 0; r < noRefreshIters; r++) {
            run(iterationDurationNanos, ttlNanos);
        }
        // Do tests with refresh after 0.1 ms.
        iterationDurationNanos = TimeUnit.MILLISECONDS.toNanos(10);
        ttlNanos = iterationDurationNanos / 100;
        assertNotEquals(0, ttlNanos); // avoid div/0 later
        for (int r = 0; r < refreshIters; r++) {
            run(iterationDurationNanos, ttlNanos);
        }
    }

    private void run(final long iterationDurationNanos, final long ttlNanos) throws Throwable {
        final Supplier<Long> s = new Supplier<Long>() {
            private long value;

            // this method is not thread-safe, the returned value of the counter may go down
            // if this method is called concurrently from different threads
            @Override
            public Long get() {
                return ++value;
            }
        };
        // The memoizer we are testing
        final Supplier<Long> m = memoize(s, ttlNanos);
        // Hold the results until all threads terminate
        final Collection<Future<Void>> results = new ArrayList<>();
        // Mark the start time, end after iterationDuration
        final long beginNanos = System.nanoTime();
        for (int tid = 0; tid < numberOfThreads; tid++) {
            results.add(ex.submit(() -> {
                // First read from the memoizer. Only one thread will win this race to increment
                // 0 to 1, but all threads should read at least 1, if not increment further
                Long previousValue = m.get();
                assertNotNull(previousValue);
                assertTrue(previousValue > 0);
                // Memoizer's ttl was set during previous call (for race winning thread) or
                // earlier (for losing threads) but if we delay for at least ttl from now, we
                // are sure to get at least one increment if ttl is nonnegative
                final long firstSupplierCallNanos = System.nanoTime();
                // using guaranteedIteration this loop is guaranteed to be executed at
                // least once regardless of whether we have exceeded time delays
                boolean guaranteedIteration = false;
                long now;
                while ((now = System.nanoTime()) - beginNanos < iterationDurationNanos
                        || now - firstSupplierCallNanos < ttlNanos || (guaranteedIteration = !guaranteedIteration)) {
                    // guaranteedIteration will only be set true when the first two timing
                    // conditions are false, which will allow at least one iteration. After that
                    // final iteration the boolean will toggle false again to stop the loop.
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    final Long newValue = m.get();
                    assertNotNull(newValue);// check that we never get uninitialized value
                    assertTrue(String.format("newValue=%s, previousValue=%s", newValue, previousValue),
                            newValue >= previousValue);// check that the counter never goes down
                    previousValue = newValue;
                }
                return null;
            }));
        }
        /*
         * Make sure all the submitted tasks finished correctly
         */
        finishAllThreads(results);
        /*
         * All the writes to s.value field happened-before this read because of all the
         * result.get() invocations, so it holds the final/max value returned by any
         * thread. We cannot access s.value but it's private, and s.get() will increment
         * before returning, so here we subtract 1 from the result to determine what the
         * internal s.value was before this call increments it.
         */
        final long actualNumberOfIncrements = s.get() - 1;
        testIncrementCounts(actualNumberOfIncrements, iterationDurationNanos, ttlNanos);

    }

    private static void finishAllThreads(Collection<Future<Void>> results)
            throws InterruptedException, ExecutionException {
        for (final Future<Void> result : results) {
            result.get();
        }
    }

    private static void testIncrementCounts(long actualNumberOfIncrements, long iterationDurationNanos, long ttlNanos) {
        if (ttlNanos < 0) {
            assertEquals(String.format("ttlNanos=%d, expectedNumberOfIncrements=%d, actualNumberOfIncrements=%s",
                    ttlNanos, 1, actualNumberOfIncrements), 1, actualNumberOfIncrements);
        }
        /*
         * Calculation of expectedNumberOfIncrements is a bit tricky because there is no
         * such thing. We can only talk about min and max possible values when ttl > 0.
         *
         * Min: Two increments are guaranteed: the initial one, because it does not
         * depend on timings, and a second one which ensures at least ttlNanos have
         * elapsed since the first one. All other refreshes may or may not happen
         * depending on the timings. Therefore the min is 2. In the case of negative ttl
         * we should get only one increment ever; otherwise we must have at least 2
         * increments.
         *
         * Max: Each thread has a chance to refresh one more time after
         * (iterationDurationNanos / ttlNanos) refreshes have been collectively done,
         * which will increment again. This happens because an arbitrary amount of time
         * may elapse between the instant when a thread enters the while cycle body for
         * the last time (the last iteration), and the instant that is observed by the
         * MemoizedObject.get method. Additionally, each thread may refresh one more
         * time because of the last iteration of the loop caused by guaranteedIteration.
         * Therefore each thread may do up to 2 additional refreshes.
         */
        else {
            final int minExpectedNumberOfIncrements = 2;
            final long maxExpectedNumberOfIncrements = (iterationDurationNanos / ttlNanos) + 2 * numberOfThreads;

            assertTrue(String.format(
                    "ttlNanos=%s, minExpectedNumberOfRefreshes=%s, maxExpectedNumberOfRefreshes=%s, actualNumberOfRefreshes=%s",
                    ttlNanos, minExpectedNumberOfIncrements, maxExpectedNumberOfIncrements, actualNumberOfIncrements),
                    minExpectedNumberOfIncrements <= actualNumberOfIncrements
                            && actualNumberOfIncrements <= maxExpectedNumberOfIncrements);
        }
    }
}
