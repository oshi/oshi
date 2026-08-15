/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.bsd;

import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.WAITING;
import static oshi.software.os.OSProcess.State.ZOMBIE;
import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSProcess;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.driver.unix.ProcLimits;

/**
 * Abstract base shared by the BSD-family OSProcess implementations (FreeBSD, OpenBSD, DragonFly, NetBSD). Holds the
 * field storage, the trivial accessors, command-line/argument/environment/bitness memoization, the {@code ps} state
 * mapping, the {@code cpuset} affinity lookup, and the {@code /proc/<pid>/limits} fallback. Platform-specific work (the
 * {@code ps} attribute query, thread enumeration, cwd/open-file lookups, and the native argument/environment and
 * file-limit reads) is provided by the per-platform subclasses.
 */
@ThreadSafe
public abstract class BsdOSProcess extends AbstractOSProcess {

    private final Supplier<Integer> bitness = memoize(this::queryBitness);
    private final Supplier<String> commandLine = memoize(this::queryCommandLine);
    private final Supplier<List<String>> arguments = memoize(this::queryArguments);
    private final Supplier<Map<String, String>> environmentVariables = memoize(this::queryEnvironmentVariables);

    protected volatile String user = "";
    protected volatile String userID = "";
    protected volatile String group = "";
    protected volatile String groupID = "";
    protected volatile long residentSetSize;
    protected volatile long minorFaults;
    protected volatile long majorFaults;
    protected volatile long voluntaryContextSwitches;
    protected volatile long involuntaryContextSwitches;
    protected volatile String commandLineBackup = "";

    /**
     * Constructs a new {@code BsdOSProcess}.
     *
     * @param pid the process ID
     */
    protected BsdOSProcess(int pid) {
        super(pid);
    }

    @Override
    public String getCommandLine() {
        return this.commandLine.get();
    }

    private String queryCommandLine() {
        String cl = String.join(" ", getArguments());
        return cl.isEmpty() ? this.commandLineBackup : cl;
    }

    @Override
    public List<String> getArguments() {
        return arguments.get();
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables.get();
    }

    @Override
    public String getUser() {
        return this.user;
    }

    @Override
    public String getUserID() {
        return this.userID;
    }

