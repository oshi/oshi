/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

import static oshi.software.common.os.unix.bsd.BsdPsKeyword.ARGS;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.COMM;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.ETIMES;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.GID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.GROUP;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.MAJFLT;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.MINFLT;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.NIVCSW;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.NLWP;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.NVCSW;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.PID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.PPID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.PRI;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.RSS;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.STATE;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.SYSTIME;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.TIME;
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

import oshi.software.common.os.unix.bsd.BsdOSProcess;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.bsd.BsdPsThreadKeyword;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.freebsd.ProcstatUtil;

/**
 * Abstract base for a FreeBSD OSProcess. Inherits the shared BSD {@code ps} parsing and adds the fields whose column
 * set differs on FreeBSD.
 */
public abstract class FreeBsdOSProcess extends BsdOSProcess {

    /**
     * Ordered {@code ps} columns queried for each process. Shared by the FreeBsdOSProcess subclasses (JNA/FFM) and the
     * FreeBSD OperatingSystem classes so the column list and parsing stay in lockstep. {@code ARGS} must remain last.
     */
    public static final List<BsdPsKeyword> PS_KEYWORDS = Collections
            .unmodifiableList(Arrays.asList(STATE, PID, PPID, USER, UID, GROUP, GID, NLWP, PRI, VSZ, RSS, ETIMES,
                    SYSTIME, TIME, COMM, MAJFLT, MINFLT, NVCSW, NIVCSW, ARGS));

    public static final String PS_COMMAND_ARGS = PS_KEYWORDS.stream().map(Enum::name)
            .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.joining(","));

    /**
     * The {@code KERN_PROC} sysctl subtype that returns a process's executable path.
     */
    protected static final int KERN_PROC_PATHNAME = 12;

    /**
     * The size of the buffer for an executable path, the platform's {@code PATH_MAX}.
     */
    protected static final int PATH_MAX = 1024;

    /**
     * The {@code KERN_PROC} sysctl subtype that returns one resource limit of a process, given its ID and the resource.
     */
    protected static final int KERN_PROC_RLIMIT = 37;

    /**
     * The resource number of the open file limit, {@code RLIMIT_NOFILE}.
     */
    protected static final int RLIMIT_NOFILE_RESOURCE = 8;

    /**
     * The size of the {@code struct rlimit} that {@link #KERN_PROC_RLIMIT} returns: the soft and the hard limit, each a
     * 64-bit {@code rlim_t}.
     */
    protected static final int RLIMIT_SIZE = 16;

    /**
     * Constructs a new {@code FreeBsdOSProcess}.
     *
     * @param pid the process ID
     */
    protected FreeBsdOSProcess(int pid) {
        super(pid);
    }

    @Override
    protected long otherProcessOpenFileLimit(int index) {
        // FreeBSD's procfs, which is not mounted by default, has no limits file; the kernel reports any visible
        // process's limit through the same sysctl procstat -l reads
        return queryOtherRlimitNofile(index == 1);
    }

    /**
     * Reads another process's open file limit through the {@code kern.proc.rlimit} sysctl, with the MIB
     * {@code {CTL_KERN, KERN_PROC, KERN_PROC_RLIMIT, pid, RLIMIT_NOFILE}}. The kernel allows it for any process the
     * caller can see.
     *
     * @param soft {@code true} for the soft limit, {@code false} for the hard limit
     * @return the limit, or {@code -1} if the process has exited or cannot be seen
     */
    protected abstract long queryOtherRlimitNofile(boolean soft);

    @Override
    protected List<BsdPsKeyword> psKeywords() {
        return PS_KEYWORDS;
    }

    @Override
    protected String psCommandArgs() {
        return PS_COMMAND_ARGS;
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return ProcstatUtil.getCwd(getProcessID());
    }

    @Override
    public long getOpenFiles() {
        return ProcstatUtil.getOpenFiles(getProcessID());
    }

    @Override
    public List<OSThread> getThreadDetails() {
        String psCommand = "ps -awwxo " + FreeBsdOSThread.PS_THREAD_COLUMNS + " -H";
        if (getProcessID() >= 0) {
            psCommand += " -p " + getProcessID();
        }
        Predicate<Map<BsdPsThreadKeyword, String>> hasColumnsPri = threadMap -> threadMap
                .containsKey(BsdPsThreadKeyword.PRI);
        return ExecutingCommand.runNative(psCommand).stream().skip(1).parallel()
                .map(thread -> ParseUtil.stringToEnumMap(BsdPsThreadKeyword.class, FreeBsdOSThread.PS_THREAD_KEYWORDS,
                        thread.trim(), ' '))
                .filter(hasColumnsPri).map(threadMap -> new FreeBsdOSThread(getProcessID(), threadMap))
                .filter(VALID_THREAD).collect(Collectors.toList());
    }
}
