/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.netbsd;

import static oshi.software.common.os.unix.bsd.BsdPsKeyword.ARGS;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.COMM;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.CPUTIME;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.ETIME;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.GID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.GROUP;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.MAJFLT;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.MINFLT;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.NIVCSW;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.NVCSW;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.PID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.PPID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.PRI;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.RSS;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.STATE;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.UID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.USER;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.VSZ;
import static oshi.software.os.OSThread.ThreadFiltering.VALID_THREAD;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.unix.bsd.BsdOSProcess;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.netbsd.FstatUtil;
import oshi.util.common.platform.unix.netbsd.NetBsdSysctlUtil;

/**
 * OSProcess implementation
 */
@ThreadSafe
public class NetBsdOSProcess extends BsdOSProcess {

    /**
     * Ordered {@code ps} columns queried for each process. Shared by NetBsdOSProcess and the NetBSD OperatingSystem so
     * the column list and parsing stay in lockstep. {@code ARGS} must remain last.
     */
    public static final List<BsdPsKeyword> PS_KEYWORDS = Collections.unmodifiableList(Arrays.asList(STATE, PID, PPID,
            USER, UID, GROUP, GID, PRI, VSZ, RSS, ETIME, CPUTIME, COMM, MAJFLT, MINFLT, NVCSW, NIVCSW, ARGS));

    public static final String PS_COMMAND_ARGS = PS_KEYWORDS.stream().map(Enum::name)
            .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.joining(","));

    /*
     * Package-private for use by NetBsdOSThread
     */
    enum PsThreadColumns {
        LID, STATE, ETIME, CPUTIME, NIVCSW, NVCSW, MAJFLT, MINFLT, PRI, ARGS;
    }

    static final String PS_THREAD_COLUMNS = Arrays.stream(PsThreadColumns.values()).map(Enum::name)
            .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.joining(","));

    private final NetBsdOperatingSystem os;

    public NetBsdOSProcess(int pid, Map<BsdPsKeyword, String> psMap, NetBsdOperatingSystem os) {
        super(pid);
        this.os = os;
        updateAttributes(psMap);
    }

    @Override
    protected List<BsdPsKeyword> psKeywords() {
        return PS_KEYWORDS;
    }

    @Override
    protected String psCommandArgs() {
        return PS_COMMAND_ARGS;
    }

    @Override
    protected List<String> queryArguments() {
        // NetBSD provides command line via /proc filesystem
        byte[] cmdBytes = FileUtil.readAllBytes("/proc/" + getProcessID() + "/cmdline", false);
        if (cmdBytes != null && cmdBytes.length > 0) {
            return Collections.unmodifiableList(ParseUtil.parseByteArrayToStrings(cmdBytes));
        }
        return Collections.emptyList();
    }

    @Override
    protected Map<String, String> queryEnvironmentVariables() {
        // For the current process, use Java's System.getenv()
        if (getProcessID() == this.os.getProcessId()) {
            return System.getenv();
        }
        // Environment of other processes is not accessible without JNA on NetBSD
        return Collections.emptyMap();
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return FstatUtil.getCwd(getProcessID());
    }

    @Override
    public long getOpenFiles() {
        return FstatUtil.getOpenFiles(getProcessID());
    }

    @Override
    public long getSoftOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            long limit = NetBsdSysctlUtil.sysctl("kern.maxfilesperproc", 0L);
            if (limit <= 0) {
                limit = NetBsdSysctlUtil.sysctl("kern.maxfiles", 0L);
            }
            return limit > 0 ? limit : -1L;
        }
        return -1L;
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            long limit = NetBsdSysctlUtil.sysctl("kern.maxfiles", 0L);
            return limit > 0 ? limit : -1L;
        }
        return -1L;
    }

    @Override
    protected int queryBitness() {
        // NetBSD does not maintain a compatibility layer.
        // Process bitness is OS bitness
        return System.getProperty("os.arch", "").contains("64") ? 64 : 32;
    }

    @Override
    public long getAffinityMask() {
        long bitMask = 0L;
        // schedctl -p <pid> shows affinity; -A cpus would set it
        List<String> schedctl = ExecutingCommand.runNative("schedctl -p " + getProcessID());
        for (String line : schedctl) {
            // Output includes "Affinity: <list>" when bound
            if (line.contains("Affinity:")) {
                String[] parts = line.split("Affinity:")[1].trim().split("[,\\s]+");
                for (String part : parts) {
                    int bitToSet = ParseUtil.parseIntOrDefault(part.trim(), -1);
                    if (bitToSet >= 0) {
                        bitMask |= 1L << bitToSet;
                    }
                }
                return bitMask;
            }
        }
        // Not bound — return all CPUs
        int ncpu = NetBsdSysctlUtil.sysctl("hw.ncpuonline", 1);
        return ncpu < 64 ? (1L << ncpu) - 1 : -1L;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        // NetBSD ps shows per-LWP rows when lid is in the format specifier
        String psCommand = "ps -awwxo " + PS_THREAD_COLUMNS;
        if (getProcessID() >= 0) {
            psCommand += " -p " + getProcessID();
        }
        Predicate<Map<PsThreadColumns, String>> hasColumnsArgs = threadMap -> threadMap
                .containsKey(PsThreadColumns.ARGS);
        return ExecutingCommand.runNative(psCommand).stream().skip(1)
                .map(thread -> ParseUtil.stringToEnumMap(PsThreadColumns.class, thread.trim(), ' '))
                .filter(hasColumnsArgs).map(threadMap -> new NetBsdOSThread(getProcessID(), threadMap))
                .filter(VALID_THREAD).collect(Collectors.toList());
    }

    @Override
    protected void updateThreadCount() {
        // Use nlwp keyword to get LWP count for this process
        List<String> nlwpList = ExecutingCommand.runNative("ps -o nlwp -p " + getProcessID());
        if (nlwpList.size() > 1) {
            this.threadCount = ParseUtil.parseIntOrDefault(nlwpList.get(1).trim(), 1);
        } else {
            this.threadCount = 1;
        }
    }
}
