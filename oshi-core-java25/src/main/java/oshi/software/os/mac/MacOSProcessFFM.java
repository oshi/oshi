/*
 * Copyright 2020-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.mac.MacSystemFunctions.getrlimit;
import static oshi.ffm.mac.MacSystemStructs.RLIMIT;
import static oshi.ffm.mac.MacSystemStructs.RLIM_CUR;
import static oshi.ffm.mac.MacSystemStructs.RLIM_MAX;
import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSProcess.State.NEW;
import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.WAITING;
import static oshi.software.os.OSProcess.State.ZOMBIE;
import static oshi.util.Memoizer.memoize;
import static oshi.util.platform.mac.SysctlUtilFFM.sysctl;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.mac.SystemB.Group;
import com.sun.jna.platform.mac.SystemB.Passwd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.mac.ThreadInfo;
import oshi.jna.Struct.CloseableProcTaskAllInfo;
import oshi.jna.Struct.CloseableRUsageInfoV2;
import oshi.jna.Struct.CloseableVnodePathInfo;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.util.GlobalConfig;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * OSProcess implementation
 */
@ThreadSafe
public class MacOSProcessFFM extends AbstractOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(MacOSProcessFFM.class);

    private static final int ARGMAX = sysctl("kern.argmax", 0);
    private static final long TICKS_PER_MS;
    static {
        // default to 1 tick per nanosecond
        long ticksPerSec = 1_000_000_000L;
        IOIterator iter = IOKitUtil.getMatchingServices("IOPlatformDevice");
        if (iter != null) {
            IORegistryEntry cpu = iter.next();
            while (cpu != null) {
                try {
                    String s = cpu.getName().toLowerCase(Locale.ROOT);
                    if (s.startsWith("cpu") && s.length() > 3) {
                        byte[] data = cpu.getByteArrayProperty("timebase-frequency");
                        if (data != null) {
                            ticksPerSec = ParseUtil.byteArrayToLong(data, 4, false);
                            break;
                        }
                    }
                } finally {
                    cpu.release();
                }
                cpu = iter.next();
            }
            iter.release();
        }
        // Convert to ticks per millisecond
        TICKS_PER_MS = ticksPerSec / 1000L;
    }

    private static final boolean LOG_MAC_SYSCTL_WARNING = GlobalConfig.get(GlobalConfig.OSHI_OS_MAC_SYSCTL_LOGWARNING,
            false);

    private static final int MAC_RLIMIT_NOFILE = 8;

    // 64-bit flag
    private static final int P_LP64 = 0x4;
    /*
     * macOS States:
     */
    private static final int SSLEEP = 1; // sleeping on high priority
    private static final int SWAIT = 2; // sleeping on low priority
    private static final int SRUN = 3; // running
    private static final int SIDL = 4; // intermediate state in process creation
    private static final int SZOMB = 5; // intermediate state in process termination
    private static final int SSTOP = 6; // process being traced

    private int majorVersion;
    private int minorVersion;
    private final MacOperatingSystemFFM os;

    private Supplier<String> commandLine = memoize(this::queryCommandLine);
    private Supplier<Pair<List<String>, Map<String, String>>> argsEnviron = memoize(this::queryArgsAndEnvironment);

    private String name = "";
    private String path = "";
    private String currentWorkingDirectory;
    private String user;
    private String userID;
    private String group;
    private String groupID;
    private State state = INVALID;
    private int parentProcessID;
    private int threadCount;
    private int priority;
    private long virtualSize;
    private long residentSetSize;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private long bytesRead;
    private long bytesWritten;
    private long openFiles;
    private int bitness;
    private long minorFaults;
    private long majorFaults;
    private long contextSwitches;

    public MacOSProcessFFM(int pid, int major, int minor, MacOperatingSystemFFM os) {
        super(pid);
        this.majorVersion = major;
        this.minorVersion = minor;
        this.os = os;
        updateAttributes();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public String getCommandLine() {
        return this.commandLine.get();
    }

    private String queryCommandLine() {
        return String.join(" ", getArguments());
    }

    @Override
    public List<String> getArguments() {
        return argsEnviron.get().getA();
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return argsEnviron.get().getB();
    }

    private Pair<List<String>, Map<String, String>> queryArgsAndEnvironment() {
        int pid = getProcessID();
        // Set up return objects
        List<String> args = new ArrayList<>();
        // API does not specify any particular order of entries, but it is reasonable to
        // maintain whatever order the OS provided to the end user
        Map<String, String> env = new LinkedHashMap<>();

        // Get command line via sysctl
        int[] mib = { 1, 49, pid }; // CTL_KERN, KERN_PROCARGS2, pid
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment procargs = arena.allocate(ARGMAX);
            procargs.fill((byte) 0);

            long size = sysctl(mib, procargs);
            if (size > 0) {
                // Procargs contains an int representing total # of args, followed by a
                // null-terminated execpath string and then the arguments, each
                // null-terminated (possible multiple consecutive nulls),
                // The execpath string is also the first arg.
                // Following this is an int representing total # of env, followed by
                // null-terminated envs in similar format
                int nargs = procargs.get(JAVA_INT, 0);
                // Sanity check
                if (nargs > 0 && nargs <= 1024) {
                    // Skip first int (containing value of nargs)
                    long offset = Integer.BYTES;
                    // Skip exec_command
                    offset += procargs.getString(offset).length();
                    // Iterate character by character using offset
                    // Build each arg and add to list
                    while (offset < size) {
                        // Skip null bytes
                        while (offset < size && procargs.get(JAVA_BYTE, offset) == 0) {
                            offset++;
                        }
                        if (offset >= size) {
                            break;
                        }
                        // Read string until null terminator
                        String arg = procargs.getString(offset);
                        if (arg.isEmpty()) {
                            break;
                        }
                        if (nargs-- > 0) {
                            // Still processing arguments
                            args.add(arg);
                        } else {
                            // Processing environment variables
                            int idx = arg.indexOf('=');
                            if (idx > 0) {
                                env.put(arg.substring(0, idx), arg.substring(idx + 1));
                            }
                        }
                        offset += arg.length() + 1; // +1 for null terminator
                    }
                }
            } else {
                // Don't warn for pid 0
                if (pid > 0 && LOG_MAC_SYSCTL_WARNING) {
                    LOG.warn("Failed sysctl call for process arguments (kern.procargs2), process {} may not exist.",
                            pid);
                }
            }
        }
        return new Pair<>(Collections.unmodifiableList(args), Collections.unmodifiableMap(env));
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return this.currentWorkingDirectory;
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
    public State getState() {
        return this.state;
    }

    @Override
    public int getParentProcessID() {
        return this.parentProcessID;
    }

    @Override
    public int getThreadCount() {
        return this.threadCount;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        long now = System.currentTimeMillis();
        return ThreadInfo.queryTaskThreads(getProcessID()).stream().parallel().map(stat -> {
            // For long running threads the start time calculation can overestimate
            long start = Math.max(now - stat.getUpTime(), getStartTime());
            return new MacOSThread(getProcessID(), stat.getThreadId(), stat.getState(), stat.getSystemTime(),
                    stat.getUserTime(), start, now - start, stat.getPriority());
        }).collect(Collectors.toList());
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public long getVirtualSize() {
        return this.virtualSize;
    }

    @Override
    public long getResidentSetSize() {
        return this.residentSetSize;
    }

    @Override
    public long getKernelTime() {
        return this.kernelTime;
    }

    @Override
    public long getUserTime() {
        return this.userTime;
    }

    @Override
    public long getUpTime() {
        return this.upTime;
    }

    @Override
    public long getStartTime() {
        return this.startTime;
    }

    @Override
    public long getBytesRead() {
        return this.bytesRead;
    }

    @Override
    public long getBytesWritten() {
        return this.bytesWritten;
    }

    @Override
    public long getOpenFiles() {
        return this.openFiles;
    }

    @Override
    public long getSoftOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment buffer = arena.allocate(RLIMIT);
                int result = getrlimit(MAC_RLIMIT_NOFILE, buffer);
                if (result > 0) {
                    return buffer.get(JAVA_LONG, RLIMIT.byteOffset(RLIM_CUR));
                }
            } catch (Throwable e) {
                // Ignore, return 0 below
            }
            return 0;
        }
        return -1L; // not supported
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment buffer = arena.allocate(RLIMIT);
                int result = getrlimit(MAC_RLIMIT_NOFILE, buffer);
                if (result > 0) {
                    return buffer.get(JAVA_LONG, RLIMIT.byteOffset(RLIM_MAX));
                }
            } catch (Throwable e) {
                // Ignore, return 0 below
            }
            return 0;
        }
        return -1L; // not supported
    }

    @Override
    public int getBitness() {
        return this.bitness;
    }

    @Override
    public long getAffinityMask() {
        // macOS doesn't do affinity. Return a bitmask of the current processors.
        int logicalProcessorCount = sysctl("hw.logicalcpu", 1);
        return logicalProcessorCount < 64 ? (1L << logicalProcessorCount) - 1 : -1L;
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
    public long getContextSwitches() {
        return this.contextSwitches;
    }

    @Override
    public boolean updateAttributes() {
        long now = System.currentTimeMillis();
        try (CloseableProcTaskAllInfo taskAllInfo = new CloseableProcTaskAllInfo()) {
            if (0 > SystemB.INSTANCE.proc_pidinfo(getProcessID(), SystemB.PROC_PIDTASKALLINFO, 0, taskAllInfo,
                    taskAllInfo.size()) || taskAllInfo.ptinfo.pti_threadnum < 1) {
                this.state = INVALID;
                return false;
            }
            try (Memory buf = new Memory(SystemB.PROC_PIDPATHINFO_MAXSIZE)) {
                if (0 < SystemB.INSTANCE.proc_pidpath(getProcessID(), buf, SystemB.PROC_PIDPATHINFO_MAXSIZE)) {
                    this.path = buf.getString(0).trim();
                    // Overwrite name with last part of path
                    String[] pathSplit = this.path.split("/");
                    if (pathSplit.length > 0) {
                        this.name = pathSplit[pathSplit.length - 1];
                    }
                }
            }
            if (this.name.isEmpty()) {
                // pbi_comm contains first 16 characters of name
                this.name = Native.toString(taskAllInfo.pbsd.pbi_comm, StandardCharsets.UTF_8);
            }

            switch (taskAllInfo.pbsd.pbi_status) {
            case SSLEEP:
                this.state = SLEEPING;
                break;
            case SWAIT:
                this.state = WAITING;
                break;
            case SRUN:
                this.state = RUNNING;
                break;
            case SIDL:
                this.state = NEW;
                break;
            case SZOMB:
                this.state = ZOMBIE;
                break;
            case SSTOP:
                this.state = STOPPED;
                break;
            default:
                this.state = OTHER;
                break;
            }
            this.parentProcessID = taskAllInfo.pbsd.pbi_ppid;
            this.userID = Integer.toString(taskAllInfo.pbsd.pbi_uid);
            Passwd pwuid = SystemB.INSTANCE.getpwuid(taskAllInfo.pbsd.pbi_uid);
            this.user = pwuid == null ? Integer.toString(taskAllInfo.pbsd.pbi_uid) : pwuid.pw_name;
            this.groupID = Integer.toString(taskAllInfo.pbsd.pbi_gid);
            Group grgid = SystemB.INSTANCE.getgrgid(taskAllInfo.pbsd.pbi_gid);
            this.group = grgid == null ? Integer.toString(taskAllInfo.pbsd.pbi_gid) : grgid.gr_name;
            this.threadCount = taskAllInfo.ptinfo.pti_threadnum;
            this.priority = taskAllInfo.ptinfo.pti_priority;
            this.virtualSize = taskAllInfo.ptinfo.pti_virtual_size;
            this.residentSetSize = taskAllInfo.ptinfo.pti_resident_size;
            this.kernelTime = taskAllInfo.ptinfo.pti_total_system / TICKS_PER_MS;
            this.userTime = taskAllInfo.ptinfo.pti_total_user / TICKS_PER_MS;
            this.startTime = taskAllInfo.pbsd.pbi_start_tvsec * 1000L + taskAllInfo.pbsd.pbi_start_tvusec / 1000L;
            this.upTime = now - this.startTime;
            this.openFiles = taskAllInfo.pbsd.pbi_nfiles;
            this.bitness = (taskAllInfo.pbsd.pbi_flags & P_LP64) == 0 ? 32 : 64;
            this.majorFaults = taskAllInfo.ptinfo.pti_pageins;
            // testing using getrusage confirms pti_faults includes both major and minor
            this.minorFaults = taskAllInfo.ptinfo.pti_faults - taskAllInfo.ptinfo.pti_pageins; // NOSONAR squid:S2184
            this.contextSwitches = taskAllInfo.ptinfo.pti_csw;
        }
        if (this.majorVersion > 10 || this.minorVersion >= 9) {
            try (CloseableRUsageInfoV2 rUsageInfoV2 = new CloseableRUsageInfoV2()) {
                if (0 == SystemB.INSTANCE.proc_pid_rusage(getProcessID(), SystemB.RUSAGE_INFO_V2, rUsageInfoV2)) {
                    this.bytesRead = rUsageInfoV2.ri_diskio_bytesread;
                    this.bytesWritten = rUsageInfoV2.ri_diskio_byteswritten;
                }
            }
        }
        try (CloseableVnodePathInfo vpi = new CloseableVnodePathInfo()) {
            if (0 < SystemB.INSTANCE.proc_pidinfo(getProcessID(), SystemB.PROC_PIDVNODEPATHINFO, 0, vpi, vpi.size())) {
                this.currentWorkingDirectory = Native.toString(vpi.pvi_cdir.vip_path, StandardCharsets.US_ASCII);
            }
        }
        return true;
    }
}
