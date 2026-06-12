/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.openbsd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.unix.bsd.BsdOperatingSystem;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Abstract base shared by the OpenBSD OperatingSystem implementations (JNA and FFM). The cross-BSD-common pieces live
 * in {@link BsdOperatingSystem}; this layer adds the OpenBSD {@code ps} process enumeration, the {@code ps -axHo tid}
 * thread count, the text-parsed boot time, and the (non-native) internet protocol stats and network params. The native
 * bits (the {@link OSProcess} and {@code FileSystem} factories, {@code getpid}/{@code getthrid}, and the sysctl version
 * query) are provided by the JNA and FFM subclasses.
 */
@ThreadSafe
public abstract class OpenBsdOperatingSystem extends BsdOperatingSystem {

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new OpenBsdInternetProtocolStats();
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new OpenBsdNetworkParams();
    }

    @Override
    protected List<OSProcess> getProcessListFromPS(int pid) {
        List<OSProcess> procs = new ArrayList<>();
        // https://man.openbsd.org/ps#KEYWORDS
        // missing are threadCount and kernelTime which is included in cputime
        String psCommand = "ps -awwxo " + OpenBsdOSProcess.PS_COMMAND_ARGS;
        if (pid >= 0) {
            psCommand += " -p " + pid;
        }
        List<String> procList = ExecutingCommand.runNative(psCommand);
        if (procList.isEmpty() || procList.size() < 2) {
            return procs;
        }

        // remove header row
        procList.remove(0);
        // Fill list
        for (String proc : procList) {
            Map<BsdPsKeyword, String> psMap = ParseUtil.stringToEnumMap(BsdPsKeyword.class,
                    OpenBsdOSProcess.PS_KEYWORDS, proc.trim(), ' ');
            // Check if last (thus all) value populated
            if (psMap.containsKey(BsdPsKeyword.ARGS)) {
                procs.add(createProcess(pid < 0 ? ParseUtil.parseIntOrDefault(psMap.get(BsdPsKeyword.PID), 0) : pid,
                        psMap));
            }
        }
        return procs;
    }

    @Override
    public OSThread getCurrentThread() {
        OSProcess proc = getCurrentProcess();
        final int tid = getThreadId();
        return proc.getThreadDetails().stream().filter(t -> t.getThreadId() == tid).findFirst()
                .orElse(new OpenBsdOSThread(proc.getProcessID(), tid));
    }

    @Override
    public int getThreadCount() {
        // -H "Also display information about kernel visible threads"
        // -k "Also display information about kernel threads"
        // column TID holds thread ID
        List<String> threadList = ExecutingCommand.runNative("ps -axHo tid");
        if (!threadList.isEmpty()) {
            // Subtract 1 for header
            return threadList.size() - 1;
        }
        return 0;
    }

    @Override
    protected long queryBootTime() {
        // Boot time will be the first consecutive string of digits.
        return ParseUtil.parseLongOrDefault(
                ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                System.currentTimeMillis() / 1000);
    }

    /**
     * Creates a backend-specific {@link OSProcess} from a parsed {@code ps} row.
     *
     * @param pid   the process ID
     * @param psMap the parsed {@code ps} columns for this process
     * @return a new OSProcess
     */
    protected abstract OSProcess createProcess(int pid, Map<BsdPsKeyword, String> psMap);
}
