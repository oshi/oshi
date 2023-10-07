/*
 * Copyright 2019-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisabledOnOs(OS.SOLARIS)
final class MemoizerTest {
    // We want enough threads that some of them are forced to wait
    private static final int numberOfThreads = Math.max(5, Runtime.getRuntime().availableProcessors() + 2);

    private ExecutorService ex;

    @BeforeEach
    void before() {
        final ThreadPoolExecutor ex = new ThreadPoolExecutor(numberOfThreads, numberOfThreads, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        ex.allowCoreThreadTimeOut(false);
        ex.prestartAllCoreThreads(); // make sure we don't lose refreshes in tests because of
                                     // spending time to start threads
        this.ex = ex;
    }

    @AfterEach
    void after() throws InterruptedException {
        ex.shutdownNow();
        ex.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    @Test
    void get() throws Throwable {
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
        assertThat("ttlNanos should not be zero", ttlNanos, is(not(0L))); // avoid div/0 later
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
                assertThat("previousValue should not be null", previousValue, is(notNullValue()));
                assertThat("previousValue should be greater than zero", previousValue, is(greaterThan(0L)));
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
                    // check that we never get uninitialized
                    assertThat("newValue should not be null", newValue, is(notNullValue()));
                    // check that the counter never goes down // value
                    assertThat("newValue shuld be larger", newValue, is(not(lessThan(previousValue))));
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
         * All the writes to s.value field happened-before this read because of all the result.get() invocations, so it
         * holds the final/max value returned by any thread. We cannot access s.value but it's private, and s.get() will
         * increment before returning, so here we subtract 1 from the result to determine what the internal s.value was
         * before this call increments it.
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
            assertThat(String.format(Locale.ROOT, "ttlNanos=%d", ttlNanos), actualNumberOfIncrements, is(1L));
        } else {
            /*
             * Calculation of expectedNumberOfIncrements is a bit tricky because there is no such thing. We can only
             * talk about min and max possible values when ttl > 0.
             *
             * Min: Two increments are guaranteed: the initial one, because it does not depend on timings, and a second
             * one which ensures at least ttlNanos have elapsed since the first one. All other refreshes may or may not
             * happen depending on the timings. Therefore the min is 2. In the case of negative ttl we should get only
             * one increment ever; otherwise we must have at least 2 increments.
             *
             * Max: Each thread has a chance to refresh one more time after (iterationDurationNanos / ttlNanos)
             * refreshes have been collectively done, which will increment again. This happens because an arbitrary
             * amount of time may elapse between the instant when a thread enters the while cycle body for the last time
             * (the last iteration), and the instant that is observed by the MemoizedObject.get method. Additionally,
             * each thread may refresh one more time because of the last iteration of the loop caused by
             * guaranteedIteration. Therefore each thread may do up to 2 additional refreshes.
             */
            final long minExpectedNumberOfIncrements = 2L;
            final long maxExpectedNumberOfIncrements = (iterationDurationNanos / ttlNanos) + 2L * numberOfThreads;

            assertThat(String.format(Locale.ROOT, "ttlNanos=%s", ttlNanos), minExpectedNumberOfIncrements,
                    is(lessThanOrEqualTo(actualNumberOfIncrements)));
            assertThat(String.format(Locale.ROOT, "ttlNanos=%s", ttlNanos), actualNumberOfIncrements,
                    is(lessThanOrEqualTo(maxExpectedNumberOfIncrements)));
        }
    }
}
