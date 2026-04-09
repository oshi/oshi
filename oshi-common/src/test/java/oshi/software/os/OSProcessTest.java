/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.software.os.OSProcess.State;

class OSProcessTest {

    private static OSProcess processWithResidentMemory(long residentMemory) {
        return new OSProcess() {
            @Override
            public String getName() {
                return "test";
            }

            @Override
            public String getPath() {
                return "/test";
            }

            @Override
            public String getCommandLine() {
                return "test";
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
            public int getProcessID() {
                return 42;
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
                return residentMemory;
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
            public double getProcessCpuLoadCumulative() {
                return 0.0;
            }

            @Override
            public double getProcessCpuLoadBetweenTicks(OSProcess proc) {
                return 0.0;
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
                return true;
            }

            @Override
            public List<OSThread> getThreadDetails() {
                return Collections.emptyList();
            }
        };
    }

    private static final OSProcess MINIMAL = processWithResidentMemory(4096L);

    private static final OSProcess OVERRIDING = new OSProcess() {
        @Override
        public String getName() {
            return "override";
        }

        @Override
        public String getPath() {
            return "/override";
        }

        @Override
        public String getCommandLine() {
            return "override";
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
            return "0";
        }

        @Override
        public String getGroup() {
            return "root";
        }

        @Override
        public String getGroupID() {
            return "0";
        }

        @Override
        public State getState() {
            return State.SLEEPING;
        }

        @Override
        public int getProcessID() {
            return 99;
        }

        @Override
        public int getParentProcessID() {
            return 1;
        }

        @Override
        public int getThreadCount() {
            return 4;
        }

        @Override
        public int getPriority() {
            return 10;
        }

        @Override
        public long getVirtualSize() {
            return 0L;
        }

        @Override
        public long getResidentMemory() {
            return 8192L;
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
        public double getProcessCpuLoadCumulative() {
            return 0.0;
        }

        @Override
        public double getProcessCpuLoadBetweenTicks(OSProcess proc) {
            return 0.0;
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
            return true;
        }

        @Override
        public List<OSThread> getThreadDetails() {
            return Collections.emptyList();
        }

        @Override
        public long getPrivateResidentMemory() {
            return 2048L;
        }

        @Override
        public long getMinorFaults() {
            return 10L;
        }

        @Override
        public long getMajorFaults() {
            return 2L;
        }

        @Override
        public long getContextSwitches() {
            return 5L;
        }
    };

    @Test
    void testDefaultGetPrivateResidentMemoryDelegatesToResident() {
        assertThat(MINIMAL.getPrivateResidentMemory(), is(4096L));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testDefaultGetResidentSetSizeDelegatesToResident() {
        assertThat(MINIMAL.getResidentSetSize(), is(4096L));
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
    void testDefaultGetContextSwitches() {
        assertThat(MINIMAL.getContextSwitches(), is(0L));
    }

    @Test
    void testOverriddenDefaults() {
        assertThat(OVERRIDING.getPrivateResidentMemory(), is(2048L));
        assertThat(OVERRIDING.getMinorFaults(), is(10L));
        assertThat(OVERRIDING.getMajorFaults(), is(2L));
        assertThat(OVERRIDING.getContextSwitches(), is(5L));
    }

    @Test
    void testStateEnum() {
        assertThat(State.valueOf("RUNNING"), is(State.RUNNING));
        assertThat(State.valueOf("INVALID"), is(State.INVALID));
    }
}
