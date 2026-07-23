/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.callInArenaBooleanOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaLongOrDefault;
import static oshi.ffm.ForeignFunctions.readFixedWidthString;
import static oshi.ffm.platform.mac.MacSystem.GROUP;
import static oshi.ffm.platform.mac.MacSystem.MAXCOMLEN;
import static oshi.ffm.platform.mac.MacSystem.MAXPATHLEN;
import static oshi.ffm.platform.mac.MacSystem.PASSWD;
import static oshi.ffm.platform.mac.MacSystem.PBI_COMM;
import static oshi.ffm.platform.mac.MacSystem.PBI_FLAGS;
import static oshi.ffm.platform.mac.MacSystem.PBI_GID;
import static oshi.ffm.platform.mac.MacSystem.PBI_NFILES;
import static oshi.ffm.platform.mac.MacSystem.PBI_PPID;
import static oshi.ffm.platform.mac.MacSystem.PBI_START_TVSEC;
import static oshi.ffm.platform.mac.MacSystem.PBI_START_TVUSEC;
import static oshi.ffm.platform.mac.MacSystem.PBI_STATUS;
import static oshi.ffm.platform.mac.MacSystem.PBI_UID;
import static oshi.ffm.platform.mac.MacSystem.PBSD;
import static oshi.ffm.platform.mac.MacSystem.PROC_BSD_INFO;
import static oshi.ffm.platform.mac.MacSystem.PROC_PIDPATHINFO_MAXSIZE;
import static oshi.ffm.platform.mac.MacSystem.PROC_PIDTASKALLINFO;
import static oshi.ffm.platform.mac.MacSystem.PROC_PIDVNODEPATHINFO;
import static oshi.ffm.platform.mac.MacSystem.PROC_TASK_ALL_INFO;
import static oshi.ffm.platform.mac.MacSystem.PROC_TASK_INFO;
import static oshi.ffm.platform.mac.MacSystem.PTINFO;
import static oshi.ffm.platform.mac.MacSystem.PTI_CSW;
import static oshi.ffm.platform.mac.MacSystem.PTI_FAULTS;
import static oshi.ffm.platform.mac.MacSystem.PTI_PAGEINS;
import static oshi.ffm.platform.mac.MacSystem.PTI_PRIORITY;
import static oshi.ffm.platform.mac.MacSystem.PTI_RESIDENT_SIZE;
import static oshi.ffm.platform.mac.MacSystem.PTI_THREADNUM;
import static oshi.ffm.platform.mac.MacSystem.PTI_TOTAL_SYSTEM;
import static oshi.ffm.platform.mac.MacSystem.PTI_TOTAL_USER;
import static oshi.ffm.platform.mac.MacSystem.PTI_VIRTUAL_SIZE;
import static oshi.ffm.platform.mac.MacSystem.PVI_CDIR;
import static oshi.ffm.platform.mac.MacSystem.RI_DISKIO_BYTESREAD;
import static oshi.ffm.platform.mac.MacSystem.RI_DISKIO_BYTESWRITTEN;
import static oshi.ffm.platform.mac.MacSystem.RI_PHYS_FOOTPRINT;
import static oshi.ffm.platform.mac.MacSystem.RLIMIT;
import static oshi.ffm.platform.mac.MacSystem.RLIM_CUR;
import static oshi.ffm.platform.mac.MacSystem.RLIM_MAX;
import static oshi.ffm.platform.mac.MacSystem.RUSAGEINFOV2;
import static oshi.ffm.platform.mac.MacSystem.RUSAGE_INFO_V2;
import static oshi.ffm.platform.mac.MacSystem.VIP_PATH;
import static oshi.ffm.platform.mac.MacSystem.VNODE_INFO_PATH;
import static oshi.ffm.platform.mac.MacSystem.VNODE_PATH_INFO;
import static oshi.ffm.platform.mac.MacSystemFunctions.getgrgid;
import static oshi.ffm.platform.mac.MacSystemFunctions.getpwuid;
import static oshi.ffm.platform.mac.MacSystemFunctions.getrlimit;
import static oshi.ffm.platform.mac.MacSystemFunctions.proc_pid_rusage;
import static oshi.ffm.platform.mac.MacSystemFunctions.proc_pidinfo;
import static oshi.ffm.platform.mac.MacSystemFunctions.proc_pidpath;
import static oshi.ffm.util.platform.mac.SysctlUtilFFM.sysctl;
import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.util.LogLevel.DEBUG;
import static oshi.util.LogLevel.TRACE;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;
import oshi.ffm.platform.mac.IOKit.IOIterator;
import oshi.ffm.platform.mac.IOKit.IORegistryEntry;
import oshi.ffm.platform.mac.MacSystemFunctions;
import oshi.ffm.util.platform.mac.IOKitUtilFFM;
import oshi.software.common.os.mac.MacOSProcess;
import oshi.software.common.os.mac.MacOperatingSystem;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * FFM-backed macOS OSProcess.
 */
