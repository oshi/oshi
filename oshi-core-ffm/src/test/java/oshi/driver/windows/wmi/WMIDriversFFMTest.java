/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;

import oshi.driver.common.windows.wmi.Win32Process.CommandLineProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.ffm.SystemInfo;

@EnabledForJreRange(min = JRE.JAVA_25)
@EnabledOnOs(OS.WINDOWS)
class WMIDriversFFMTest {

    @Test
    void testQueryWMIWin32Process() {
        WmiResult<CommandLineProperty> cl = Win32ProcessFFM.queryCommandLines(null);
        assertThat("Failed Win32Process.queryCommandLines", cl.getResultCount(), is(greaterThan(0)));
        Set<Integer> clset = IntStream.range(0, cl.getResultCount())
                .map(i -> WmiUtil.getUint32(cl, CommandLineProperty.PROCESSID, i)).boxed().collect(Collectors.toSet());
        assertThat("Failed Win32Process.queryProcesses", Win32ProcessFFM.queryProcesses(clset).getResultCount(),
                is(both(greaterThan(0)).and(lessThanOrEqualTo(clset.size()))));

        Win32ProcessCachedFFM cache = Win32ProcessCachedFFM.getInstance();
        int processId = new SystemInfo().getOperatingSystem().getProcessId();
        assertThat("Failed Win32Process.queryCommandLines for current process", clset.contains(processId), is(true));
        String commandLine = cache.getCommandLine(processId, 0L);
        assertThat("Failed Win32ProcessCached.getCommandLine", commandLine, is(notNullValue()));
        assertThat("Failed Win32ProcessCached.getCommandLine cached lookup", cache.getCommandLine(processId, 0L),
                is(commandLine));
    }
}
