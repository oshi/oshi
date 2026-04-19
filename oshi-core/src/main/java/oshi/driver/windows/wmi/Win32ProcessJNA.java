/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Process;
import oshi.driver.common.windows.wmi.Win32Process.CommandLineProperty;
import oshi.driver.common.windows.wmi.Win32Process.ProcessXPProperty;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_Process} using JNA.
 */
@ThreadSafe
public final class Win32ProcessJNA extends Win32Process {

    private Win32ProcessJNA() {
    }

    /**
     * Returns process command lines.
     *
     * @param pidsToQuery Process IDs to query for command lines. Pass {@code null} to query all processes.
     * @return A {@link WmiResult} containing process IDs and command lines used to start the provided processes.
     */
    public static WmiResult<CommandLineProperty> queryCommandLines(Set<Integer> pidsToQuery) {
        WmiQuery<CommandLineProperty> commandLineQuery = new WmiQuery<>(buildWmiClassNameWithPidFilter(pidsToQuery),
                CommandLineProperty.class);
        return Objects
                .requireNonNull(WmiQueryHandler.createInstance(), "WmiQueryHandler.createInstance() returned null")
                .queryWMI(commandLineQuery);
    }

    /**
     * Returns process info.
     *
     * @param pids Process IDs to query.
     * @return Information on the provided processes.
     */
    public static WmiResult<ProcessXPProperty> queryProcesses(Collection<Integer> pids) {
        WmiQuery<ProcessXPProperty> processQueryXP = new WmiQuery<>(buildWmiClassNameWithPidFilter(pids),
                ProcessXPProperty.class);
        return Objects
                .requireNonNull(WmiQueryHandler.createInstance(), "WmiQueryHandler.createInstance() returned null")
                .queryWMI(processQueryXP);
    }
}
