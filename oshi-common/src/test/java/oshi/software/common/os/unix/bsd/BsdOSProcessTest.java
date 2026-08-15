/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.bsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.software.os.OSProcess;
import oshi.software.os.OSThread;

/**
 * Tests the shared open-file-limit skeleton and ELF-bitness parsing hoisted into {@link BsdOSProcess}.
 */
class BsdOSProcessTest {

    // Builds a minimal BSD process whose open-file-limit hooks return sentinel values so the base's routing (current
    // process via getrlimit vs. other process via the fallback) can be observed.
    private static BsdOSProcess stubProcess(int pid, int ownPid) {
        return new BsdOSProcess(pid) {
            @Override
            protected List<BsdPsKeyword> psKeywords() {
                return Collections.emptyList();
            }

            @Override
            protected String psCommandArgs() {
                return "";
            }

            @Override
            protected List<String> queryArguments() {
                return Collections.emptyList();
            }

            @Override
            protected Map<String, String> queryEnvironmentVariables() {
                return Collections.emptyMap();
            }

            @Override
            protected int queryBitness() {
                return 0;
            }

            @Override
            protected int queryOwnProcessId() {
                return ownPid;
            }

            @Override
            protected long queryRlimitNofile(boolean soft) {
                return soft ? 1024L : 4096L;
            }

            @Override
            protected long otherProcessOpenFileLimit(int index) {
                return index == 1 ? -11L : -22L;
            }

            @Override
            public String getCurrentWorkingDirectory() {
                return "";
            }

            @Override
            public long getOpenFiles() {
                return 0L;
            }

            @Override
            public List<OSThread> getThreadDetails() {
                return Collections.emptyList();
            }
        };
    }

    @Test
    void testOpenFileLimitsForCurrentProcess() {
        // pid matches the current process id, so getrlimit values are returned
        BsdOSProcess self = stubProcess(42, 42);
        assertThat(self.getSoftOpenFileLimit(), is(1024L));
        assertThat(self.getHardOpenFileLimit(), is(4096L));
    }

    @Test
    void testOpenFileLimitsForOtherProcess() {
        // pid differs from the current process id, so the non-current fallback is used
        BsdOSProcess other = stubProcess(42, 7);
        assertThat(other.getSoftOpenFileLimit(), is(-11L));
        assertThat(other.getHardOpenFileLimit(), is(-22L));
    }

    @Test
    void testElfBitness() {
        assertThat(BsdOSProcess.elfBitness("FreeBSD ELF64"), is(64));
        assertThat(BsdOSProcess.elfBitness("FreeBSD ELF32"), is(32));
        assertThat(BsdOSProcess.elfBitness("not an abi string"), is(0));
    }

    // Minimal ps column map for the single-TIME-column platforms (DragonFly, OpenBSD, NetBSD).
    private static Map<BsdPsKeyword, String> psRow(String time) {
        Map<BsdPsKeyword, String> psMap = new EnumMap<>(BsdPsKeyword.class);
        psMap.put(BsdPsKeyword.STATE, "S");
        psMap.put(BsdPsKeyword.PPID, "1");
        psMap.put(BsdPsKeyword.USER, "root");
        psMap.put(BsdPsKeyword.UID, "0");
        psMap.put(BsdPsKeyword.RGID, "0");
        psMap.put(BsdPsKeyword.PRI, "20");
        psMap.put(BsdPsKeyword.VSZ, "1024");
        psMap.put(BsdPsKeyword.RSS, "512");
        psMap.put(BsdPsKeyword.NLWP, "4");
        psMap.put(BsdPsKeyword.TIME, time);
        psMap.put(BsdPsKeyword.ETIMES, "60");
        psMap.put(BsdPsKeyword.UCOMM, "java");
        return psMap;
    }

    @Test
    void testUserTimeIsClampedMonotonic() {
        BsdOSProcess proc = stubProcess(42, 42);
        // First read establishes the value
        proc.updateAttributes(psRow("0:09.78"));
        assertThat(proc.getUserTime(), is(9780L));
        // DragonFly's ps TIME sums only the currently live LWPs, so an exiting thread can take its accumulated CPU
        // time out of the process total. Never report a decrease; clamp to the last value instead.
        proc.updateAttributes(psRow("0:08.25"));
        assertThat(proc.getUserTime(), is(9780L));
        // A genuine increase still comes through
        proc.updateAttributes(psRow("0:10.00"));
        assertThat(proc.getUserTime(), is(10000L));
    }

    @Test
    void testKernelAndUserTimeClampedWithSystimeColumn() {
        BsdOSProcess proc = stubProcess(42, 42);
        // FreeBSD reports systime separately and TIME is user+sys, so user time is a subtraction of two ps columns
        Map<BsdPsKeyword, String> first = psRow("0:10.00");
        first.put(BsdPsKeyword.SYSTIME, "0:04.00");
        proc.updateAttributes(first);
        assertThat(proc.getKernelTime(), is(4000L));
        assertThat(proc.getUserTime(), is(6000L));
        // Both components regress; both are clamped
        Map<BsdPsKeyword, String> second = psRow("0:09.00");
        second.put(BsdPsKeyword.SYSTIME, "0:03.50");
        proc.updateAttributes(second);
        assertThat(proc.getKernelTime(), is(4000L));
        assertThat(proc.getUserTime(), is(6000L));
    }

    /**
     * A ps row missing its STATE column used to throw from charAt(0) partway through updateAttributes, leaving the
     * process half-updated. An absent state now reads as OTHER and the rest of the row is still applied.
     */
    @Test
    void testAbsentStateColumnYieldsOtherRatherThanThrowing() {
        BsdOSProcess proc = stubProcess(42, 42);
        Map<BsdPsKeyword, String> psMap = psRow("0:09.78");
        psMap.remove(BsdPsKeyword.STATE);

        assertThat(proc.updateAttributes(psMap), is(true));
        assertThat(proc.getState(), is(OSProcess.State.OTHER));
        assertThat(proc.getParentProcessID(), is(1));
        assertThat(proc.getUser(), is("root"));
    }

    /**
     * Absent string columns read as the empty string rather than null, which the OSProcess getters promise.
     */
    @Test
    void testAbsentStringColumnsReadAsEmpty() {
        BsdOSProcess proc = stubProcess(42, 42);
        Map<BsdPsKeyword, String> psMap = psRow("0:09.78");
        psMap.remove(BsdPsKeyword.USER);
        psMap.remove(BsdPsKeyword.UID);

        assertThat(proc.updateAttributes(psMap), is(true));
        assertThat(proc.getUser(), is(""));
        assertThat(proc.getUserID(), is(""));
    }
}
