/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux.proc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.driver.linux.proc.ProcessStat.PidStat;
import oshi.driver.linux.proc.ProcessStat.PidStatM;
import oshi.software.os.OSProcess;
import oshi.software.os.OSProcess.State;
import oshi.util.ParseUtil;
import oshi.util.tuples.Triplet;

@EnabledOnOs(OS.LINUX)
class ProcessStatTest {

    @Test
    void testGetPidFiles() {
        File[] files = ProcessStat.getPidFiles();
        assertThat("Files should be non-empty array", files.length, greaterThan(0));
        // No need to test every file, but no guarantee any one file exists due to race
        // condition. Track that each test succeeds at least once.
        boolean fileDescriptorFilesTested = false;
        boolean pidStatMTested = false;
        boolean pidStatsTested = false;
        boolean threadIdsTested = false;
        for (File f : files) {
            int pid = ParseUtil.parseIntOrDefault(f.getName(), -1);
            assertThat("Pid " + f.getName() + " must be numeric", pid, greaterThanOrEqualTo(0));
            if (!fileDescriptorFilesTested) {
                fileDescriptorFilesTested = testGetFileDescriptorFiles(pid);
            }
            if (!pidStatMTested) {
                pidStatMTested = testGetPidStatM(pid);
            }
            if (!pidStatsTested) {
                pidStatsTested = testGetPidStats(pid);
            }
            if (!threadIdsTested) {
                threadIdsTested = testGetThreadIds(pid);
            }
            // If every test has passed at least once, we're done
            if (fileDescriptorFilesTested && pidStatMTested && pidStatsTested && threadIdsTested) {
                break;
            }
        }
        assertTrue(fileDescriptorFilesTested, "File Descriptor tests failed");
        assertTrue(pidStatMTested, "PidStatM tests failed");
        assertTrue(pidStatsTested, "PidStats tests failed");
        assertTrue(threadIdsTested, "ThreadIds tests failed");
    }

    private boolean testGetFileDescriptorFiles(int pid) {
        // Pids should have STDIN, STDOUT, and STDERR fds
        return ProcessStat.getFileDescriptorFiles(pid).length >= 3;
    }

    private boolean testGetPidStatM(int pid) {
        Map<PidStatM, Long> statM = ProcessStat.getPidStatM(pid);
        assertNotNull(statM);
        return !statM.isEmpty();
    }

    private boolean testGetPidStats(int pid) {
        Triplet<String, Character, Map<PidStat, Long>> stats = ProcessStat.getPidStats(pid);
        assertNotNull(stats);
        State procState = ProcessStat.getState(stats.getB());
        return stats.getA().length() > 0 // at least one process should have nonempty name
                // At least one process should be running or sleeping
                && (procState.equals(OSProcess.State.RUNNING) || procState.equals(OSProcess.State.SLEEPING))
                // Have keys at least up to RSS
                && stats.getC().containsKey(PidStat.RSS);
    }

    private boolean testGetThreadIds(int pid) {
        // Every process should have at least one thread (itself)!
        return !ProcessStat.getThreadIds(pid).isEmpty();
    }

    @Test
    void testQuerySocketToPidMap() {
        assertThat("Socket to pid map shouldn't be empty.", ProcessStat.querySocketToPidMap().size(), greaterThan(0));
    }
}