@ThreadSafe
public class MacOSProcessFFM extends MacOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(MacOSProcessFFM.class);

    private static final int ARGMAX = sysctl("kern.argmax", 0);
    private static final long TICKS_PER_MS;
    static {
        // default to 1 tick per nanosecond
        long ticksPerSec = 1_000_000_000L;
        IOIterator iter = IOKitUtilFFM.getMatchingServices("IOPlatformDevice");
        if (iter != null) {
            IORegistryEntry cpu = iter.next();
            while (cpu != null) {
                try (IORegistryEntry current = cpu) {
                    String s = current.getName().toLowerCase(Locale.ROOT);
                    if (s.startsWith("cpu") && s.length() > 3) {
                        // Frequency is typically only on lowest-numbered P-core
                        byte[] data = current.getByteArrayProperty("timebase-frequency");
                        if (data != null) {
                            ticksPerSec = ParseUtil.byteArrayToLong(data, 4, false);
                            break;
                        }
                    }
                }
                cpu = iter.next();
            }
            iter.release();
        }
        // Convert to ticks per millisecond
        TICKS_PER_MS = ticksPerSec / 1000L;
    }

    public MacOSProcessFFM(int pid, int major, int minor, MacOperatingSystem os) {
        super(pid, major, minor, os);
        updateAttributes();
    }

    @Override
    protected Pair<List<String>, Map<String, String>> queryArgsAndEnvironment() {
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
    protected int queryLogicalProcessorCount() {
        return sysctl("hw.logicalcpu", 1);
    }

    @Override
    public long getSoftOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            return callInArenaLongOrDefault(arena -> {
                MemorySegment buffer = arena.allocate(RLIMIT);
                int result = getrlimit(MAC_RLIMIT_NOFILE, buffer);
                if (result == 0) {
                    return buffer.get(JAVA_LONG, RLIMIT.byteOffset(RLIM_CUR));
                }
                return 0L;
            }, LOG, DEBUG, "Failed to query soft open file limit", 0L);
        }
        return -1L; // not supported
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            return callInArenaLongOrDefault(arena -> {
                MemorySegment buffer = arena.allocate(RLIMIT);
                int result = getrlimit(MAC_RLIMIT_NOFILE, buffer);
                if (result == 0) {
                    return buffer.get(JAVA_LONG, RLIMIT.byteOffset(RLIM_MAX));
                }
                return 0L;
            }, LOG, DEBUG, "Failed to query hard open file limit", 0L);
        }
        return -1L; // not supported
    }

    @Override
    public boolean updateAttributes() {
        long now = System.currentTimeMillis();
        int pid = getProcessID();
        boolean updated = callInArenaBooleanOrDefault(arena -> {
            MemorySegment taskAllInfo = arena.allocate(PROC_TASK_ALL_INFO);
            int infoResult = proc_pidinfo(pid, PROC_PIDTASKALLINFO, 0L, taskAllInfo,
                    (int) PROC_TASK_ALL_INFO.byteSize());
            if (infoResult <= 0) {
                return false;
            }
            MemorySegment ptinfo = taskAllInfo.asSlice(PROC_TASK_ALL_INFO.byteOffset(PTINFO),
                    PROC_TASK_INFO.byteSize());
            if (ptinfo.get(JAVA_INT, PROC_TASK_INFO.byteOffset(PTI_THREADNUM)) < 1) {
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
                this.name = readFixedWidthString(pbsd, PROC_BSD_INFO.byteOffset(PBI_COMM), MAXCOMLEN);
            }

            // Get Process state based on status
            int status = pbsd.get(JAVA_INT, PROC_BSD_INFO.byteOffset(PBI_STATUS));
            this.state = stateFromStatus(status);

            // User and group info
            this.parentProcessID = pbsd.get(JAVA_INT, PROC_BSD_INFO.byteOffset(PBI_PPID));
            int uid = pbsd.get(JAVA_INT, PROC_BSD_INFO.byteOffset(PBI_UID));
            this.userID = Integer.toString(uid);
            MemorySegment pwuid = getpwuid(uid);
            if (pwuid != null) {
                MemorySegment passwdStruct = ForeignFunctions.getStructFromNativePointer(pwuid, PASSWD, arena);
                MemorySegment nameAddress = passwdStruct.get(ADDRESS, PASSWD.byteOffset(groupElement("pw_name")));
                this.user = ForeignFunctions.getStringFromNativePointer(nameAddress, arena);
            } else {
                this.user = this.userID;
            }

            int gid = pbsd.get(JAVA_INT, PROC_BSD_INFO.byteOffset(PBI_GID));
            this.groupID = Integer.toString(gid);
            MemorySegment grgid = getgrgid(gid);
            if (grgid != null) {
                MemorySegment groupStruct = ForeignFunctions.getStructFromNativePointer(grgid, GROUP, arena);
                MemorySegment nameAddress = groupStruct.get(ADDRESS, GROUP.byteOffset(groupElement("gr_name")));
                this.group = ForeignFunctions.getStringFromNativePointer(nameAddress, arena);
            } else {
                this.group = this.groupID;
            }

            // Process metrics
            this.threadCount = ptinfo.get(JAVA_INT, PROC_TASK_INFO.byteOffset(PTI_THREADNUM));
            this.priority = ptinfo.get(JAVA_INT, PROC_TASK_INFO.byteOffset(PTI_PRIORITY));
            this.virtualSize = ptinfo.get(JAVA_LONG, PROC_TASK_INFO.byteOffset(PTI_VIRTUAL_SIZE));
            this.residentSetSize = ptinfo.get(JAVA_LONG, PROC_TASK_INFO.byteOffset(PTI_RESIDENT_SIZE));
            // Default/fallback: RSS. Will be overwritten by phys_footprint when available.
            this.memoryFootprint = this.residentSetSize;
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
            // getrusage(RUSAGE_SELF) aggregates across all threads for current process
            if (getProcessID() == this.os.getProcessId()) {
                try {
                    MemorySegment rusageSegment = arena.allocate(MacSystemFunctions.RUSAGE_SIZE);
                    if (0 == MacSystemFunctions.getrusage(MacSystemFunctions.RUSAGE_SELF, rusageSegment)) {
                        this.voluntaryContextSwitches = rusageSegment.get(JAVA_LONG,
                                MacSystemFunctions.RUSAGE_NVCSW_OFFSET);
                        this.involuntaryContextSwitches = rusageSegment.get(JAVA_LONG,
                                MacSystemFunctions.RUSAGE_NIVCSW_OFFSET);
                        this.contextSwitches = this.voluntaryContextSwitches + this.involuntaryContextSwitches;
                    } else {
                        this.voluntaryContextSwitches = 0L;
                        this.involuntaryContextSwitches = 0L;
                    }
                } catch (Throwable t) {
                    LOG.debug("FFM getrusage failed: {}", t.toString());
                    this.voluntaryContextSwitches = 0L;
                    this.involuntaryContextSwitches = 0L;
                }
            }

            // Get rusage info for newer OS versions
            if (this.majorVersion > 10 || (this.majorVersion == 10 && this.minorVersion >= 9)) {
                MemorySegment rusage = arena.allocate(RUSAGEINFOV2);
                if (0 == proc_pid_rusage(pid, RUSAGE_INFO_V2, rusage)) {
                    this.bytesRead = rusage.get(JAVA_LONG, RUSAGEINFOV2.byteOffset(RI_DISKIO_BYTESREAD));
                    this.bytesWritten = rusage.get(JAVA_LONG, RUSAGEINFOV2.byteOffset(RI_DISKIO_BYTESWRITTEN));
                    this.memoryFootprint = rusage.get(JAVA_LONG, RUSAGEINFOV2.byteOffset(RI_PHYS_FOOTPRINT));
                }
            }

            // Get working directory info
            MemorySegment vnodeInfo = arena.allocate(VNODE_PATH_INFO);
            if (proc_pidinfo(pid, PROC_PIDVNODEPATHINFO, 0L, vnodeInfo, (int) VNODE_PATH_INFO.byteSize()) > 0) {
                // Get the current directory path using nested path elements
                this.currentWorkingDirectory = vnodeInfo.asSlice(VNODE_PATH_INFO.byteOffset(PVI_CDIR))
                        .asSlice(VNODE_INFO_PATH.byteOffset(VIP_PATH), MAXPATHLEN).getString(0);
            }
            return true;
        }, LOG, TRACE, "Failed to update process attributes", false);
        if (!updated) {
            this.state = INVALID;
        }
        return updated;
    }
}
