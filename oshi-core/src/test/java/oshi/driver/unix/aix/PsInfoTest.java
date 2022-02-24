/*
 * MIT License
 *
 * Copyright (c) 2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.driver.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.List;
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

        File directory = new File(String.format("/proc/%d/lwp", pid));
        File[] numericFiles = directory.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());
        assertNotNull(numericFiles);
        for (File lwpidFile : numericFiles) {
            int tid = ParseUtil.parseIntOrDefault(lwpidFile.getName(), 0);
            AixLwpsInfo lwpsinfo = PsInfo.queryLwpsInfo(pid, tid);
            assertThat("Thread ID in structure should match TID", lwpsinfo.pr_lwpid, is((long) tid));
        }
    }
}
