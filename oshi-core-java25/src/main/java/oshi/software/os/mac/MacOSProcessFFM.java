/*
 * Copyright 2020-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.mac.MacSystemFunctions.getStringFromNativePointer;
import static oshi.ffm.mac.MacSystemFunctions.getStructFromNativePointer;
import static oshi.ffm.mac.MacSystemFunctions.getgrgid;
import static oshi.ffm.mac.MacSystemFunctions.getpwuid;
import static oshi.ffm.mac.MacSystemFunctions.getrlimit;
import static oshi.ffm.mac.MacSystemFunctions.proc_pid_rusage;
import static oshi.ffm.mac.MacSystemFunctions.proc_pidinfo;
import static oshi.ffm.mac.MacSystemFunctions.proc_pidpath;
import static oshi.ffm.mac.MacSystemHeaders.PROC_PIDPATHINFO_MAXSIZE;
import static oshi.ffm.mac.MacSystemHeaders.PROC_PIDTASKALLINFO;
import static oshi.ffm.mac.MacSystemHeaders.PROC_PIDVNODEPATHINFO;
import static oshi.ffm.mac.MacSystemHeaders.RUSAGE_INFO_V2;
import static oshi.ffm.mac.MacSystemStructs.GROUP;
import static oshi.ffm.mac.MacSystemStructs.PASSWD;
import static oshi.ffm.mac.MacSystemStructs.PBI_COMM;
import static oshi.ffm.mac.MacSystemStructs.PBI_FLAGS;
import static oshi.ffm.mac.MacSystemStructs.PBI_GID;
import static oshi.ffm.mac.MacSystemStructs.PBI_NFILES;
import static oshi.ffm.mac.MacSystemStructs.PBI_PPID;
import static oshi.ffm.mac.MacSystemStructs.PBI_START_TVSEC;
import static oshi.ffm.mac.MacSystemStructs.PBI_START_TVUSEC;
import static oshi.ffm.mac.MacSystemStructs.PBI_STATUS;
import static oshi.ffm.mac.MacSystemStructs.PBI_UID;
import static oshi.ffm.mac.MacSystemStructs.PBSD;
import static oshi.ffm.mac.MacSystemStructs.PROC_BSD_INFO;
import static oshi.ffm.mac.MacSystemStructs.PROC_TASK_ALL_INFO;
import static oshi.ffm.mac.MacSystemStructs.PROC_TASK_INFO;
import static oshi.ffm.mac.MacSystemStructs.PTINFO;
import static oshi.ffm.mac.MacSystemStructs.PTI_CSW;
import static oshi.ffm.mac.MacSystemStructs.PTI_FAULTS;
import static oshi.ffm.mac.MacSystemStructs.PTI_PAGEINS;
import static oshi.ffm.mac.MacSystemStructs.PTI_PRIORITY;
import static oshi.ffm.mac.MacSystemStructs.PTI_RESIDENT_SIZE;
import static oshi.ffm.mac.MacSystemStructs.PTI_THREADNUM;
import static oshi.ffm.mac.MacSystemStructs.PTI_TOTAL_SYSTEM;
import static oshi.ffm.mac.MacSystemStructs.PTI_TOTAL_USER;
import static oshi.ffm.mac.MacSystemStructs.PTI_VIRTUAL_SIZE;
import static oshi.ffm.mac.MacSystemStructs.PVI_CDIR;
import static oshi.ffm.mac.MacSystemStructs.RI_DISKIO_BYTESREAD;
import static oshi.ffm.mac.MacSystemStructs.RI_DISKIO_BYTESWRITTEN;
import static oshi.ffm.mac.MacSystemStructs.RLIMIT;
import static oshi.ffm.mac.MacSystemStructs.RLIM_CUR;
import static oshi.ffm.mac.MacSystemStructs.RLIM_MAX;
import static oshi.ffm.mac.MacSystemStructs.RUSAGEINFOV2;
import static oshi.ffm.mac.MacSystemStructs.VIP_PATH;
import static oshi.ffm.mac.MacSystemStructs.VNODE_INFO_PATH;
import static oshi.ffm.mac.MacSystemStructs.VNODE_PATH_INFO;
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

import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.mac.ThreadInfo;
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
        int pid = getProcessID();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment taskAllInfo = arena.allocate(PROC_TASK_ALL_INFO);
            int infoResult = proc_pidinfo(pid, PROC_PIDTASKALLINFO, 0L, taskAllInfo,
                    (int) PROC_TASK_ALL_INFO.byteSize());
            if (infoResult <= 0) {
                this.state = INVALID;
                return false;
            }
            MemorySegment ptinfo = taskAllInfo.asSlice(PROC_TASK_ALL_INFO.byteOffset(PTINFO),
                    PROC_TASK_INFO.byteSize());
            if (ptinfo.get(JAVA_INT, PROC_TASK_INFO.byteOffset(PTI_THREADNUM)) < 1) {
                this.state = INVALID;
                return false;
            }
            MemorySegment pbsd = taskAllInfo.asSlice(PROC_TASK_ALL_INFO.byteOffset(PBSD), PROC_BSD_INFO.byteSize());

            // Get process path
            MemorySegment pathBuf = arena.allocate(PROC_PIDPATHINFO_MAXSIZE);
            if (proc_pidpath(pid, pathBuf, PROC_PIDPATHINFO_MAXSIZE) > 0) {
                this.path = pathBuf.getString(0).trim();
                // Overwrite name with last part of path
                String[] pathSplit = this.path.split("/");
                if (pathSplit.length > 0) {
                    this.name = pathSplit[pathSplit.length - 1];
                }
            }
            if (this.name.isEmpty()) {
                // pbi_comm contains first 16 characters of name
                this.name = pbsd.asSlice(PROC_BSD_INFO.byteOffset(PBI_COMM)).getString(0);
            }

            // Get Process state based on status
            int status = pbsd.get(JAVA_INT, PROC_BSD_INFO.byteOffset(PBI_STATUS));
            this.state = switch (status) {
            case SSLEEP -> SLEEPING;
            case SWAIT -> WAITING;
            case SRUN -> RUNNING;
            case SIDL -> NEW;
            case SZOMB -> ZOMBIE;
            case SSTOP -> STOPPED;
            default -> OTHER;
            };

            // User and group info
            this.parentProcessID = pbsd.get(JAVA_INT, PROC_BSD_INFO.byteOffset(PBI_PPID));
            int uid = pbsd.get(JAVA_INT, PROC_BSD_INFO.byteOffset(PBI_UID));
            this.userID = Integer.toString(uid);
            MemorySegment pwuid = getpwuid(uid);
            if (pwuid != null) {
                MemorySegment passwdStruct = getStructFromNativePointer(pwuid, PASSWD, arena);
                MemorySegment nameAddress = passwdStruct.get(ADDRESS, PASSWD.byteOffset(groupElement("pw_name")));
                this.user = getStringFromNativePointer(nameAddress, arena);
            } else {
                this.user = this.userID;
            }

            int gid = pbsd.get(JAVA_INT, PROC_BSD_INFO.byteOffset(PBI_GID));
            this.groupID = Integer.toString(gid);
            MemorySegment grgid = getgrgid(gid);
            if (grgid != null) {
                MemorySegment groupStruct = getStructFromNativePointer(pwuid, GROUP, arena);
                MemorySegment nameAddress = groupStruct.get(ADDRESS, GROUP.byteOffset(groupElement("gr_name")));
                this.group = getStringFromNativePointer(nameAddress, arena);
            } else {
                this.group = this.groupID;
            }

            // Process metrics
            this.threadCount = ptinfo.get(JAVA_INT, PROC_TASK_INFO.byteOffset(PTI_THREADNUM));
            this.priority = ptinfo.get(JAVA_INT, PROC_TASK_INFO.byteOffset(PTI_PRIORITY));
            this.virtualSize = ptinfo.get(JAVA_LONG, PROC_TASK_INFO.byteOffset(PTI_VIRTUAL_SIZE));
            this.residentSetSize = ptinfo.get(JAVA_LONG, PROC_TASK_INFO.byteOffset(PTI_RESIDENT_SIZE));
            this.kernelTime = ptinfo.get(JAVA_LONG, PROC_TASK_INFO.byteOffset(PTI_TOTAL_SYSTEM)) / TICKS_PER_MS;
            this.userTime = ptinfo.get(JAVA_LONG, PROC_TASK_INFO.byteOffset(PTI_TOTAL_USER)) / TICKS_PER_MS;

            long startSec = pbsd.get(JAVA_LONG, PROC_BSD_INFO.byteOffset(PBI_START_TVSEC));
            long startUsec = pbsd.get(JAVA_LONG, PROC_BSD_INFO.byteOffset(PBI_START_TVUSEC));
            this.startTime = startSec * 1000L + startUsec / 1000L;
            this.upTime = now - this.startTime;

            this.openFiles = pbsd.get(JAVA_INT, PROC_BSD_INFO.byteOffset(PBI_NFILES));
            int flags = pbsd.get(JAVA_INT, PROC_BSD_INFO.byteOffset(PBI_FLAGS));
            this.bitness = (flags & P_LP64) == 0 ? 32 : 64;

            this.majorFaults = ptinfo.get(JAVA_INT, PROC_TASK_INFO.byteOffset(PTI_PAGEINS));
            int totalFaults = ptinfo.get(JAVA_INT, PROC_TASK_INFO.byteOffset(PTI_FAULTS));
            // testing using getrusage confirms pti_faults includes both major and minor
            this.minorFaults = totalFaults - this.majorFaults;
            this.contextSwitches = ptinfo.get(JAVA_INT, PROC_TASK_INFO.byteOffset(PTI_CSW));

            // Get rusage info for newer OS versions
            if (this.majorVersion > 10 || this.minorVersion >= 9) {
                MemorySegment rusage = arena.allocate(RUSAGEINFOV2);
                if (0 == proc_pid_rusage(pid, RUSAGE_INFO_V2, rusage)) {
                    this.bytesRead = rusage.get(JAVA_LONG, RUSAGEINFOV2.byteOffset(RI_DISKIO_BYTESREAD));
                    this.bytesWritten = rusage.get(JAVA_LONG, RUSAGEINFOV2.byteOffset(RI_DISKIO_BYTESWRITTEN));
                }
            }

            // Get working directory info
            MemorySegment vnodeInfo = arena.allocate(VNODE_PATH_INFO);
            if (proc_pidinfo(pid, PROC_PIDVNODEPATHINFO, 0L, vnodeInfo, (int) VNODE_PATH_INFO.byteSize()) > 0) {
                // Get the current directory path using nested path elements
                this.currentWorkingDirectory = vnodeInfo.asSlice(VNODE_PATH_INFO.byteOffset(PVI_CDIR))
                        .asSlice(VNODE_INFO_PATH.byteOffset(VIP_PATH)).getString(0);
            }
        } catch (Throwable e) {
            this.state = INVALID;
            return false;
        }
        return true;
    }
}