    @Override
    public String getGroup() {
        return this.group;
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
    public long getMinorFaults() {
        return this.minorFaults;
    }

    @Override
    public long getMajorFaults() {
        return this.majorFaults;
    }

    @Override
    public long getVoluntaryContextSwitches() {
        return this.voluntaryContextSwitches;
    }

    @Override
    public long getInvoluntaryContextSwitches() {
        return this.involuntaryContextSwitches;
    }

    @Override
    public int getBitness() {
        return this.bitness.get();
    }

    @Override
    public long getAffinityMask() {
        long bitMask = 0L;
        // Would prefer to use native cpuset_getaffinity call but variable sizing is
        // kernel-dependent and requires C macros, so we use commandline instead.
        String cpuset = ExecutingCommand.getFirstAnswer("cpuset -gp " + getProcessID());
        // Sample output:
        // pid 8 mask: 0, 1
        // cpuset: getaffinity: No such process
        String[] split = cpuset.split(":", -1);
        if (split.length > 1) {
            String[] bits = split[1].split(",", -1);
            for (String bit : bits) {
                int bitToSet = ParseUtil.parseIntOrDefault(bit.trim(), -1);
                if (bitToSet >= 0 && bitToSet < Long.SIZE) {
                    bitMask |= 1L << bitToSet;
                }
            }
        }
        return bitMask;
    }

    @Override
    public boolean updateAttributes() {
        List<BsdPsKeyword> cols = psKeywords();
        String psCommand = "ps -awwxo " + psCommandArgs() + " -p " + getProcessID();
        List<String> procList = ExecutingCommand.runNative(psCommand);
        if (procList.size() > 1) {
            // Skip the header row and parse the first data row against this platform's columns
            Map<BsdPsKeyword, String> psMap = ParseUtil.stringToEnumMap(BsdPsKeyword.class, cols,
                    procList.get(1).trim(), ' ');
            // Check if the last (thus all) value populated
            if (psMap.containsKey(cols.get(cols.size() - 1))) {
                return updateAttributes(psMap);
            }
        }
        this.state = State.INVALID;
        return false;
    }

    /**
     * Populates this process's attributes from a parsed {@code ps} row. Shared by every BSD platform; differences in
     * the available columns are handled by checking which keys are present in the map.
     * <p>
     * Synchronized because it is the only writer of this object's fields, reached both from {@link #updateAttributes()}
     * and from each platform's constructor. The lock makes the whole field set update atomically, so two concurrent
     * refreshes cannot interleave values from two different {@code ps} samples, and it makes the read-modify-write in
     * {@link #monotonic(long, long)} safe: without it, two racing clamps can both read the same previous value and the
     * lower one can land last, reintroducing the decrease the clamp exists to prevent.
     *
     * @param psMap the parsed {@code ps} columns for this process
     * @return {@code true} once the attributes are populated
     */
    protected synchronized boolean updateAttributes(Map<BsdPsKeyword, String> psMap) {
        long now = System.currentTimeMillis();
        String stateValue = ParseUtil.getStringValueOrEmpty(psMap.get(BsdPsKeyword.STATE));
        // An absent state column falls through to OTHER rather than failing the whole update
        this.state = getStateFromOutput(stateValue.isEmpty() ? ' ' : stateValue.charAt(0));
        this.parentProcessID = ParseUtil.parseIntOrDefault(psMap.get(BsdPsKeyword.PPID), 0);
        this.user = ParseUtil.getStringValueOrEmpty(psMap.get(BsdPsKeyword.USER));
        this.userID = ParseUtil.getStringValueOrEmpty(psMap.get(BsdPsKeyword.UID));
        // Most platforms report a group name and gid. DragonFly's ps has no group column, so fall
        // back to the user name and the real group id.
        if (psMap.containsKey(BsdPsKeyword.GROUP)) {
            this.group = ParseUtil.getStringValueOrEmpty(psMap.get(BsdPsKeyword.GROUP));
            this.groupID = ParseUtil.getStringValueOrEmpty(psMap.get(BsdPsKeyword.GID));
        } else {
            this.group = this.user;
            this.groupID = ParseUtil.getStringValueOrEmpty(psMap.get(BsdPsKeyword.RGID));
        }
        this.priority = ParseUtil.parseIntOrDefault(psMap.get(BsdPsKeyword.PRI), 0);
        // These are in KB, multiply
        this.virtualSize = ParseUtil.parseLongOrDefault(psMap.get(BsdPsKeyword.VSZ), 0) * 1024;
        this.residentSetSize = ParseUtil.parseLongOrDefault(psMap.get(BsdPsKeyword.RSS), 0) * 1024;
        // Thread count comes from the nlwp column where present; platforms that omit it (OpenBSD,
        // NetBSD) populate it via a separate ps query.
        if (psMap.containsKey(BsdPsKeyword.NLWP)) {
            this.threadCount = ParseUtil.parseIntOrDefault(psMap.get(BsdPsKeyword.NLWP), 0);
        } else {
            updateThreadCount();
        }
        // Kernel/user time: FreeBSD reports systime separately (time is user+sys); the others fold
        // kernel time into a single cputime/time column. Clamp both against a decrease; see
        // monotonic(long, long).
        if (psMap.containsKey(BsdPsKeyword.SYSTIME)) {
            long sysTime = ParseUtil.parseDHMSOrDefault(psMap.get(BsdPsKeyword.SYSTIME), 0L);
            this.kernelTime = monotonic(this.kernelTime, sysTime);
            this.userTime = monotonic(this.userTime,
                    ParseUtil.parseDHMSOrDefault(psMap.get(BsdPsKeyword.TIME), 0L) - sysTime);
        } else if (psMap.containsKey(BsdPsKeyword.CPUTIME)) {
            this.kernelTime = 0L;
            this.userTime = monotonic(this.userTime, ParseUtil.parseDHMSOrDefault(psMap.get(BsdPsKeyword.CPUTIME), 0L));
        } else {
            this.kernelTime = 0L;
            this.userTime = monotonic(this.userTime, ParseUtil.parseDHMSOrDefault(psMap.get(BsdPsKeyword.TIME), 0L));
        }
        // Start/up time: DragonFly resolves an absolute start time from /proc; the others derive it
        // from the ps elapsed-time column.
        long startMillis = queryStartTimeMillis();
        if (startMillis > 0L) {
            this.startTime = startMillis;
            this.upTime = Math.max(1L, now - startMillis);
        } else {
            BsdPsKeyword elapsedKey = psMap.containsKey(BsdPsKeyword.ETIMES) ? BsdPsKeyword.ETIMES : BsdPsKeyword.ETIME;
            long elapsedTime = ParseUtil.parseDHMSOrDefault(psMap.get(elapsedKey), 0L);
            this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
            this.startTime = now - this.upTime;
        }
        // Path/name come from comm (ucomm on DragonFly)
        this.path = ParseUtil.getStringValueOrEmpty(
                psMap.get(psMap.containsKey(BsdPsKeyword.UCOMM) ? BsdPsKeyword.UCOMM : BsdPsKeyword.COMM));
        this.name = this.path.substring(this.path.lastIndexOf('/') + 1);
        this.minorFaults = ParseUtil.parseLongOrDefault(psMap.get(BsdPsKeyword.MINFLT), 0L);
        this.majorFaults = ParseUtil.parseLongOrDefault(psMap.get(BsdPsKeyword.MAJFLT), 0L);
        this.voluntaryContextSwitches = ParseUtil.parseLongOrDefault(psMap.get(BsdPsKeyword.NVCSW), 0L);
        this.involuntaryContextSwitches = ParseUtil.parseLongOrDefault(psMap.get(BsdPsKeyword.NIVCSW), 0L);
        // Command-line fallback: the full args column (named "command" on DragonFly)
        this.commandLineBackup = ParseUtil.getStringValueOrEmpty(
                psMap.get(psMap.containsKey(BsdPsKeyword.COMMAND) ? BsdPsKeyword.COMMAND : BsdPsKeyword.ARGS));
        return true;
    }

    /**
     * Clamps a freshly-parsed CPU-time counter so it never falls below the value already reported.
     * <p>
     * The BSD {@code ps} time columns are not reliably monotonic. On DragonFly the {@code TIME} column sums only the
     * process's currently live LWPs, so an exiting thread takes its accumulated CPU time out of the total; where user
     * time is derived by subtracting {@code systime} from {@code TIME}, the two columns are sampled independently and
     * the difference can dip on its own. FreeBSD's kernel applies the same guard internally in {@code calcru1()} before
     * handing these numbers to userland, but DragonFly has no equivalent, so OSHI enforces it here.
     *
     * @param previous the value last reported, or 0 on a first population
     * @param parsed   the value just parsed from {@code ps}
     * @return {@code parsed}, or {@code previous} if that would be a decrease
     */
    protected static long monotonic(long previous, long parsed) {
        return Math.max(previous, parsed);
    }

    /**
     * Maps a {@code ps} single-character state to an {@link State}.
     *
     * @param stateValue the first character of the {@code ps} STATE column
     * @return the corresponding process state
     */
    protected static State getStateFromOutput(char stateValue) {
        switch (stateValue) {
            case 'R':
                return RUNNING;
            case 'I':
            case 'S':
                return SLEEPING;
            case 'D':
            case 'L':
            case 'U':
                return WAITING;
            case 'Z':
                return ZOMBIE;
            case 'T':
                return STOPPED;
            default:
                return OTHER;
        }
    }

    @Override
    public long getSoftOpenFileLimit() {
        if (getProcessID() == queryOwnProcessId()) {
            return queryRlimitNofile(true);
        }
        return otherProcessOpenFileLimit(1);
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == queryOwnProcessId()) {
            return queryRlimitNofile(false);
        }
        return otherProcessOpenFileLimit(2);
    }

