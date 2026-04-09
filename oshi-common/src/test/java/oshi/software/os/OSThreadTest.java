/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.software.os.OSProcess.State;

class OSThreadTest {

    private static final OSThread MINIMAL = new OSThread() {
        @Override
        public int getThreadId() {
            return 1;
        }

        @Override
        public State getState() {
            return State.RUNNING;
        }

        @Override
        public double getThreadCpuLoadCumulative() {
            return 0.0;
        }

        @Override
        public double getThreadCpuLoadBetweenTicks(OSThread thread) {
            return 0.0;
        }

        @Override
        public int getOwningProcessId() {
            return 42;
        }

        @Override
        public long getKernelTime() {
            return 0L;
        }

        @Override
        public long getUserTime() {
            return 0L;
        }

        @Override
        public long getUpTime() {
            return 0L;
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

    private static final OSThread OVERRIDING = new OSThread() {
        @Override
        public int getThreadId() {
            return 2;
        }

        @Override
        public State getState() {
            return State.SLEEPING;
        }

        @Override
        public double getThreadCpuLoadCumulative() {
            return 0.0;
        }

        @Override
        public double getThreadCpuLoadBetweenTicks(OSThread thread) {
            return 0.0;
        }

        @Override
        public int getOwningProcessId() {
            return 99;
        }

        @Override
        public long getKernelTime() {
            return 0L;
        }

        @Override
        public long getUserTime() {
            return 0L;
        }

        @Override
        public long getUpTime() {
            return 0L;
        }

        @Override
        public long getStartTime() {
            return 0L;
        }

        @Override
        public int getPriority() {
            return 5;
        }

        @Override
        public String getName() {
            return "worker-1";
        }

        @Override
        public long getStartMemoryAddress() {
            return 0xDEADBEEFL;
        }

        @Override
        public long getContextSwitches() {
            return 7L;
        }

        @Override
        public long getMinorFaults() {
            return 3L;
        }

        @Override
        public long getMajorFaults() {
            return 1L;
        }

        @Override
        public boolean updateAttributes() {
            return true;
        }
    };

    @Test
    void testDefaultGetName() {
        assertThat(MINIMAL.getName(), is(""));
    }

    @Test
    void testDefaultGetStartMemoryAddress() {
        assertThat(MINIMAL.getStartMemoryAddress(), is(0L));
    }

    @Test
    void testDefaultGetContextSwitches() {
        assertThat(MINIMAL.getContextSwitches(), is(0L));
    }

    @Test
    void testDefaultGetMinorFaults() {
        assertThat(MINIMAL.getMinorFaults(), is(0L));
    }

    @Test
    void testDefaultGetMajorFaults() {
        assertThat(MINIMAL.getMajorFaults(), is(0L));
    }

    @Test
    void testDefaultUpdateAttributes() {
        assertThat(MINIMAL.updateAttributes(), is(false));
    }

    @Test
    void testOverriddenDefaults() {
        assertThat(OVERRIDING.getName(), is("worker-1"));
        assertThat(OVERRIDING.getStartMemoryAddress(), is(0xDEADBEEFL));
        assertThat(OVERRIDING.getContextSwitches(), is(7L));
        assertThat(OVERRIDING.getMinorFaults(), is(3L));
        assertThat(OVERRIDING.getMajorFaults(), is(1L));
        assertThat(OVERRIDING.updateAttributes(), is(true));
    }
}
