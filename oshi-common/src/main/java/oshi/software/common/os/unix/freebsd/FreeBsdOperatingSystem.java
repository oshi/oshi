/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

import static oshi.software.os.OperatingSystem.ProcessFiltering.VALID_PROCESS;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.unix.bsd.BsdOperatingSystem;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.os.OSProcess;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Abstract base shared by the FreeBSD OperatingSystem implementations (JNA and FFM). The cross-BSD-common pieces live
 * in {@link BsdOperatingSystem}; this layer adds the FreeBSD {@code ps} process enumeration and current-thread factory.
 * The native bits (the {@link OSProcess}/{@code FileSystem}/{@code NetworkParams}/{@code InternetProtocolStats}
 * factories, {@code getpid}/{@code thr_self}, sysctl version and boot-time queries, and {@code who} sessions) are
 * provided by the JNA and FFM subclasses.
 */
@ThreadSafe
public abstract class FreeBsdOperatingSystem extends BsdOperatingSystem {

    @Override
    protected List<OSProcess> getProcessListFromPS(int pid) {
        String psCommand = "ps -awwxo " + FreeBsdOSProcess.PS_COMMAND_ARGS;
        if (pid >= 0) {
            psCommand += " -p " + pid;
        }

        Predicate<Map<BsdPsKeyword, String>> hasKeywordArgs = psMap -> psMap.containsKey(BsdPsKeyword.ARGS);
        return ExecutingCommand.runNative(psCommand).stream().skip(1).parallel()
                .map(proc -> ParseUtil.stringToEnumMap(BsdPsKeyword.class, FreeBsdOSProcess.PS_KEYWORDS, proc.trim(),
                        ' '))
                .filter(hasKeywordArgs)
                .map(psMap -> createProcess(pid < 0 ? ParseUtil.parseIntOrDefault(psMap.get(BsdPsKeyword.PID), 0) : pid,
                        psMap))
                .filter(VALID_PROCESS).collect(Collectors.toList());
    }

    @Override
    public OSThread getCurrentThread() {
        OSProcess proc = getCurrentProcess();
        final int tid = getThreadId();
        return proc.getThreadDetails().stream().filter(t -> t.getThreadId() == tid).findFirst()
                .orElse(new FreeBsdOSThread(proc.getProcessID(), tid));
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