    /**
     * Returns the open-file limit for another (non-current) process. The default reads {@code /proc/<pid>/limits} where
     * a procfs is available; platforms without one (OpenBSD) override this to return {@code -1}.
     *
     * @param index {@code 1} for the soft limit, {@code 2} for the hard limit
     * @return the limit, or {@code -1} if unavailable
     */
    protected long otherProcessOpenFileLimit(int index) {
        return getProcessOpenFileLimit(getProcessID(), index);
    }

    /**
     * Returns the process ID of the current (JVM) process. {@code getrlimit} only reports the calling process, so the
     * open-file-limit methods use this to decide whether they can query it natively. The default returns {@code -1} (so
     * the process is never treated as the current one); the native subclasses override it with the operating system's
     * own process ID.
     *
     * @return the current process ID, or {@code -1} if unknown
     */
    protected int queryOwnProcessId() {
        return -1;
    }

    /**
     * Reads the current process's soft or hard open-file limit via {@code getrlimit(RLIMIT_NOFILE)}. Only called when
     * this process is the current one. The default returns {@code -1}; the native subclasses override it.
     *
     * @param soft {@code true} for the soft limit, {@code false} for the hard limit
     * @return the limit, or {@code -1} if unavailable
     */
    protected long queryRlimitNofile(boolean soft) {
        return -1L;
    }

