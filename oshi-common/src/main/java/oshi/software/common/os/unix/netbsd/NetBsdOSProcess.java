/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.netbsd;

import static oshi.software.common.os.unix.bsd.BsdPsKeyword.ARGS;
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

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.unix.bsd.BsdOSProcess;
import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.common.os.unix.bsd.BsdPsThreadKeyword;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.common.platform.unix.bsd.BsdSysctlUtil;
import oshi.util.common.platform.unix.netbsd.FstatUtil;

/**
 * OSProcess implementation
 */
@ThreadSafe
public class NetBsdOSProcess extends BsdOSProcess {
    private static final Pattern AFFINITY = Pattern.compile("Affinity:");
    private static final Pattern COMMA_OR_WHITESPACE = Pattern.compile("[,\\s]+");
    // The kernel withholds parts of a process while it is inside posix_spawn; see queryArguments and isTornRow
    private static final int SPAWN_RETRY_ATTEMPTS_SELF = 15;
    private static final int SPAWN_RETRY_ATTEMPTS_OTHER = 2;
    private static final long SPAWN_RETRY_MILLIS = 20L;

    /**
     * Ordered {@code ps} columns queried for each process. Shared by NetBsdOSProcess and the NetBSD OperatingSystem so
     * the column list and parsing stay in lockstep. {@code ARGS} must remain last.
     */
    public static final List<BsdPsKeyword> PS_KEYWORDS = Collections.unmodifiableList(Arrays.asList(STATE, PID, PPID,
            USER, UID, GROUP, GID, PRI, VSZ, RSS, ETIME, CPUTIME, MAJFLT, MINFLT, NVCSW, NIVCSW, ARGS));

