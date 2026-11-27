/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.dragonflybsd;

import static oshi.software.common.os.unix.bsd.BsdPsKeyword.COMMAND;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.MAJFLT;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.MINFLT;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.NIVCSW;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.NLWP;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.NVCSW;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.PID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.PPID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.PRI;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.RGID;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.RSS;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.STATE;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.TIME;
import static oshi.software.common.os.unix.bsd.BsdPsKeyword.UCOMM;
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
import oshi.software.os.OperatingSystem;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.dragonflybsd.ProcstatUtil;

/**
 * Abstract base for a DragonFly BSD OSProcess. Inherits the shared BSD {@code ps} parsing and adds the fields whose
 * column set differs on DragonFly.
 */
public abstract class DragonFlyBsdOSProcess extends BsdOSProcess {

    /**
     * Ordered {@code ps} columns queried for each process. Shared by the DragonFlyBsdOSProcess subclasses (JNA/FFM) and
     * the DragonFly OperatingSystem classes so the column list and parsing stay in lockstep. {@code COMMAND} must
     * remain last.
     */
    public static final List<BsdPsKeyword> PS_KEYWORDS = Collections.unmodifiableList(Arrays.asList(STATE, PID, PPID,
            USER, UID, RGID, NLWP, PRI, VSZ, RSS, TIME, MAJFLT, MINFLT, NVCSW, NIVCSW, UCOMM, COMMAND));

    /**
     * The {@code ps -o} column list for {@link #PS_KEYWORDS}. DragonFly's {@code ps} prints a size wider than its
     * column scaled, as {@code 5.65G}, and a column is as wide as its header, so {@code vsz} and {@code rss} get
     * headers wide enough for any count of KiB.
     */
    public static final String PS_COMMAND_ARGS = PS_KEYWORDS.stream().map(DragonFlyBsdOSProcess::psColumn)
            .collect(Collectors.joining(","));

    private static String psColumn(BsdPsKeyword keyword) {
        String column = keyword.name().toLowerCase(Locale.ROOT);
        return keyword == VSZ || keyword == RSS ? column + "=" + keyword.name() + "_IN_KIB_UNSCALED" : column;
    }

    /**
     * The {@code KERN_PROC} sysctl subtype that returns a process's executable path (12 on FreeBSD).
     */
    protected static final int KERN_PROC_PATHNAME = 9;

    /**
     * The size of the buffer for an executable path, the platform's {@code PATH_MAX}.
     */
    protected static final int PATH_MAX = 1024;

    /**
     * The longest command name the kernel keeps, {@code MAXCOMLEN}; {@code ucomm} is cut to this length.
     */
    private static final int MAXCOMLEN = 16;

    /**
     * The longest {@code ps} command column that {@code MAXCOMLEN} bytes of arguments can print as: every byte escaped
     * as {@code \ooo}, followed by a {@code " (ucomm)"} suffix.
     */
    private static final int MAX_PRINTED_COMLEN = 4 * MAXCOMLEN + MAXCOMLEN + 3;

    private final OperatingSystem os;

    private volatile String ucomm = "";