    /**
     * Derives process bitness from a {@code kern.proc.sv_name} system-call ABI string.
     *
     * @param svName the ABI name (e.g. {@code "FreeBSD ELF64"})
     * @return {@code 32} or {@code 64}, or {@code 0} if not recognized
     */
    protected static int elfBitness(String svName) {
        if (svName.contains("ELF32")) {
            return 32;
        } else if (svName.contains("ELF64")) {
            return 64;
        }
        return 0;
    }

    /**
     * Parses the soft or hard open-file limit for another process from {@code /proc/<pid>/limits}.
     *
     * @param processId the process ID
     * @param index     {@code 1} for the soft limit, {@code 2} for the hard limit
     * @return the limit, or {@code -1} if unavailable
     */
    protected static long getProcessOpenFileLimit(long processId, int index) {
        return ProcLimits.queryOpenFileLimit(processId, index);
    }

    /**
     * Returns this platform's ordered {@code ps} column keys, used both to build the {@code ps} command and to parse
     * its output positionally.
     *
     * @return the ordered column keys
     */
    protected abstract List<BsdPsKeyword> psKeywords();

    /**
     * Returns the comma-separated {@code ps -o} column list for this platform, derived from {@link #psKeywords()}.
     *
     * @return the {@code ps} column argument
     */
    protected abstract String psCommandArgs();

    /**
     * Returns this process's start time in epoch milliseconds, or {@code -1} to derive it from the {@code ps}
     * elapsed-time column. The default derives from elapsed time; platforms with an absolute source (DragonFly's
     * {@code /proc}) override this.
     *
     * @return the start time in epoch milliseconds, or {@code -1} to derive from elapsed time
     */
    protected long queryStartTimeMillis() {
        return -1L;
    }

    /**
     * Populates {@link #threadCount} for platforms whose {@code ps} output lacks an {@code nlwp} column. The default is
     * a no-op (the count is taken from the {@code nlwp} column); OpenBSD and NetBSD override this with a separate
     * {@code ps} query.
     */
    protected void updateThreadCount() {
        // Default no-op; overridden where ps lacks an nlwp column
    }

    /**
     * Queries this process's argument list.
     *
     * @return the arguments, or an empty list if unavailable
     */
    protected abstract List<String> queryArguments();

    /**
     * Queries this process's environment variables.
     *
     * @return the environment map, or an empty map if unavailable
     */
    protected abstract Map<String, String> queryEnvironmentVariables();

    /**
     * Queries this process's bitness (32 or 64), or {@code 0} if unknown.
     *
     * @return the bitness
     */
    protected abstract int queryBitness();
}