    public static final String PS_COMMAND_ARGS = PS_KEYWORDS.stream().map(Enum::name)
            .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.joining(","));

    /**
     * The {@code KERN_PROC_ARGS} sysctl subtype that returns a process's executable path.
     */
    protected static final int KERN_PROC_PATHNAME = 5;

    /**
     * The {@code KERN_PROC_ARGS} sysctl subtype that returns a process's current working directory.
     */
    protected static final int KERN_PROC_CWD = 6;

    /**
     * The size of the buffer for a {@code kern.proc_args} path: twice the platform's {@code PATH_MAX} of 1024, the
     * longest working directory the kernel returns. An executable path is never longer than {@code PATH_MAX}.
     */
    protected static final int PROC_ARGS_PATH_SIZE = 2048;

    protected final NetBsdOperatingSystem os;

    /**
     * Constructs a new {@code NetBsdOSProcess}.
     *
     * @param pid   the process ID
     * @param psMap the parsed {@code ps} columns for this process
     * @param os    the owning operating system
     */
    public NetBsdOSProcess(int pid, Map<BsdPsKeyword, String> psMap, NetBsdOperatingSystem os) {
        super(pid);
        this.os = os;
        updateAttributes(psMap);
    }

    @Override
    protected synchronized boolean updateAttributes(Map<BsdPsKeyword, String> psMap) {
        Map<BsdPsKeyword, String> row = psMap;
        int attempts = getProcessID() == this.os.getProcessId() ? SPAWN_RETRY_ATTEMPTS_SELF
                : SPAWN_RETRY_ATTEMPTS_OTHER;
        for (int attempt = 1; attempt < attempts && isTornRow(row); attempt++) {
            Util.sleep(SPAWN_RETRY_MILLIS);
            Map<BsdPsKeyword, String> retry = queryPsRow();
            if (retry.isEmpty()) {
                break;
            }
            row = retry;
        }
        return super.updateAttributes(row);
    }

    /**
     * Whether a {@code ps} row is missing the counters the kernel withholds while the process is inside
     * {@code posix_spawn}. Package-private for testing.
     * <p>
     * {@code fill_kproc2()} fills the CPU time, page faults and context switches only when it can take the process's
     * {@code p_reflock}; {@code sysctl_doeproc} reports the process with all of them zero otherwise. A JVM holds that
     * lock whenever one of its threads runs a command, as OSHI does. A live process that has run at all has nonzero
     * minor faults and context switches, so all four zero marks such a row.
     *
     * @param psMap the parsed {@code ps} columns
     * @return {@code true} if the row's CPU time, minor faults and both context-switch counts are all zero
     */
    static boolean isTornRow(Map<BsdPsKeyword, String> psMap) {
        return !ParseUtil.getStringValueOrEmpty(psMap.get(STATE)).startsWith("Z")
                && ParseUtil.parseDHMSOrDefault(psMap.get(CPUTIME), 0L) == 0L
                && ParseUtil.parseLongOrDefault(psMap.get(MINFLT), 0L) == 0L
                && ParseUtil.parseLongOrDefault(psMap.get(NVCSW), 0L) == 0L
                && ParseUtil.parseLongOrDefault(psMap.get(NIVCSW), 0L) == 0L;
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
    protected String queryPath(Map<BsdPsKeyword, String> psMap) {
        String path = queryExecutablePath();
        if (!path.isEmpty()) {
            return path;
        }
        // Without the kernel's answer, the path is argv[0]. NetBSD's ps prints argv[0] in the comm column, the same
        // value the args column begins with, but clips it to the width of its "COMMAND" header - seven characters -
        // because only the last column is left unbounded. So argv[0] is taken from the args column, which is queried
        // last and is therefore printed in full.
        String argv = parseArgv0(psMap.get(BsdPsKeyword.ARGS));
        // The placeholder says the arguments were unavailable, so no path is known
        return isPlaceholder(argv) ? "" : argv;
    }

    @Override
    protected String queryExecutablePath() {
        return queryProcArgsPath(KERN_PROC_PATHNAME);
    }

    /**
     * Queries a path for this process from the kernel, the {@code kern.proc_args} sysctl with the given subtype.
     *
     * @param subtype {@link #KERN_PROC_PATHNAME} for the executable or {@link #KERN_PROC_CWD} for the working directory
     * @return the path, or an empty string if this build cannot make the call or the kernel has none, as it has no
     *         executable for the kernel itself
     */
    protected String queryProcArgsPath(int subtype) {
        return "";
    }

    @Override
    protected String queryCommandLineBackup(Map<BsdPsKeyword, String> psMap) {
        return parseCommandLine(psMap.get(BsdPsKeyword.ARGS));
    }

    /**
     * Extracts the command line from a {@code ps} args column.
     *
     * @param args the args column, the process's arguments separated by spaces
     * @return the arguments, or an empty string where {@code ps} substituted the parenthesized command because the
     *         kernel would not release them, which is a placeholder rather than a command line
     */
    static String parseCommandLine(@Nullable String args) {
        String argv = ParseUtil.getStringValueOrEmpty(args);
        return isPlaceholder(parseArgv0(argv)) ? "" : argv;
    }

    @Override
    protected String queryName(Map<BsdPsKeyword, String> psMap, String path) {
        // From argv[0] rather than the path, so that it matches the name each of the process's threads is given
        return parseName(psMap.get(BsdPsKeyword.ARGS));
    }

    /**
     * Extracts the process name from a {@code ps} args column: the last path component of {@code argv[0]}.
     * <p>
     * The placeholder {@code ps} substitutes when the kernel refuses the arguments wraps the kernel's own name for the
     * process, which is the name whether or not the arguments could be read, so it is unwrapped rather than reporting
     * {@code ps}'s punctuation as the name. Either way two readings of one process give the same name.
     *
     * @param args the args column, the process's arguments separated by spaces
     * @return the process name
     */
    static String parseName(@Nullable String args) {
        String argv = parseArgv0(args);
        return isPlaceholder(argv) ? argv.substring(1, argv.length() - 1) : argv.substring(argv.lastIndexOf('/') + 1);
    }

    /**
     * Extracts {@code argv[0]} from a {@code ps} args column.
     *
     * @param args the args column, the process's arguments separated by spaces
     * @return {@code argv[0]}, which is a path when the process was started by one, or the command in parentheses where
     *         the kernel would not release the arguments
     */
    static String parseArgv0(@Nullable String args) {
        String argv = ParseUtil.getStringValueOrEmpty(args).trim();
        int space = argv.indexOf(' ');
        return space < 0 ? argv : argv.substring(0, space);
    }

    /**
     * Whether a {@code ps} args column holds the parenthesized command that {@code ps} substitutes when the kernel
     * refuses a process's arguments, which it does while that process is inside {@code posix_spawn}.
     *
     * @param argv the value from {@link #parseArgv0(String)}
     * @return {@code true} if it is that placeholder rather than a real {@code argv[0]}
     */
    static boolean isPlaceholder(String argv) {
        return argv.length() > 2 && argv.charAt(0) == '(' && argv.charAt(argv.length() - 1) == ')';
    }

    @Override
    protected List<String> queryArguments() {
        // NetBSD provides command line via /proc filesystem
        String cmdline = "/proc/" + getProcessID() + "/cmdline";
        // The kernel refuses to read a process's arguments while that process is inside posix_spawn, which a JVM is
        // whenever it runs a command, so an empty read of a file that exists is retried. This process is the case OSHI
        // provokes itself, by running ps while another thread spawns, and so is given the longer budget; for any other
        // process an empty read is usually genuine. The result is memoized for the life of this object, so a single
        // refusal would otherwise stick.
        int attempts = getProcessID() == this.os.getProcessId() ? SPAWN_RETRY_ATTEMPTS_SELF
                : SPAWN_RETRY_ATTEMPTS_OTHER;
        for (int attempt = 1;; attempt++) {
            byte[] cmdBytes = FileUtil.readAllBytes(cmdline, false);
            if (cmdBytes.length > 0) {
                return Collections.unmodifiableList(ParseUtil.parseByteArrayToStrings(cmdBytes));
            }
            if (attempt >= attempts || !new File(cmdline).exists()) {
                return Collections.emptyList();
            }
            Util.sleep(SPAWN_RETRY_MILLIS);
        }
    }

    @Override
    protected Map<String, String> queryEnvironmentVariables() {
        // For the current process, use Java's System.getenv()
        if (getProcessID() == this.os.getProcessId()) {
            return System.getenv();
        }
        return parseEnvironment(queryEnvironmentBytes());
    }

    /**
     * Queries the environment of this process from the kernel, the {@code kern.proc_args} sysctl with
     * {@code KERN_PROC_ENV}.
     *
     * @return The NUL-delimited {@code KEY=VALUE} entries, or an empty array if this build cannot make the call; NetBSD
     *         offers no command that reads another process's environment
     */
    protected byte[] queryEnvironmentBytes() {
        return new byte[0];
    }

    /**
     * Parses the environment returned by the {@code kern.proc_args} sysctl.
     *
     * @param envBytes NUL-delimited {@code KEY=VALUE} entries
     * @return the environment, empty if there were no entries
     */
    static Map<String, String> parseEnvironment(byte[] envBytes) {
        // Parse by raw bytes rather than by String offsets, which would misalign on a multibyte charset
        Map<String, String> env = new LinkedHashMap<>();
        for (String envStr : ParseUtil.parseByteArrayToStrings(envBytes)) {
            int idx = envStr.indexOf('=');
            if (idx > 0) {
                env.put(envStr.substring(0, idx), envStr.substring(idx + 1));
            }
        }
        return env.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(env);
    }

    @Override
    public String getCurrentWorkingDirectory() {
        String cwd = queryProcArgsPath(KERN_PROC_CWD);
        return cwd.isEmpty() ? FstatUtil.getCwd(getProcessID()) : cwd;
    }

    @Override
    public long getOpenFiles() {
        return FstatUtil.getOpenFiles(getProcessID());
    }

    @Override
    public long getSoftOpenFileLimit() {
        return queryOwnOpenFileLimit(true);
    }

    @Override
    public long getHardOpenFileLimit() {
        return queryOwnOpenFileLimit(false);
    }

    private long queryOwnOpenFileLimit(boolean soft) {
        if (getProcessID() == this.os.getProcessId()) {
            // getrlimit where a backend can call it; otherwise the sysctl a child process inherits, then the system max
            long limit = queryRlimitNofile(soft);
            if (limit >= 0) {
                return limit;
            }
            limit = BsdSysctlUtil.sysctl("proc.curproc.rlimit.descriptors." + (soft ? "soft" : "hard"), 0L);
            if (limit <= 0) {
                limit = BsdSysctlUtil.sysctl("kern.maxfiles", 0L);
            }
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
                String[] parts = COMMA_OR_WHITESPACE.split(AFFINITY.split(line, -1)[1].trim(), -1);
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
        int ncpu = BsdSysctlUtil.sysctl("hw.ncpuonline", 1);
        return ncpu < 64 ? (1L << ncpu) - 1 : -1L;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        String psCommand = NetBsdOSThread.PS_THREAD_COMMAND;
        if (getProcessID() >= 0) {
            psCommand += " -p " + getProcessID();
        }
        Predicate<Map<BsdPsThreadKeyword, String>> hasColumnsArgs = threadMap -> threadMap
                .containsKey(BsdPsThreadKeyword.ARGS);
        return ExecutingCommand.runNative(psCommand).stream().skip(1).parallel()
                .map(thread -> ParseUtil.stringToEnumMap(BsdPsThreadKeyword.class, NetBsdOSThread.PS_THREAD_KEYWORDS,
                        thread.trim(), ' '))
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

    @Override
    protected long[] queryKernelUserTime() {
        return parseKernelUserTime(FileUtil.getStringFromFile("/proc/" + getProcessID() + "/stat"));
    }

    /**
     * Parses the kernel and user time from NetBSD's Linux-format {@code /proc/<pid>/stat}, which ps does not separate.
     * Package-private for testing.
     * <p>
     * Fields 14 and 15 are the {@code p_uutime} and {@code p_ustime} of the process's {@code kinfo_proc2}, in a fixed
     * 100 ticks per second whatever the kernel's {@code hz} ({@code UTIME2TICKS} in {@code procfs_linux.c}). The
     * command name is parenthesized and may itself contain spaces or parentheses, so fields are counted from the last
     * closing parenthesis, after which field 3, the state, comes first.
     *
     * @param stat the contents of {@code /proc/<pid>/stat}, empty if procfs is not mounted or the process has exited
     * @return the kernel and user time in milliseconds, in that order, or an empty array if they could not be read
     */
    static long[] parseKernelUserTime(String stat) {
        int nameEnd = stat.lastIndexOf(')');
        if (nameEnd < 0) {
            return new long[0];
        }
        String[] split = ParseUtil.whitespaces.split(stat.substring(nameEnd + 1).trim(), -1);
        // Field n of the Linux format is at index n - 3
        if (split.length < 13) {
            return new long[0];
        }
        long utime = ParseUtil.parseLongOrDefault(split[11], -1L);
        long stime = ParseUtil.parseLongOrDefault(split[12], -1L);
        if (utime < 0L || stime < 0L) {
            return new long[0];
        }
        return new long[] { stime * 10L, utime * 10L };
    }
}