    /**
     * Constructs a new {@code DragonFlyBsdOSProcess}.
     *
     * @param pid the process ID
     * @param os  the operating system this process runs on
     */
    protected DragonFlyBsdOSProcess(int pid, OperatingSystem os) {
        super(pid);
        this.os = os;
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
    public String getCurrentWorkingDirectory() {
        return ProcstatUtil.getCwd(getProcessID());
    }

    @Override
    public long getOpenFiles() {
        return ProcstatUtil.getOpenFiles(getProcessID());
    }

    @Override
    protected int queryOwnProcessId() {
        return this.os.getProcessId();
    }

    /**
     * {@inheritDoc}
     * <p>
     * On DragonFly BSD only the current process is asked; every other process falls back to the {@code ps} column. The
     * kernel's {@code KERN_PROC_PATHNAME} handler dereferences the target's file-descriptor table without checking it,
     * and an exiting process frees that table before it drops its executable's name, so querying another process at the
     * wrong moment panics the kernel.
     */
    @Override
    protected final String queryExecutablePath() {
        return getProcessID() == queryOwnProcessId() ? queryOwnExecutablePath() : "";
    }

    /**
     * Returns the full path of the current process's executable through the {@code KERN_PROC_PATHNAME} sysctl. Only
     * called when this process is the current one; see {@link #queryExecutablePath()}.
     *
     * @return the path, or an empty string if it is unavailable
     */
    protected abstract String queryOwnExecutablePath();

    /**
     * {@inheritDoc}
     * <p>
     * Every process but the current one falls back to {@code argv[0]} from {@code /proc/<pid>/cmdline}, which holds the
     * path when the process was started by one and is not cut to {@code MAXCOMLEN} as {@code ucomm} is. A process can
     * rewrite {@code argv[0]}, so it is used only when it names the same program as {@code ucomm}; otherwise, or
     * without procfs, the result is {@code ucomm}.
     */
    @Override
    protected String queryPath(Map<BsdPsKeyword, String> psMap) {
        String path = queryExecutablePath();
        if (!path.isEmpty()) {
            return path;
        }
        String command = ParseUtil.getStringValueOrEmpty(psMap.get(UCOMM));
        // The NUL-separated arguments keep a path with spaces whole, which ps's space-separated column does not
        List<String> args = queryArguments();
        String argv0 = args.isEmpty() ? "" : args.get(0);
        String argv0Name = argv0.substring(argv0.lastIndexOf('/') + 1);
        return !command.isEmpty() && namesProgram(argv0Name, command) ? argv0 : command;
    }

    /**
     * Whether the last element of {@code argv[0]} names the program whose kernel command name is {@code ucomm}.
     *
     * @param argv0Name the last element of {@code argv[0]}
     * @param ucomm     the kernel's command name, cut to {@code MAXCOMLEN}
     * @return {@code true} if it is the same name, or extends a truncated {@code ucomm} with more of a file name
     */
    static boolean namesProgram(String argv0Name, String ucomm) {
        if (argv0Name.equals(ucomm)) {
            return true;
        }
        // A setproctitle() title, "name: status", also extends ucomm, but a file name rarely holds a colon or a space
        return ucomm.length() == MAXCOMLEN && argv0Name.startsWith(ucomm)
                && argv0Name.substring(MAXCOMLEN).chars().noneMatch(c -> c == ':' || Character.isWhitespace(c));
    }

    /**
     * {@inheritDoc}
     * <p>
     * DragonFly's {@code /proc/<pid>/cmdline} holds the kernel's argument cache, which is dropped for a command line
     * longer than {@code kern.ps_arg_cache_limit}. The file then holds another process's {@code MAXCOMLEN} command name
     * buffer, and the current process loses its arguments past the limit. {@code ps} reads the arguments from the
     * process's memory instead, so where the two disagree its command column is used, split on whitespace. That split
     * cannot keep an argument containing a space whole, and keeps the escapes {@code ps} prints for unprintable
     * characters.
     */
    @Override
    protected List<String> queryArguments() {
        byte[] cmdline = FileUtil.readAllBytes("/proc/" + getProcessID() + "/cmdline", false);
        return Collections.unmodifiableList(reconcileArguments(cmdline, this.commandLineBackup, this.ucomm));
    }

    @Override
    protected String queryCommandLineBackup(Map<BsdPsKeyword, String> psMap) {
        this.ucomm = ParseUtil.getStringValueOrEmpty(psMap.get(UCOMM));
        return super.queryCommandLineBackup(psMap);
    }

    /**
     * Chooses between the arguments in {@code /proc/<pid>/cmdline} and the {@code ps} command column.
     *
     * @param cmdline   the bytes of {@code /proc/<pid>/cmdline}
     * @param psCommand the {@code ps} command column, or an empty string if unknown
     * @param ucomm     the {@code ps} command name, or an empty string if unknown
     * @return the arguments: the file's when it has none or they agree with {@code ps}, since only the file keeps an
     *         argument that contains a space whole; otherwise those {@code ps} printed, or none if it could read none
     *         either
     */
    static List<String> reconcileArguments(byte[] cmdline, String psCommand, String ucomm) {
        List<String> args = ParseUtil.parseByteArrayToStrings(cmdline);
        String joined = String.join(" ", args);
        // Without the file, a process title cannot be told from arguments; the command line falls back to ps's
        if (args.isEmpty() || psCommand.isEmpty() || psCommand.equals(joined)) {
            return args;
        }
        // ps prints "(ucomm)" for a process whose arguments it cannot read, so the file holds the name buffer too
        if (psCommand.startsWith("(") && psCommand.endsWith(")")) {
            return cmdline.length == MAXCOMLEN ? Collections.emptyList() : args;
        }
        // The kernel drops its argument cache only past kern.ps_arg_cache_limit (256 by default), so the file holds the
        // name buffer only when ps prints more than any MAXCOMLEN bytes of arguments could. That buffer holds ucomm,
        // not argv[0], so argv[0] then comes from ps
        boolean nameBuffer = cmdline.length == MAXCOMLEN && psCommand.length() > MAX_PRINTED_COMLEN;
        String argv0 = nameBuffer ? ParseUtil.whitespaces.split(psCommand.trim(), 2)[0] : args.get(0);
        // ps follows the arguments with " (ucomm)" when the last element of argv[0] does not name the command in its
        // first MAXCOMLEN characters, as with a setproctitle() title; otherwise a trailing "(ucomm)" is an argument
        String suffix = " (" + ucomm + ")";
        boolean decorated = !ucomm.isEmpty() && psCommand.endsWith(suffix)
                && !firstComlen(argv0.substring(argv0.lastIndexOf('/') + 1)).equals(firstComlen(ucomm));
        String printed = decorated ? psCommand.substring(0, psCommand.length() - suffix.length()) : psCommand;
        if (printed.equals(joined)) {
            return args;
        }
        // The current process's file stops at an argument boundary; ps escaping a character only looks like a prefix
        boolean truncated = printed.startsWith(joined + " ");
        if (nameBuffer || truncated) {
            return Arrays.asList(ParseUtil.whitespaces.split(printed.trim(), -1));
        }
        return args;
    }

    private static String firstComlen(String name) {
        return name.length() > MAXCOMLEN ? name.substring(0, MAXCOMLEN) : name;
    }

    @Override
    protected int queryBitness() {
        // DragonFly BSD has no KERN_PROC_SV_NAME (its KERN_PROC subtype 9 is the executable's path) and no ABI
        // compatibility layer: an x86_64 kernel runs only 64-bit programs (kern.elf64 exists, kern.elf32 does not), and
        // the i386 kernel of 3.8 and earlier only 32-bit ones. So every process has the operating system's bitness.
        return this.os.getBitness();
    }

    @Override
    protected long queryStartTimeMillis() {
        return queryStartTimeFromProc(getProcessID());
    }

    @Override
    protected long[] queryKernelUserTime() {
        return parseKernelUserTime(FileUtil.readFile("/proc/" + getProcessID() + "/status", false));
    }

    /**
     * Parses the kernel and user time from DragonFly's {@code /proc/<pid>/status}, which ps does not separate.
     * Package-private for testing.
     * <p>
     * The ninth and tenth fields are the user and system time as {@code seconds,microseconds}. The command name, the
     * first field, has its spaces escaped, so whitespace separates every field.
     *
     * @param status the lines of {@code /proc/<pid>/status}, empty if the process has exited
     * @return the kernel and user time in milliseconds, in that order, or an empty array if they could not be read
     */
    static long[] parseKernelUserTime(List<String> status) {
        if (status.isEmpty()) {
            return new long[0];
        }
        String[] split = ParseUtil.whitespaces.split(status.get(0).trim(), -1);
        if (split.length < 10) {
            return new long[0];
        }
        long user = secUsecToMillis(split[8]);
        long system = secUsecToMillis(split[9]);
        if (user < 0L || system < 0L) {
            return new long[0];
        }
        return new long[] { system, user };
    }

    private static long secUsecToMillis(String secUsec) {
        String[] parts = secUsec.split(",", -1);
        if (parts.length != 2) {
            return -1L;
        }
        long seconds = ParseUtil.parseLongOrDefault(parts[0], -1L);
        long micros = ParseUtil.parseLongOrDefault(parts[1], -1L);
        return seconds < 0L || micros < 0L ? -1L : seconds * 1000L + micros / 1000L;
    }

    private static long queryStartTimeFromProc(int pid) {
        List<String> status = FileUtil.readFile("/proc/" + pid + "/status", false);
        if (!status.isEmpty()) {
            // Format: name pid ... startSec,startUsec ...
            String[] split = ParseUtil.whitespaces.split(status.get(0).trim(), -1);
            if (split.length >= 8) {
                String[] timeParts = split[7].split(",", -1);
                long seconds = ParseUtil.parseLongOrDefault(timeParts[0], 0L);
                if (seconds > 0) {
                    return seconds * 1000L;
                }
            }
        }
        // Unknown start time: return the sentinel so updateAttributes() falls back to elapsed time
        return -1L;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        String psCommand = "ps -wwxo " + DragonFlyBsdOSThread.PS_THREAD_COLUMNS + " -H";
        if (getProcessID() >= 0) {
            psCommand += " -p " + getProcessID();
        }
        Predicate<Map<BsdPsThreadKeyword, String>> hasColumnsPri = threadMap -> threadMap
                .containsKey(BsdPsThreadKeyword.PRI);
        return ExecutingCommand.runNative(psCommand).stream().skip(1).parallel()
                .map(thread -> ParseUtil.stringToEnumMap(BsdPsThreadKeyword.class,
                        DragonFlyBsdOSThread.PS_THREAD_KEYWORDS, thread.trim(), ' '))
                .filter(hasColumnsPri).map(threadMap -> new DragonFlyBsdOSThread(getProcessID(), threadMap))
                .filter(VALID_THREAD).collect(Collectors.toList());
    }

    @Override
    public long getAffinityMask() {
        // DragonFly has no cpuset command, which the other BSDs read affinity from
        return queryAffinityMask();
    }

    /**
     * Reads this process's CPU affinity mask through {@code sched_getaffinity}, which reports the mask of its first LWP
     * limited to the active CPUs. The kernel allows it for any process.
     *
     * @return the mask of CPUs 0 to 63, or 0 if the process has exited
     */
    protected abstract long queryAffinityMask();
}
