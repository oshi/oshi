/**
 * MIT License
 *
 * Copyright (c) 2010-2019 The OSHI project team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.os.mac;

import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory; // NOSONAR squid:s1191
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.mac.SystemB.Group;
import com.sun.jna.platform.mac.SystemB.Passwd;
import com.sun.jna.platform.mac.SystemB.ProcTaskAllInfo;
import com.sun.jna.platform.mac.SystemB.ProcTaskInfo;
import com.sun.jna.platform.mac.SystemB.RUsageInfoV2;
import com.sun.jna.platform.mac.SystemB.Timeval;
import com.sun.jna.platform.mac.SystemB.VnodePathInfo;
import com.sun.jna.ptr.IntByReference;

import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.SysctlUtil;

/**
 * <p>
 * MacOperatingSystem class.
 * </p>
 */
public class MacOperatingSystem extends AbstractOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(MacOperatingSystem.class);

    private int maxProc = 1024;

    private final String osXVersion;
    private final int major;
    private final int minor;

    // 64-bit flag
    private static final int P_LP64 = 0x4;
    /*
     * OS X States:
     */
    private static final int SSLEEP = 1; // sleeping on high priority
    private static final int SWAIT = 2; // sleeping on low priority
    private static final int SRUN = 3; // running
    private static final int SIDL = 4; // intermediate state in process creation
    private static final int SZOMB = 5; // intermediate state in process
                                        // termination
    private static final int SSTOP = 6; // process being traced

    private static final long BOOTTIME;
    static {
        Timeval tv = new Timeval();
        if (!SysctlUtil.sysctl("kern.boottime", tv) || tv.tv_sec.longValue() == 0L) {
            // Usually this works. If it doesn't, fall back to text parsing.
            // Boot time will be the first consecutive string of digits.
            BOOTTIME = ParseUtil.parseLongOrDefault(
                    ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                    System.currentTimeMillis() / 1000);
        } else {
            // tv now points to a 64-bit timeval structure for boot time.
            // First 4 bytes are seconds, second 4 bytes are microseconds
            // (we ignore)
            BOOTTIME = tv.tv_sec.longValue();
        }
    }

    /**
     * <p>
     * Constructor for MacOperatingSystem.
     * </p>
     */
    @SuppressWarnings("deprecation")
    public MacOperatingSystem() {
        this.version = new MacOSVersionInfoEx();
        this.osXVersion = System.getProperty("os.version");
        this.major = ParseUtil.getFirstIntValue(this.osXVersion);
        this.minor = ParseUtil.getNthIntValue(this.osXVersion, 2);
        // Set max processes
        this.maxProc = SysctlUtil.sysctl("kern.maxproc", 0x1000);
    }

    @Override
    public String queryManufacturer() {
        return "Apple";
    }

    @Override
    public FamilyVersionInfo queryFamilyVersionInfo() {
        String family = this.major == 10 && this.minor >= 12 ? "macOS" : System.getProperty("os.name");
        String codeName = parseCodeName();
        String buildNumber = SysctlUtil.sysctl("kern.osversion", "");
        return new FamilyVersionInfo(family, new OSVersionInfo(this.osXVersion, codeName, buildNumber));
    }

    private String parseCodeName() {
        if (this.major == 10) {
            switch (this.minor) {
            // MacOS
            case 15:
                return "Catalina";
            case 14:
                return "Mojave";
            case 13:
                return "High Sierra";
            case 12:
                return "Sierra";
            // OS X
            case 11:
                return "El Capitan";
            case 10:
                return "Yosemite";
            case 9:
                return "Mavericks";
            case 8:
                return "Mountain Lion";
            case 7:
                return "Lion";
            case 6:
                return "Snow Leopard";
            case 5:
                return "Leopard";
            case 4:
                return "Tiger";
            case 3:
                return "Panther";
            case 2:
                return "Jaguar";
            case 1:
                return "Puma";
            case 0:
                return "Cheetah";
            default:
            }
        }
        LOG.warn("Unable to parse version {}.{} to a codename.", this.major, this.minor);
        return "";
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness == 64 || (this.major == 10 && this.minor > 6)) {
            return 64;
        }
        return ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("getconf LONG_BIT"), 32);
    }

    @Override
    protected boolean queryElevated() {
        return System.getenv("SUDO_COMMAND") != null;
    }

    @Override
    public FileSystem getFileSystem() {
        return new MacFileSystem();
    }

    @Override
    public OSProcess[] getProcesses(int limit, ProcessSort sort, boolean slowFields) {
        List<OSProcess> procs = new ArrayList<>();
        int[] pids = new int[this.maxProc];
        int numberOfProcesses = SystemB.INSTANCE.proc_listpids(SystemB.PROC_ALL_PIDS, 0, pids,
                pids.length * SystemB.INT_SIZE) / SystemB.INT_SIZE;
        for (int i = 0; i < numberOfProcesses; i++) {
            // Handle off-by-one bug in proc_listpids where the size returned
            // is: SystemB.INT_SIZE * (pids + 1)
            if (pids[i] == 0) {
                continue;
            }

            OSProcess proc = getProcess(pids[i], slowFields);
            if (proc != null) {
                procs.add(proc);
            }
        }
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[0]);
    }

    @Override
    public OSProcess getProcess(int pid, boolean slowFields) {
        ProcTaskAllInfo taskAllInfo = new ProcTaskAllInfo();
        if (0 > SystemB.INSTANCE.proc_pidinfo(pid, SystemB.PROC_PIDTASKALLINFO, 0, taskAllInfo, taskAllInfo.size())) {
            return null;
        }
        String name = null;
        String path = "";
        Pointer buf = new Memory(SystemB.PROC_PIDPATHINFO_MAXSIZE);
        if (0 < SystemB.INSTANCE.proc_pidpath(pid, buf, SystemB.PROC_PIDPATHINFO_MAXSIZE)) {
            path = buf.getString(0).trim();
            // Overwrite name with last part of path
            String[] pathSplit = path.split("/");
            if (pathSplit.length > 0) {
                name = pathSplit[pathSplit.length - 1];
            }
        }
        // If process is gone, return null
        if (taskAllInfo.ptinfo.pti_threadnum < 1) {
            return null;
        }
        if (name == null) {
            // pbi_comm contains first 16 characters of name
            // null terminated
            for (int t = 0; t < taskAllInfo.pbsd.pbi_comm.length; t++) {
                if (taskAllInfo.pbsd.pbi_comm[t] == 0) {
                    name = new String(taskAllInfo.pbsd.pbi_comm, 0, t, StandardCharsets.UTF_8);
                    break;
                }
            }
        }
        long bytesRead = 0;
        long bytesWritten = 0;
        if (this.minor >= 9) {
            RUsageInfoV2 rUsageInfoV2 = new RUsageInfoV2();
            if (0 == SystemB.INSTANCE.proc_pid_rusage(pid, SystemB.RUSAGE_INFO_V2, rUsageInfoV2)) {
                bytesRead = rUsageInfoV2.ri_diskio_bytesread;
                bytesWritten = rUsageInfoV2.ri_diskio_byteswritten;
            }
        }
        long now = System.currentTimeMillis();
        OSProcess proc = new OSProcess(this);
        proc.setName(name);
        proc.setPath(path);
        switch (taskAllInfo.pbsd.pbi_status) {
        case SSLEEP:
            proc.setState(OSProcess.State.SLEEPING);
            break;
        case SWAIT:
            proc.setState(OSProcess.State.WAITING);
            break;
        case SRUN:
            proc.setState(OSProcess.State.RUNNING);
            break;
        case SIDL:
            proc.setState(OSProcess.State.NEW);
            break;
        case SZOMB:
            proc.setState(OSProcess.State.ZOMBIE);
            break;
        case SSTOP:
            proc.setState(OSProcess.State.STOPPED);
            break;
        default:
            proc.setState(OSProcess.State.OTHER);
            break;
        }
        proc.setProcessID(pid);
        proc.setParentProcessID(taskAllInfo.pbsd.pbi_ppid);
        proc.setUserID(Integer.toString(taskAllInfo.pbsd.pbi_uid));
        Passwd user = SystemB.INSTANCE.getpwuid(taskAllInfo.pbsd.pbi_uid);
        proc.setUser(user == null ? proc.getUserID() : user.pw_name);
        proc.setGroupID(Integer.toString(taskAllInfo.pbsd.pbi_gid));
        Group group = SystemB.INSTANCE.getgrgid(taskAllInfo.pbsd.pbi_gid);
        proc.setGroup(group == null ? proc.getGroupID() : group.gr_name);
        proc.setThreadCount(taskAllInfo.ptinfo.pti_threadnum);
        proc.setPriority(taskAllInfo.ptinfo.pti_priority);
        proc.setVirtualSize(taskAllInfo.ptinfo.pti_virtual_size);
        proc.setResidentSetSize(taskAllInfo.ptinfo.pti_resident_size);
        proc.setKernelTime(taskAllInfo.ptinfo.pti_total_system / 1000000L);
        proc.setUserTime(taskAllInfo.ptinfo.pti_total_user / 1000000L);
        proc.setStartTime(taskAllInfo.pbsd.pbi_start_tvsec * 1000L + taskAllInfo.pbsd.pbi_start_tvusec / 1000L);
        proc.setUpTime(now - proc.getStartTime());
        proc.setBytesRead(bytesRead);
        proc.setBytesWritten(bytesWritten);
        proc.setCommandLine(getCommandLine(pid));
        proc.setOpenFiles(taskAllInfo.pbsd.pbi_nfiles);
        proc.setBitness((taskAllInfo.pbsd.pbi_flags & P_LP64) == 0 ? 32 : 64);

        VnodePathInfo vpi = new VnodePathInfo();
        if (0 < SystemB.INSTANCE.proc_pidinfo(pid, SystemB.PROC_PIDVNODEPATHINFO, 0, vpi, vpi.size())) {
            int len = 0;
            for (byte b : vpi.pvi_cdir.vip_path) {
                if (b == 0) {
                    break;
                }
                len++;
            }
            proc.setCurrentWorkingDirectory(new String(vpi.pvi_cdir.vip_path, 0, len, StandardCharsets.US_ASCII));
        }
        return proc;
    }

    @Override
    public OSProcess[] getChildProcesses(int parentPid, int limit, ProcessSort sort) {
        List<OSProcess> procs = new ArrayList<>();
        int[] pids = new int[this.maxProc];
        int numberOfProcesses = SystemB.INSTANCE.proc_listpids(SystemB.PROC_ALL_PIDS, 0, pids,
                pids.length * SystemB.INT_SIZE) / SystemB.INT_SIZE;
        for (int i = 0; i < numberOfProcesses; i++) {
            // Handle off-by-one bug in proc_listpids where the size returned
            // is: SystemB.INT_SIZE * (pids + 1)
            if (pids[i] == 0) {
                continue;
            }
            if (parentPid == getParentProcessPid(pids[i])) {
                OSProcess proc = getProcess(pids[i], true);
                if (proc != null) {
                    procs.add(proc);
                }
            }
        }
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[0]);
    }

    private int getParentProcessPid(int pid) {
        ProcTaskAllInfo taskAllInfo = new ProcTaskAllInfo();
        if (0 > SystemB.INSTANCE.proc_pidinfo(pid, SystemB.PROC_PIDTASKALLINFO, 0, taskAllInfo, taskAllInfo.size())) {
            return 0;
        }
        return taskAllInfo.pbsd.pbi_ppid;
    }

    private String getCommandLine(int pid) {
        // Get command line via sysctl
        int[] mib = new int[3];
        mib[0] = 1; // CTL_KERN
        mib[1] = 49; // KERN_PROCARGS2
        mib[2] = pid;
        // Allocate memory for arguments
        int argmax = SysctlUtil.sysctl("kern.argmax", 0);
        Pointer procargs = new Memory(argmax);
        IntByReference size = new IntByReference(argmax);
        // Fetch arguments
        if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, procargs, size, null, 0)) {
            LOG.warn(
                    "Failed syctl call for process arguments (kern.procargs2), process {} may not exist. Error code: {}",
                    pid, Native.getLastError());
            return "";
        }
        // Procargs contains an int representing total # of args, followed by a
        // null-terminated execpath string and then the arguments, each
        // null-terminated (possible multiple consecutive nulls),
        // The execpath string is also the first arg.
        int nargs = procargs.getInt(0);
        // Sanity check
        if (nargs < 0 || nargs > 1024) {
            LOG.error("Nonsensical number of process arguments for pid {}: {}", pid, nargs);
            return "";
        }
        List<String> args = new ArrayList<>(nargs);
        // Skip first int (containing value of nargs)
        long offset = SystemB.INT_SIZE;
        // Skip exec_command
        offset += procargs.getString(offset).length();
        // Iterate character by character using offset
        // Build each arg and add to list
        while (nargs-- > 0 && offset < size.getValue()) {
            // Advance through additional nulls
            while (procargs.getByte(offset) == 0) {
                if (++offset >= size.getValue()) {
                    break;
                }
            }
            // Grab a string. This should go until the null terminator
            String arg = procargs.getString(offset);
            args.add(arg);
            // Advance offset to next null
            offset += arg.length();
        }
        // Return args null-delimited
        return String.join("\0", args);
    }

    @Override
    public long getProcessAffinityMask(int processId) {
        // macOS doesn't do affinity. Return a bitmask of the current processors.
        int logicalProcessorCount = SysctlUtil.sysctl("hw.logicalcpu", 1);
        return logicalProcessorCount < 64 ? (1L << logicalProcessorCount) - 1 : -1L;
    }

    @Override
    public int getProcessId() {
        return SystemB.INSTANCE.getpid();
    }

    @Override
    public int getProcessCount() {
        return SystemB.INSTANCE.proc_listpids(SystemB.PROC_ALL_PIDS, 0, null, 0) / SystemB.INT_SIZE;
    }

    @Override
    public int getThreadCount() {
        // Get current pids, then slightly pad in case new process starts while
        // allocating array space
        int[] pids = new int[getProcessCount() + 10];
        int numberOfProcesses = SystemB.INSTANCE.proc_listpids(SystemB.PROC_ALL_PIDS, 0, pids, pids.length)
                / SystemB.INT_SIZE;
        int numberOfThreads = 0;
        ProcTaskInfo taskInfo = new ProcTaskInfo();
        for (int i = 0; i < numberOfProcesses; i++) {
            SystemB.INSTANCE.proc_pidinfo(pids[i], SystemB.PROC_PIDTASKINFO, 0, taskInfo, taskInfo.size());
            numberOfThreads += taskInfo.pti_threadnum;
        }
        return numberOfThreads;
    }

    @Override
    public long getSystemUptime() {
        return System.currentTimeMillis() / 1000 - BOOTTIME;
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new MacNetworkParams();
    }

    @Override
    public OSService[] getServices() {
        // Get running services
        List<OSService> services = new ArrayList<>();
        Set<String> running = new HashSet<>();
        for (OSProcess p : getChildProcesses(1, 0, ProcessSort.PID)) {
            OSService s = new OSService(p.getName(), p.getProcessID(), RUNNING);
            services.add(s);
            running.add(p.getName());
        }
        // Get Directories for stopped services
        ArrayList<File> files = new ArrayList<>();
        File dir = new File("/System/Library/LaunchAgents");
        if (dir.exists() && dir.isDirectory()) {
            files.addAll(Arrays.asList(dir.listFiles((f, name) -> name.toLowerCase().endsWith(".plist"))));
        } else {
            LOG.error("Directory: /System/Library/LaunchAgents does not exist");
        }
        dir = new File("/System/Library/LaunchDaemons");
        if (dir.exists() && dir.isDirectory()) {
            files.addAll(Arrays.asList(dir.listFiles((f, name) -> name.toLowerCase().endsWith(".plist"))));
        } else {
            LOG.error("Directory: /System/Library/LaunchDaemons does not exist");
        }
        for (File f : files) {
            // remove .plist extension
            String name = f.getName().substring(0, f.getName().length() - 6);
            int index = name.lastIndexOf('.');
            String shortName = (index < 0 || index > name.length() - 2) ? name : name.substring(index + 1);
            if (!running.contains(name) && !running.contains(shortName)) {
                OSService s = new OSService(name, 0, STOPPED);
                services.add(s);
            }
        }
        return services.toArray(new OSService[0]);
    }
}
