/*
 * Copyright 2022-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.solaris;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.SystemInfo;
import oshi.jna.platform.unix.SolarisLibc.SolarisLwpsInfo;
import oshi.jna.platform.unix.SolarisLibc.SolarisPsInfo;
import oshi.util.Constants;
import oshi.util.ParseUtil;

@EnabledOnOs(OS.SOLARIS)
class PsInfoTest {
    @Test
    void testQueryPsInfo() {
        int pid = new SystemInfo().getOperatingSystem().getProcessId();
        SolarisPsInfo psinfo = PsInfo.queryPsInfo(pid);
        assertThat("Process ID in structure should match PID", psinfo.pr_pid, is(pid));
        File directory = new File(String.format(Locale.ROOT, "/proc/%d/lwp", pid));
        File[] numericFiles = directory.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());
        assertNotNull(numericFiles);
        for (File lwpidFile : numericFiles) {
            int tid = ParseUtil.parseIntOrDefault(lwpidFile.getName(), 0);
            SolarisLwpsInfo lwpsinfo = PsInfo.queryLwpsInfo(pid, tid);
            assertThat("Thread ID in structure should match TID", lwpsinfo.pr_lwpid, is(tid));
        }
    }
}
