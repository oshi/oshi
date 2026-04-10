/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSThread;
import oshi.software.os.OperatingSystem.OSVersionInfo;
import oshi.util.tuples.Pair;

class AbstractOperatingSystemTest {

    /**
     * Minimal stub process for testing filtering/sorting
     *
     * @param pid the process ID
     * @return a stub process
     */
    private static OSProcess stubProcess(int pid) {
        return new StubProcess(pid);
    }

    private static AbstractOperatingSystem createOS(List<OSProcess> allProcesses) {
        return new AbstractOperatingSystem() {
            @Override
            protected String queryManufacturer() {
                return "TestMfg";
            }

            @Override
            protected Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
                return new Pair<>("TestFamily", new OSVersionInfo("1.0", "Code", "100"));
            }

            @Override
            protected int queryBitness(int jvmBitness) {
                return jvmBitness;
            }

            @Override
            protected List<OSProcess> queryAllProcesses() {
                return allProcesses;
            }

            @Override
            protected List<OSProcess> queryChildProcesses(int parentPid) {
                return allProcesses;
            }

            @Override
            protected List<OSProcess> queryDescendantProcesses(int parentPid) {
                return allProcesses;
            }

            @Override
            public FileSystem getFileSystem() {
                return null;
            }

            @Override
            public InternetProtocolStats getInternetProtocolStats() {
                return null;
            }

            @Override
            public OSProcess getProcess(int pid) {
                return allProcesses.stream().filter(p -> p.getProcessID() == pid).findFirst().orElse(null);
            }

            @Override
            public int getProcessCount() {
                return allProcesses.size();
            }

            @Override
            public int getProcessId() {
                return 1;
            }

            @Override
            public OSThread getCurrentThread() {
                return null;
            }

            @Override
            public int getThreadId() {
                return 1;
            }

            @Override
            public int getThreadCount() {
                return 0;
            }

            @Override
            public long getSystemUptime() {
                return 0L;
            }

            @Override
            public long getSystemBootTime() {
                return 0L;
            }

            @Override
            public NetworkParams getNetworkParams() {
                return null;
            }
        };
    }

    @Test
    void testMemoizedFields() {
        AbstractOperatingSystem os = createOS(Collections.emptyList());
        assertThat(os.getManufacturer(), is("TestMfg"));
        assertThat(os.getFamily(), is("TestFamily"));
        assertThat(os.getVersionInfo().getVersion(), is("1.0"));
        assertThat(os.getBitness(), is(greaterThanOrEqualTo(32)));
        // Verify memoization returns same instances
        assertThat(os.getVersionInfo(), is(sameInstance(os.getVersionInfo())));
    }

    @Test
    void testGetProcessesNoFilterNoSortNoLimit() {
        AbstractOperatingSystem os = createOS(Arrays.asList(stubProcess(1), stubProcess(2), stubProcess(3)));
        assertThat(os.getProcesses(null, null, 0), hasSize(3));
    }

    @Test
    void testGetProcessesWithFilter() {
        AbstractOperatingSystem os = createOS(Arrays.asList(stubProcess(1), stubProcess(2), stubProcess(3)));
        Predicate<OSProcess> filter = p -> p.getProcessID() > 1;
        assertThat(os.getProcesses(filter, null, 0), hasSize(2));
    }

    @Test
    void testGetProcessesWithSortAndLimit() {
        AbstractOperatingSystem os = createOS(Arrays.asList(stubProcess(1), stubProcess(2), stubProcess(3)));
        Comparator<OSProcess> sort = Comparator.comparingInt(OSProcess::getProcessID).reversed();
        List<OSProcess> result = os.getProcesses(null, sort, 1);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getProcessID(), is(3));
    }

    @Test
    void testToString() {
        AbstractOperatingSystem os = createOS(Collections.emptyList());
        assertThat(os.toString(), containsString("TestMfg"));
        assertThat(os.toString(), containsString("TestFamily"));
    }

    @Test
    void testGetChildrenOrDescendantsImmediateOnly() {
        // Tree: 1 -> 2, 1 -> 3, 2 -> 4
        Map<Integer, Integer> parentPidMap = new HashMap<>();
        parentPidMap.put(1, 0);
        parentPidMap.put(2, 1);
        parentPidMap.put(3, 1);
        parentPidMap.put(4, 2);

        Set<Integer> children = AbstractOperatingSystem.getChildrenOrDescendants(parentPidMap, 1, false);
        assertThat(children.contains(1), is(true));
        assertThat(children.contains(2), is(true));
        assertThat(children.contains(3), is(true));
        assertThat(children.contains(4), is(false));
    }

    @Test
    void testGetChildrenOrDescendantsAllDescendants() {
        Map<Integer, Integer> parentPidMap = new HashMap<>();
        parentPidMap.put(1, 0);
        parentPidMap.put(2, 1);
        parentPidMap.put(3, 1);
        parentPidMap.put(4, 2);

        Set<Integer> descendants = AbstractOperatingSystem.getChildrenOrDescendants(parentPidMap, 1, true);
        assertThat(descendants, hasSize(4));
        assertThat(descendants, containsInAnyOrder(1, 2, 3, 4));
    }

    /** Minimal stub for process filtering/sorting tests. Package-private so other tests could reuse. */
    static class StubProcess implements OSProcess {
        private final int pid;

        StubProcess(int pid) {
            this.pid = pid;
        }

        @Override
        public int getProcessID() {
            return pid;
        }

        @Override
        public int getParentProcessID() {
            return 0;
        }

        @Override
        public String getName() {
            return "stub";
        }

        @Override
        public String getPath() {
            return "";
        }

        @Override
        public String getCommandLine() {
            return "";
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
            return "";
        }

        @Override
        public String getUser() {
            return "";
        }

        @Override
        public String getUserID() {
            return "";
        }

        @Override
        public String getGroup() {
            return "";
        }

        @Override
        public String getGroupID() {
            return "";
        }

        @Override
        public State getState() {
            return State.RUNNING;
        }

        @Override
        public int getThreadCount() {
            return 0;
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public long getVirtualSize() {
            return 0;
        }

        @Override
        public long getResidentMemory() {
            return 0;
        }

        @Override
        public long getKernelTime() {
            return 0;
        }

        @Override
        public long getUserTime() {
            return 0;
        }

        @Override
        public long getUpTime() {
            return 0;
        }

        @Override
        public long getStartTime() {
            return 0;
        }

        @Override
        public long getBytesRead() {
            return 0;
        }

        @Override
        public long getBytesWritten() {
            return 0;
        }

        @Override
        public long getOpenFiles() {
            return 0;
        }

        @Override
        public long getSoftOpenFileLimit() {
            return 0;
        }

        @Override
        public long getHardOpenFileLimit() {
            return 0;
        }

        @Override
        public double getProcessCpuLoadCumulative() {
            return 0;
        }

        @Override
        public double getProcessCpuLoadBetweenTicks(OSProcess proc) {
            return 0;
        }

        @Override
        public int getBitness() {
            return 64;
        }

        @Override
        public long getAffinityMask() {
            return 0;
        }

        @Override
        public boolean updateAttributes() {
            return false;
        }

        @Override
        public List<OSThread> getThreadDetails() {
            return Collections.emptyList();
        }
    }
}
