/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.software.os.OSProcess.State;

class AbstractOSThreadTest {

    private static AbstractOSThread createThread(int ownerPid, int threadId, long kernelTime, long userTime,
            long upTime) {
        return new AbstractOSThread(ownerPid) {
            @Override
            public int getThreadId() {
                return threadId;
            }

            @Override
            public State getState() {
                return State.RUNNING;
            }

            @Override
            public long getKernelTime() {
                return kernelTime;
            }

            @Override
            public long getUserTime() {
                return userTime;
            }

            @Override
            public long getUpTime() {
                return upTime;
            }

            @Override
            public long getStartTime() {
                return 0L;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };
    }

    @Test
    void testOwningProcessId() {
        assertThat(createThread(42, 1, 0, 0, 0).getOwningProcessId(), is(42));
    }

    @Test
    void testCpuLoadCumulative() {
        // (400 + 600) / 2000 = 0.5
        assertThat(createThread(1, 1, 400, 600, 2000).getThreadCpuLoadCumulative(), is(closeTo(0.5, 0.001)));
    }

    @Test
    void testCpuLoadCumulativeZeroUptime() {
        assertThat(createThread(1, 1, 100, 200, 0).getThreadCpuLoadCumulative(), is(0d));
    }

    @Test
    void testCpuLoadBetweenTicks() {
        AbstractOSThread prior = createThread(1, 10, 100, 100, 1000);
        AbstractOSThread current = createThread(1, 10, 200, 400, 2000);
        // (200-100 + 400-100) / (2000-1000) = 0.4
        assertThat(current.getThreadCpuLoadBetweenTicks(prior), is(closeTo(0.4, 0.001)));
    }

    @Test
    void testCpuLoadBetweenTicksDifferentThreadFallsToCumulative() {
        AbstractOSThread prior = createThread(1, 99, 100, 100, 1000);
        AbstractOSThread current = createThread(1, 10, 400, 600, 2000);
        assertThat(current.getThreadCpuLoadBetweenTicks(prior), is(closeTo(0.5, 0.001)));
    }

    @Test
    void testCpuLoadBetweenTicksNullPriorFallsToCumulative() {
        AbstractOSThread current = createThread(1, 10, 400, 600, 2000);
        assertThat(current.getThreadCpuLoadBetweenTicks(null), is(closeTo(0.5, 0.001)));
    }

    @Test
    void testCpuLoadBetweenTicksDifferentOwnerFallsToCumulative() {
        AbstractOSThread prior = createThread(99, 10, 100, 100, 1000);
        AbstractOSThread current = createThread(1, 10, 400, 600, 2000);
        assertThat(current.getThreadCpuLoadBetweenTicks(prior), is(closeTo(0.5, 0.001)));
    }

    @Test
    void testCpuLoadBetweenTicksNonIncreasingUptimeFallsToCumulative() {
        AbstractOSThread prior = createThread(1, 10, 100, 100, 2000);
        AbstractOSThread current = createThread(1, 10, 400, 600, 2000);
        assertThat(current.getThreadCpuLoadBetweenTicks(prior), is(closeTo(0.5, 0.001)));
    }

    @Test
    void testToString() {
        AbstractOSThread thread = createThread(42, 7, 0, 0, 0);
        assertThat(thread.toString(), containsString("threadId=7"));
        assertThat(thread.toString(), containsString("owningProcessId=42"));
    }
}
