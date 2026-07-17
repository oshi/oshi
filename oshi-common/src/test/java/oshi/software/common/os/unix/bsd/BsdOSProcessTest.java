/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.bsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

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
}
