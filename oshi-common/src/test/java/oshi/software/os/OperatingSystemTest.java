/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import oshi.software.os.OperatingSystem.OSVersionInfo;

class OperatingSystemTest {

    /**
     * Base stub providing shared no-op implementations for OperatingSystem methods. Subclasses override only the
     * methods relevant to each test scenario.
     */
    private abstract static class StubOperatingSystem implements OperatingSystem {
        @Override
        public FileSystem getFileSystem() {
            return null;
        }

        @Override
        public InternetProtocolStats getInternetProtocolStats() {
            return null;
        }

        @Override
        public List<OSProcess> getProcesses(Predicate<OSProcess> filter, Comparator<OSProcess> sort, int limit) {
            return Collections.emptyList();
        }

        @Override
        public OSProcess getProcess(int pid) {
            return null;
        }

        @Override
        public List<OSProcess> getChildProcesses(int parentPid, Predicate<OSProcess> filter, Comparator<OSProcess> sort,
                int limit) {
            return Collections.emptyList();
        }

        @Override
        public List<OSProcess> getDescendantProcesses(int parentPid, Predicate<OSProcess> filter,
                Comparator<OSProcess> sort, int limit) {
            return Collections.emptyList();
        }

        @Override
        public int getProcessCount() {
            return 0;
        }

        @Override
        public OSThread getCurrentThread() {
            return null;
        }

        @Override
        public int getThreadCount() {
            return 0;
        }

        @Override
        public int getBitness() {
            return 64;
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
    }

    // Minimal stub — returns null for getProcess(pid), exercises default method behavior
    private static final OperatingSystem MINIMAL = new StubOperatingSystem() {
        @Override
        public String getFamily() {
            return "TestOS";
        }

        @Override
        public String getManufacturer() {
            return "TestCorp";
        }

        @Override
        public OSVersionInfo getVersionInfo() {
            return new OSVersionInfo("1.0", "Codename", "1000");
        }

        @Override
        public int getProcessId() {
            return 1;
        }

        @Override
        public int getThreadId() {
            return 1;
        }
    };

    private static final OperatingSystem OVERRIDING = new StubOperatingSystem() {
        @Override
        public String getFamily() {
            return "OverrideOS";
        }

        @Override
        public String getManufacturer() {
            return "OverrideCorp";
        }

        @Override
        public OSVersionInfo getVersionInfo() {
            return new OSVersionInfo("2.0", null, null);
        }

        @Override
        public int getProcessId() {
            return 2;
        }

        @Override
        public int getThreadId() {
            return 2;
        }

        @Override
        public boolean isElevated() {
            return true;
        }

        @Override
        public List<OSService> getServices() {
            return Collections.singletonList(new OSService("svc", 1, OSService.State.RUNNING));
        }

        @Override
        public List<OSSession> getSessions() {
            return Collections.singletonList(new OSSession("user", "tty1", 0L, "localhost"));
        }

        @Override
        public List<OSDesktopWindow> getDesktopWindows(boolean visibleOnly) {
            return Collections.singletonList(
                    new OSDesktopWindow(1L, "Title", "cmd", new java.awt.Rectangle(0, 0, 100, 100), 1L, 0, true));
        }

        @Override
        public List<ApplicationInfo> getInstalledApplications() {
            return Collections.singletonList(new ApplicationInfo("App", "1.0", "Vendor", 0L, null));
        }
    };

    @Test
    void testDefaultGetProcessesNoArg() {
        assertThat(MINIMAL.getProcesses(), is(notNullValue()));
        assertThat(MINIMAL.getProcesses(), is(empty()));
    }

    @Test
    void testDefaultGetProcessesCollection() {
        // getProcess returns null, so filtered out
        assertThat(MINIMAL.getProcesses(Arrays.asList(1, 2, 3)), is(empty()));
    }

    @Test
    void testDefaultGetCurrentProcess() {
        // getProcess(getProcessId()) returns null in MINIMAL
        assertThat(MINIMAL.getCurrentProcess(), is((Object) null));
    }

    @Test
    void testDefaultIsElevated() {
        assertThat(MINIMAL.isElevated(), is(false));
    }

    @Test
    void testDefaultGetServices() {
        assertThat(MINIMAL.getServices(), is(notNullValue()));
        assertThat(MINIMAL.getServices(), is(empty()));
    }

    @Test
    void testDefaultGetSessions() {
        assertThat(MINIMAL.getSessions(), is(notNullValue()));
        assertThat(MINIMAL.getSessions(), is(empty()));
    }

    @Test
    void testDefaultGetDesktopWindows() {
        assertThat(MINIMAL.getDesktopWindows(true), is(notNullValue()));
        assertThat(MINIMAL.getDesktopWindows(true), is(empty()));
    }

    @Test
    void testDefaultGetInstalledApplications() {
        assertThat(MINIMAL.getInstalledApplications(), is(notNullValue()));
        assertThat(MINIMAL.getInstalledApplications(), is(empty()));
    }

    @Test
    void testOverriddenDefaults() {
        assertThat(OVERRIDING.isElevated(), is(true));
        assertThat(OVERRIDING.getServices(), hasSize(1));
        assertThat(OVERRIDING.getSessions(), hasSize(1));
        assertThat(OVERRIDING.getDesktopWindows(true), hasSize(1));
        assertThat(OVERRIDING.getInstalledApplications(), hasSize(1));
    }

    @Test
    void testOSVersionInfoWithCodeNameAndBuild() {
        OSVersionInfo v = new OSVersionInfo("11", "Bullseye", "20211010");
        assertThat(v.getVersion(), is("11"));
        assertThat(v.getCodeName(), is("Bullseye"));
        assertThat(v.getBuildNumber(), is("20211010"));
        assertThat(v.toString(), is("11 (Bullseye) build 20211010"));
    }

    @Test
    void testOSVersionInfoNullCodeNameAndBuild() {
        OSVersionInfo v = new OSVersionInfo("2.0", null, null);
        assertThat(v.toString(), is("2.0"));
    }
}
