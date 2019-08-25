package oshi.util;



import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
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
    private static final int numberOfThreads = Math.max(5, Runtime.getRuntime().availableProcessors());

    private ExecutorService ex;

    @Before
    public void before() {
        final ThreadPoolExecutor ex = new ThreadPoolExecutor(numberOfThreads, numberOfThreads, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        ex.allowCoreThreadTimeOut(false);
        ex.prestartAllCoreThreads();// make sure we don't loose refreshes in tests because of spending time to start
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
        long iterationDurationNanos = TimeUnit.MILLISECONDS.toNanos(1);
        for (int r = 0; r < 20_000; r++) {
            assertTrue(run(iterationDurationNanos, -1));
        }
        iterationDurationNanos = TimeUnit.MILLISECONDS.toNanos(10);
        for (int r = 0; r < 4000; r++) {
            assertTrue(run(iterationDurationNanos, iterationDurationNanos / 100));
        }
    }

    private boolean run(final long iterationDurationNanos, final long ttlNanos) throws Throwable {
        final Supplier<Long> s = new Supplier<Long>() {
            private long value;

            @Override
            public Long get() {// this method is not thread-safe, the counter may go down if MemoizedObject
                                     // calls this method concurrently
                return ++value;
            }
        };
        final Supplier<Long> m = Memoizer.memoize(s, ttlNanos);
        final Collection<Future<Void>> results = new ArrayList<>();
        final long beginNanos = System.nanoTime();
        for (int tid = 0; tid < numberOfThreads; tid++) {
            results.add(ex.submit(() -> {
                Long previousValue = m.get();
                final long firstSupplierCallNanos = System.nanoTime();
                assertNotNull(previousValue);
                assertTrue(previousValue > 0);
                // this loop is guaranteed to be executed at least once regardless of the values
                // returned by System.nanoTime
                long now;
                do {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    final Long newValue = m.get();
                    assertNotNull(newValue);// check that we never get uninitialized value
                    assertTrue(String.format("newValue=%s, previousValue=%s", newValue, previousValue),
                            newValue >= previousValue);// check that the counter never goes down
                    previousValue = newValue;
                } while ((now = System.nanoTime()) - beginNanos < iterationDurationNanos
                        // always loop enough to make sure newValue increments
                        || now - firstSupplierCallNanos < ttlNanos);
                return null;
            }));
        }
        for (final Future<Void> result : results) {// make sure all the submitted tasks finished correctly
            try {
                result.get();
            } catch (final ExecutionException e) {
                throw e.getCause();
            }
        }
        /*
         * Calculation of expectedNumberOfRefreshes is a bit tricky because there is no
         * such thing. We can only talk about min and max possible values.
         *
         * Min: Two refreshes are guaranteed: the initial one, because it does not
         * depend on timings, and a second one which ensures at least ttlNanos have
         * elapsed since the first one. All other refreshes may or may not happen
         * depending on the timings. Therefore the min is 2.
         *
         * Max: Each thread has a chance to refresh one more time after
         * (iterationDurationNanos / ttlNanos) refreshes have been collectively done.
         * This happens because an arbitrary amount of time may elapse between the
         * instant when a thread enters the while cycle body for the last time (the last
         * iteration), and the instant that is observed by the MemoizedObject.get
         * method. Additionally, each thread may refresh one more time because of the
         * last iteration of the loop caused by ttl expiration. Therefore each thread
         * may do up to 2 additional refreshes.
         */
        final double minExpectedNumberOfRefreshes = ttlNanos > 0 ? 2 : 1;
        final double maxExpectedNumberOfRefreshes = ttlNanos > 0
                ? ((double) iterationDurationNanos / ttlNanos) + 2 * numberOfThreads
                : 1;
        // all the writes to s.value field happened-before this read because of all the
        // result.get() invocations
        // Want to test s.value but it's private. s.get() will increment before
        // returning, so subtract 1 to get what the internal value was
        final long actualNumberOfRefreshes = s.get() - 1;
        assertTrue(String.format(
                "ttlNanos=%s, minExpectedNumberOfRefreshes=%s, maxExpectedNumberOfRefreshes=%s, actualNumberOfRefreshes=%s",
                ttlNanos, minExpectedNumberOfRefreshes, maxExpectedNumberOfRefreshes, actualNumberOfRefreshes),
                minExpectedNumberOfRefreshes <= actualNumberOfRefreshes
                        && actualNumberOfRefreshes <= maxExpectedNumberOfRefreshes);
        return true;
    }
}
