/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSThread.ThreadFiltering.VALID_THREAD;
import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.util.ExceptionUtil;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.GlobalConfig;
import oshi.util.ParseUtil;
import oshi.util.PrivilegedUtil;
import oshi.util.UserGroupInfo;
import oshi.util.Util;
import oshi.util.driver.linux.proc.ProcessStat;
import oshi.util.linux.ProcPath;

/**
 * OSProcess implementation
 */
@ThreadSafe
public abstract class LinuxOSProcess extends AbstractOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxOSProcess.class);

    private static final boolean LOG_PROCFS_WARNING = GlobalConfig.get(GlobalConfig.OSHI_OS_LINUX_PROCFS_LOGWARNING,
            false);

    // Get a list of orders to pass to ParseUtil
    private static final int[] PROC_PID_STAT_ORDERS = new int[ProcPidStat.values().length];

    static {
        for (ProcPidStat stat : ProcPidStat.values()) {
            // The PROC_PID_STAT enum indices are 1-indexed.
            // Subtract one to get a zero-based index
            PROC_PID_STAT_ORDERS[stat.ordinal()] = stat.getOrder() - 1;
        }
    }

    private final LinuxOperatingSystem os;

    /**
     * Returns the {@link LinuxOperatingSystem} instance associated with this process.
     *
     * @return the operating system instance
     */
    protected LinuxOperatingSystem getOs() {
        return this.os;
    }

    private final Supplier<Integer> bitness = memoize(this::queryBitness);
    private final Supplier<String> commandLine = memoize(this::queryCommandLine);
    private final Supplier<List<String>> arguments = memoize(this::queryArguments);
    private final Supplier<Map<String, String>> environmentVariables = memoize(this::queryEnvironmentVariables);
    private final Supplier<String> user = memoize(this::queryUser);
    private final Supplier<String> group = memoize(this::queryGroup);

    private volatile String userID;
    private volatile String groupID;
    private volatile long residentSetSize;
    private volatile long privateResidentMemory;
    private volatile long minorFaults;
    private volatile long majorFaults;
    private volatile long voluntaryContextSwitches;
    private volatile long involuntaryContextSwitches;

    // Context-switch counts from getrusage for the current process, which are more accurate than the /proc values.
    // Populated by updateAttributes() via the queryContextSwitches() hook; the native-free build leaves these unset.
    private volatile boolean rusagePopulated;
    private volatile long cachedVoluntaryContextSwitches;
    private volatile long cachedInvoluntaryContextSwitches;

    /**
     * Creates a LinuxOSProcess.
     *
     * @param pid the process ID
     * @param os  the operating system
     */
    protected LinuxOSProcess(int pid, LinuxOperatingSystem os) {
        super(pid);
        this.os = os;
        updateAttributes();
    }

    @Override
    public String getCommandLine() {
        return commandLine.get();
    }

    private String queryCommandLine() {
        return Arrays.stream(FileUtil
                .getStringFromFile(String.format(Locale.ROOT, ProcPath.PID_CMDLINE, getProcessID())).split("\0"))
                .collect(Collectors.joining(" "));
    }

    @Override
    public List<String> getArguments() {
        return arguments.get();
    }

    private List<String> queryArguments() {
        return Collections.unmodifiableList(ParseUtil.parseByteArrayToStrings(FileUtil
                .readAllBytes(String.format(Locale.ROOT, ProcPath.PID_CMDLINE, getProcessID()), LOG_PROCFS_WARNING)));
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables.get();
    }

    private Map<String, String> queryEnvironmentVariables() {
        return Collections.unmodifiableMap(ParseUtil.parseByteArrayToStringMap(PrivilegedUtil.readAllBytesPrivileged(
                String.format(Locale.ROOT, ProcPath.PID_ENVIRON, getProcessID()), LOG_PROCFS_WARNING)));
    }

    @Override
    public String getCurrentWorkingDirectory() {
        try {
            String cwdLink = String.format(Locale.ROOT, ProcPath.PID_CWD, getProcessID());
            String cwd = new File(cwdLink).getCanonicalPath();
            if (!cwd.equals(cwdLink)) {
                return cwd;
            }
        } catch (IOException e) {
            LOG.trace("Couldn't find cwd for pid {}: {}", getProcessID(), e.getMessage());
        }
        return "";
    }

    @Override
    public String getUser() {
        return user.get();
    }

    private String queryUser() {
        return UserGroupInfo.getUser(userID);
    }

    @Override
    public String getUserID() {
        return this.userID;
    }

    @Override
    public String getGroup() {
        return group.get();
    }

    private String queryGroup() {
        return UserGroupInfo.getGroupName(groupID);
    }

    @Override
    public String getGroupID() {
        return this.groupID;
    }

    @Override
    public long getResidentMemory() {
        return this.residentSetSize;
    }

    @Override
    public long getPrivateResidentMemory() {
        return this.privateResidentMemory;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        return ProcessStat.getThreadIds(getProcessID()).stream().parallel()
                .map(id -> new LinuxOSThread(getProcessID(), id, getOs())).filter(VALID_THREAD)
                .collect(Collectors.toList());
    }

    @Override
    public long getMinorFaults() {
        return this.minorFaults;
    }

    @Override
    public long getMajorFaults() {
        return this.majorFaults;
    }

    @Override
    public long getVoluntaryContextSwitches() {
        return this.rusagePopulated ? this.cachedVoluntaryContextSwitches : this.voluntaryContextSwitches;
    }

    @Override
    public long getInvoluntaryContextSwitches() {
        return this.rusagePopulated ? this.cachedInvoluntaryContextSwitches : this.involuntaryContextSwitches;
    }

    @Override
    public long getOpenFiles() {
        return ProcessStat.getFileDescriptorFiles(getProcessID()).length;
    }

    @Override
    public long getSoftOpenFileLimit() {
        return getProcessID() == this.os.getProcessId() ? queryRlimitSoft()
                : getProcessOpenFileLimit(getProcessID(), 1);
    }

    @Override
    public long getHardOpenFileLimit() {
        return getProcessID() == this.os.getProcessId() ? queryRlimitHard()
                : getProcessOpenFileLimit(getProcessID(), 2);
    }

    /**
     * Queries the soft open file limit for the current process via native {@code getrlimit}.
     *
     * @return the soft limit value
     */
    protected abstract long queryRlimitSoft();

    /**
     * Queries the hard open file limit for the current process via native {@code getrlimit}.
     *
     * @return the hard limit value
     */
    protected abstract long queryRlimitHard();

    @Override
    public int getBitness() {
        return this.bitness.get();
    }

    private int queryBitness() {
        // get 5th byte of file for 64-bit check
        // https://en.wikipedia.org/wiki/Executable_and_Linkable_Format#File_header
        byte[] buffer = new byte[5];
        if (!path.isEmpty()) {
            try (InputStream is = new FileInputStream(path)) {
                if (is.read(buffer) == buffer.length) {
                    return buffer[4] == 1 ? 32 : 64;
                }
            } catch (IOException e) {
                LOG.warn("Failed to read process file: {}", path);
            }
        }
        return 0;
    }

    @Override
    public long getAffinityMask() {
        // Would prefer to use native sched_getaffinity call but variable sizing is
        // kernel-dependent and requires C macros, so we use command line instead.
        return parseAffinityMask(ExecutingCommand.getFirstAnswer("taskset -p " + getProcessID()));
    }

    /**
     * Parse the affinity mask from taskset output.
     *
     * @param tasksetOutput output of {@code taskset -p <pid>}
     * @return the affinity mask as a long, or 0 if unparseable
     */
    static long parseAffinityMask(String tasksetOutput) {
        // Output:
        // pid 3283's current affinity mask: 3
        // pid 9726's current affinity mask: f
        String[] split = ParseUtil.whitespaces.split(tasksetOutput);
        if (split.length == 0) {
            return 0;
        }
        return ExceptionUtil.getLongOrDefault(() -> new BigInteger(split[split.length - 1], 16).longValue(), 0);
    }

    @Override
    public boolean updateAttributes() {
        boolean result = updateAttributesFromProc();
        // getrusage reports more accurate context-switch counts than /proc, but only for the current process and only
        // when its /proc attributes were read successfully
        if (result && getProcessID() == getOs().getProcessId()) {
            long[] contextSwitches = queryContextSwitches();
            if (contextSwitches != null) {
                this.cachedVoluntaryContextSwitches = contextSwitches[0];
                this.cachedInvoluntaryContextSwitches = contextSwitches[1];
                this.rusagePopulated = true;
            } else {
                this.rusagePopulated = false;
            }
        }
        return result;
    }

    /**
     * Reads this (current) process's voluntary and involuntary context-switch counts via native {@code getrusage}. The
     * default returns {@code null} (the native-free build has no {@code getrusage}); the JNA and FFM subclasses
     * override it.
     *
     * @return a two-element array {@code {voluntary, involuntary}}, or {@code null} if unavailable
     */
    protected long[] queryContextSwitches() {
        return null;
    }

    private boolean updateAttributesFromProc() {
        String procPidExe = String.format(Locale.ROOT, ProcPath.PID_EXE, getProcessID());
        try {
            Path link = Paths.get(procPidExe);
            this.path = Files.readSymbolicLink(link).toString();
            // For some services the symbolic link process has terminated
            int index = path.indexOf(" (deleted)");
            if (index != -1) {
                path = path.substring(0, index);
            }
        } catch (InvalidPathException | IOException | UnsupportedOperationException | SecurityException e) {
            LOG.debug("Unable to open symbolic link {}", procPidExe);
        }
        // Fetch all the values here
        // check for terminated process race condition after last one.
        Map<String, String> io = PrivilegedUtil
                .getKeyValueMapFromFilePrivileged(String.format(Locale.ROOT, ProcPath.PID_IO, getProcessID()), ":");
        Map<String, String> status = FileUtil
                .getKeyValueMapFromFile(String.format(Locale.ROOT, ProcPath.PID_STATUS, getProcessID()), ":");
        String stat = FileUtil.getStringFromFile(String.format(Locale.ROOT, ProcPath.PID_STAT, getProcessID()));
        String statm = FileUtil.getStringFromFile(String.format(Locale.ROOT, ProcPath.PID_STATM, getProcessID()));
        if (stat.isEmpty()) {
            this.state = INVALID;
            return false;
        }
        // If some details couldn't be read from ProcPath.PID_STATUS try reading it from
        // ProcPath.PID_STAT
        getMissingDetails(status, stat);

        long now = System.currentTimeMillis();

        // We can get name and status more easily from /proc/pid/status which we
        // call later, so just get the numeric bits here
        // See man proc for how to parse /proc/[pid]/stat
        long[] statArray = ParseUtil.parseStringToLongArray(stat, PROC_PID_STAT_ORDERS,
                ProcessStat.PROC_PID_STAT_LENGTH, ' ');

        // BOOTTIME is in seconds and start time from proc/pid/stat is in jiffies.
        // Combine units to jiffies and convert to millijiffies before hz division to
        // avoid precision loss without having to cast
        this.startTime = (LinuxOperatingSystem.getBootTime() * getOs().getHz()
                + statArray[ProcPidStat.START_TIME.ordinal()]) * 1000L / getOs().getHz();
        // BOOT_TIME could be up to 500ms off and start time up to 5ms off. A process
        // that has started within last 505ms could produce a future start time/negative
        // up time, so insert a sanity check.
        if (startTime >= now) {
            startTime = now - 1;
        }
        this.parentProcessID = (int) statArray[ProcPidStat.PPID.ordinal()];
        this.threadCount = (int) statArray[ProcPidStat.THREAD_COUNT.ordinal()];
        this.priority = (int) statArray[ProcPidStat.PRIORITY.ordinal()];
        this.virtualSize = statArray[ProcPidStat.VSZ.ordinal()];
        // Parse /proc/[pid]/statm for resident and shared pages (all in pages)
        // Fields: size resident shared text lib data dt
        String[] statmFields = ParseUtil.whitespaces.split(statm);
        if (statmFields.length > 2) {
            long resident = ParseUtil.parseLongOrDefault(statmFields[1], 0L);
            long shared = ParseUtil.parseLongOrDefault(statmFields[2], 0L);
            long pageSize = getOs().getPageSize();
            this.residentSetSize = resident * pageSize;
            this.privateResidentMemory = (resident - shared) * pageSize;
        } else {
            this.residentSetSize = statArray[ProcPidStat.RSS.ordinal()] * getOs().getPageSize();
            this.privateResidentMemory = this.residentSetSize;
        }
        this.kernelTime = statArray[ProcPidStat.KERNEL_TIME.ordinal()] * 1000L / getOs().getHz();
        this.userTime = statArray[ProcPidStat.USER_TIME.ordinal()] * 1000L / getOs().getHz();
        this.minorFaults = statArray[ProcPidStat.MINOR_FAULTS.ordinal()];
        this.majorFaults = statArray[ProcPidStat.MAJOR_FAULTS.ordinal()];
        this.voluntaryContextSwitches = ParseUtil.parseLongOrDefault(status.get("voluntary_ctxt_switches"), 0L);
        this.involuntaryContextSwitches = ParseUtil.parseLongOrDefault(status.get("nonvoluntary_ctxt_switches"), 0L);

        this.upTime = now - startTime;

        // See man proc for how to parse /proc/[pid]/io
        this.bytesRead = ParseUtil.parseLongOrDefault(io.getOrDefault("read_bytes", ""), 0L);
        this.bytesWritten = ParseUtil.parseLongOrDefault(io.getOrDefault("write_bytes", ""), 0L);

        // Don't set open files or bitness or currentWorkingDirectory; fetch on demand.

        this.userID = ParseUtil.whitespaces.split(status.getOrDefault("Uid", ""))[0];
        // defer user lookup until asked
        this.groupID = ParseUtil.whitespaces.split(status.getOrDefault("Gid", ""))[0];
        // defer group lookup until asked
        this.name = status.getOrDefault("Name", "");
        this.state = ProcessStat.getState(status.getOrDefault("State", "U").charAt(0));
        return true;
    }

    /**
     * If some details couldn't be read from ProcPath.PID_STATUS try reading it from ProcPath.PID_STAT
     *
     * @param status status map to fill.
     * @param stat   string to read from.
     */
    static void getMissingDetails(Map<String, String> status, String stat) {
        if (status == null || stat == null) {
            return;
        }

        int nameStart = stat.indexOf('(');
        int nameEnd = stat.lastIndexOf(')');
        if (Util.isBlank(status.get("Name")) && nameStart > 0 && nameStart < nameEnd) {
            // remove leading and trailing parentheses
            String statName = stat.substring(nameStart + 1, nameEnd);
            status.put("Name", statName);
        }

        // As per man, the next item after the name is the state
        if (Util.isBlank(status.get("State")) && nameEnd > 0 && stat.length() > nameEnd + 2) {
            String statState = String.valueOf(stat.charAt(nameEnd + 2));
            status.put("State", statState);
        }
    }

    /**
     * Enum used to update attributes. The order field represents the 1-indexed numeric order of the stat in
     * /proc/pid/stat per the man file.
     */
    private enum ProcPidStat {
        // The parsing implementation in ParseUtil requires these to be declared
        // in increasing order
        /** PPID property. */
        PPID(4),
        /** MINOR_FAULTS property. */
        MINOR_FAULTS(10),
        /** MAJOR_FAULTS property. */
        MAJOR_FAULTS(12),
        /** USER_TIME property. */
        USER_TIME(14),
        /** KERNEL_TIME property. */
        KERNEL_TIME(15),
        /** PRIORITY property. */
        PRIORITY(18),
        /** THREAD_COUNT property. */
        THREAD_COUNT(20),
        /** START_TIME property. */
        START_TIME(22),
        /** VSZ property. */
        VSZ(23),
        /** RSS property. */
        RSS(24);

        private final int order;

        public int getOrder() {
            return this.order;
        }

        ProcPidStat(int order) {
            this.order = order;
        }
    }

    /**
     * Gets the open file limit for a process.
     *
     * @param processId the process ID
     * @param index     the limit index (soft=0, hard=1)
     * @return the file limit
     */
    protected long getProcessOpenFileLimit(long processId, int index) {
        final String limitsPath = String.format(Locale.ROOT, "/proc/%d/limits", processId);
        if (!Files.exists(Paths.get(limitsPath))) {
            return -1; // not supported
        }
        final List<String> lines = FileUtil.readFile(limitsPath);
        final Optional<String> maxOpenFilesLine = lines.stream().filter(line -> line.startsWith("Max open files"))
                .findFirst();
        if (!maxOpenFilesLine.isPresent()) {
            return -1;
        }
        final String line = maxOpenFilesLine.get();
        if (line.contains("unlimited")) {
            return -1;
        }
        // Split all non-Digits away -> ["", "{soft-limit}", "{hard-limit}"]
        final String[] split = line.split("\\D+");
        if (split.length <= index) {
            return -1;
        }
        return ParseUtil.parseLongOrDefault(split[index], -1);
    }
}
