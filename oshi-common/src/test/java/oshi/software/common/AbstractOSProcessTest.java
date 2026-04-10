/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.software.os.OSThread;

class AbstractOSProcessTest {

    private static AbstractOSProcess createProcess(int pid, long kernelTime, long userTime, long upTime) {
        return new AbstractOSProcess(pid) {
            @Override
            public String getName() {
                return "proc" + pid;
            }

            @Override
            public String getPath() {
                return "/bin/proc";
            }

            @Override
            public String getCommandLine() {
                return "proc";
            }

            @Override
            public List<String> getArguments() {
                return Collections.emptyList();
            }

            @Override
            public Map<String, String> getEnvironmentVariables() {
                return Collections.emptyMap();
            }

            @Override
            public String getCurrentWorkingDirectory() {
                return "/";
            }

            @Override
            public String getUser() {
                return "user";
            }

            @Override
            public String getUserID() {
                return "1000";
            }

            @Override
            public String getGroup() {
                return "group";
            }

            @Override
            public String getGroupID() {
                return "1000";
            }

            @Override
            public State getState() {
                return State.RUNNING;
            }

            @Override
            public int getParentProcessID() {
                return 1;
            }

            @Override
            public int getThreadCount() {
                return 1;
            }

            @Override
            public int getPriority() {
                return 0;
            }

            @Override
            public long getVirtualSize() {
                return 0L;
            }

            @Override
            public long getResidentMemory() {
                return 0L;
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
            public long getBytesRead() {
                return 0L;
            }

            @Override
            public long getBytesWritten() {
                return 0L;
            }

            @Override
            public long getOpenFiles() {
                return 0L;
            }

            @Override
            public long getSoftOpenFileLimit() {
                return 0L;
            }

            @Override
            public long getHardOpenFileLimit() {
                return 0L;
            }

            @Override
            public int getBitness() {
                return 64;
            }

            @Override
            public long getAffinityMask() {
                return 0L;
            }

            @Override
            public boolean updateAttributes() {
                return false;
            }

            @Override
            public List<OSThread> getThreadDetails() {
                return Collections.emptyList();
            }
        };
    }

    @Test
    void testGetProcessID() {
        assertThat(createProcess(42, 0, 0, 0).getProcessID(), is(42));
    }

    @Test
    void testCpuLoadCumulativeZeroUptime() {
        assertThat(createProcess(1, 100, 200, 0).getProcessCpuLoadCumulative(), is(0d));
    }

    @Test
    void testCpuLoadCumulative() {
        // (300 + 700) / 2000 = 0.5
        assertThat(createProcess(1, 300, 700, 2000).getProcessCpuLoadCumulative(), is(closeTo(0.5, 0.001)));
    }

    @Test
    void testCpuLoadBetweenTicksWithPrior() {
        AbstractOSProcess prior = createProcess(1, 100, 100, 1000);
        AbstractOSProcess current = createProcess(1, 200, 300, 2000);
        // (200-100 + 300-100) / (2000-1000) = 0.3
        assertThat(current.getProcessCpuLoadBetweenTicks(prior), is(closeTo(0.3, 0.001)));
    }

    @Test
    void testCpuLoadBetweenTicksNullPriorFallsToCumulative() {
        AbstractOSProcess current = createProcess(1, 300, 700, 2000);
        assertThat(current.getProcessCpuLoadBetweenTicks(null), is(closeTo(0.5, 0.001)));
    }

    @Test
    void testCpuLoadBetweenTicksDifferentPidFallsToCumulative() {
        AbstractOSProcess prior = createProcess(99, 100, 100, 1000);
        AbstractOSProcess current = createProcess(1, 300, 700, 2000);
        assertThat(current.getProcessCpuLoadBetweenTicks(prior), is(closeTo(0.5, 0.001)));
    }

    @Test
    void testCpuLoadBetweenTicksNonIncreasingUptimeFallsToCumulative() {
        AbstractOSProcess prior = createProcess(1, 100, 100, 2000);
        AbstractOSProcess current = createProcess(1, 300, 700, 2000);
        assertThat(current.getProcessCpuLoadBetweenTicks(prior), is(closeTo(0.5, 0.001)));
    }

    @Test
    void testToString() {
        AbstractOSProcess proc = createProcess(42, 0, 0, 0);
        assertThat(proc.toString(), containsString("processID=42"));
        assertThat(proc.toString(), containsString("name=proc42"));
    }
}
