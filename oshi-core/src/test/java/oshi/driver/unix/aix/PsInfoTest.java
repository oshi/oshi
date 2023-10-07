/*
 * Copyright 2022-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.SystemInfo;
import oshi.jna.platform.unix.AixLibc.AixLwpsInfo;
import oshi.jna.platform.unix.AixLibc.AixPsInfo;
import oshi.util.Constants;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

@EnabledOnOs(OS.AIX)
class PsInfoTest {
    @Test
    void testQueryPsInfo() {
        int pid = new SystemInfo().getOperatingSystem().getProcessId();
        AixPsInfo psinfo = PsInfo.queryPsInfo(pid);
        assertThat("Process ID in structure should match PID", psinfo.pr_pid, is((long) pid));

        Triplet<Integer, Long, Long> addrs = PsInfo.queryArgsEnvAddrs(pid, psinfo);
        assertNotNull(addrs);
        Pair<List<String>, Map<String, String>> argsEnv = PsInfo.queryArgsEnv(pid, psinfo);
        assertThat("Arg list size should match argc", argsEnv.getA().size(), is(addrs.getA().intValue()));

        File directory = new File(String.format(Locale.ROOT, "/proc/%d/lwp", pid));
        File[] numericFiles = directory.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());
        assertNotNull(numericFiles);
        for (File lwpidFile : numericFiles) {
            int tid = ParseUtil.parseIntOrDefault(lwpidFile.getName(), 0);
            AixLwpsInfo lwpsinfo = PsInfo.queryLwpsInfo(pid, tid);
            assertThat("Thread ID in structure should match TID", lwpsinfo.pr_lwpid, is((long) tid));
        }
    }
}
