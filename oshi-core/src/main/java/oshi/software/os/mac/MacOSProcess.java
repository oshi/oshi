/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import static oshi.util.Memoizer.memoize;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory; // NOSONAR squid:S1191
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.mac.SystemB.Group;
import com.sun.jna.platform.mac.SystemB.Passwd;
import com.sun.jna.platform.mac.SystemB.ProcTaskAllInfo;
import com.sun.jna.platform.mac.SystemB.RUsageInfoV2;
import com.sun.jna.platform.mac.SystemB.VnodePathInfo;
import com.sun.jna.ptr.IntByReference;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSProcess;
import oshi.util.platform.mac.SysctlUtil;

@ThreadSafe
public class MacOSProcess extends AbstractOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(MacOSProcess.class);

    // 64-bit flag
    private static final int P_LP64 = 0x4;
    /*
     * OS X States:
     */
    private static final int SSLEEP = 1; // sleeping on high priority
    private static final int SWAIT = 2; // sleeping on low priority
    private static final int SRUN = 3; // running
    private static final int SIDL = 4; // intermediate state in process creation
    private static final int SZOMB = 5; // intermediate state in process termination
    private static final int SSTOP = 6; // process being traced

    private int minorVersion;

    private Supplier<String> commandLine = memoize(this::queryCommandLine);

    private String name = "";
    private String path = "";
    private String currentWorkingDirectory;
    private String user;
    private String userID;
    private String group;
    private String groupID;
    private State state = State.INVALID;
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

    public MacOSProcess(int pid, int minor) {
        super(pid);
        this.minorVersion = minor;
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
        // Get command line via sysctl
        int[] mib = new int[3];
        mib[0] = 1; // CTL_KERN
        mib[1] = 49; // KERN_PROCARGS2
        mib[2] = getProcessID();
        // Allocate memory for arguments
        int argmax = SysctlUtil.sysctl("kern.argmax", 0);
        Pointer procargs = new Memory(argmax);
        IntByReference size = new IntByReference(argmax);
        // Fetch arguments
        if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, procargs, size, null, 0)) {
            LOG.warn(
                    "Failed syctl call for process arguments (kern.procargs2), process {} may not exist. Error code: {}",
                    getProcessID(), Native.getLastError());
            return "";
        }
        // Procargs contains an int representing total # of args, followed by a
        // null-terminated execpath string and then the arguments, each
        // null-terminated (possible multiple consecutive nulls),
        // The execpath string is also the first arg.
        int nargs = procargs.getInt(0);
        // Sanity check
        if (nargs < 0 || nargs > 1024) {
            LOG.error("Nonsensical number of process arguments for pid {}: {}", getProcessID(), nargs);
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
    public int getBitness() {
        return this.bitness;
    }

    @Override
    public long getAffinityMask() {
        // macOS doesn't do affinity. Return a bitmask of the current processors.
        int logicalProcessorCount = SysctlUtil.sysctl("hw.logicalcpu", 1);
        return logicalProcessorCount < 64 ? (1L << logicalProcessorCount) - 1 : -1L;
    }

    @Override
    public boolean updateAttributes() {
        long now = System.currentTimeMillis();
        ProcTaskAllInfo taskAllInfo = new ProcTaskAllInfo();
        if (0 > SystemB.INSTANCE.proc_pidinfo(getProcessID(), SystemB.PROC_PIDTASKALLINFO, 0, taskAllInfo,
                taskAllInfo.size()) || taskAllInfo.ptinfo.pti_threadnum < 1) {
            this.state = State.INVALID;
            return false;
        }
        Pointer buf = new Memory(SystemB.PROC_PIDPATHINFO_MAXSIZE);
        if (0 < SystemB.INSTANCE.proc_pidpath(getProcessID(), buf, SystemB.PROC_PIDPATHINFO_MAXSIZE)) {
            this.path = buf.getString(0).trim();
            // Overwrite name with last part of path
            String[] pathSplit = this.path.split("/");
            if (pathSplit.length > 0) {
                this.name = pathSplit[pathSplit.length - 1];
            }
        }
        if (this.name.isEmpty()) {
            // pbi_comm contains first 16 characters of name
            // null terminated
            for (int t = 0; t < taskAllInfo.pbsd.pbi_comm.length; t++) {
                if (taskAllInfo.pbsd.pbi_comm[t] == 0) {
                    this.name = new String(taskAllInfo.pbsd.pbi_comm, 0, t, StandardCharsets.UTF_8);
                    break;
                }
            }
        }

        switch (taskAllInfo.pbsd.pbi_status) {
        case SSLEEP:
            this.state = State.SLEEPING;
            break;
        case SWAIT:
            this.state = State.WAITING;
            break;
        case SRUN:
            this.state = State.RUNNING;
            break;
        case SIDL:
            this.state = State.NEW;
            break;
        case SZOMB:
            this.state = State.ZOMBIE;
            break;
        case SSTOP:
            this.state = State.STOPPED;
            break;
        default:
            this.state = State.OTHER;
            break;
        }
        this.parentProcessID = taskAllInfo.pbsd.pbi_ppid;
        this.userID = Integer.toString(taskAllInfo.pbsd.pbi_uid);
        Passwd pwuid = SystemB.INSTANCE.getpwuid(taskAllInfo.pbsd.pbi_uid);
        if (pwuid != null) {
            this.user = pwuid.pw_name;
        }
        this.groupID = Integer.toString(taskAllInfo.pbsd.pbi_gid);
        Group grgid = SystemB.INSTANCE.getgrgid(taskAllInfo.pbsd.pbi_gid);
        if (grgid != null) {
            this.group = grgid.gr_name;
        }
        this.threadCount = taskAllInfo.ptinfo.pti_threadnum;
        this.priority = taskAllInfo.ptinfo.pti_priority;
        this.virtualSize = taskAllInfo.ptinfo.pti_virtual_size;
        this.residentSetSize = taskAllInfo.ptinfo.pti_resident_size;
        this.kernelTime = taskAllInfo.ptinfo.pti_total_system / 1_000_000L;
        this.userTime = taskAllInfo.ptinfo.pti_total_user / 1_000_000L;
        this.startTime = taskAllInfo.pbsd.pbi_start_tvsec * 1000L + taskAllInfo.pbsd.pbi_start_tvusec / 1000L;
        this.upTime = now - this.startTime;
        this.openFiles = taskAllInfo.pbsd.pbi_nfiles;
        this.bitness = (taskAllInfo.pbsd.pbi_flags & P_LP64) == 0 ? 32 : 64;
        if (this.minorVersion >= 9) {
            RUsageInfoV2 rUsageInfoV2 = new RUsageInfoV2();
            if (0 == SystemB.INSTANCE.proc_pid_rusage(getProcessID(), SystemB.RUSAGE_INFO_V2, rUsageInfoV2)) {
                this.bytesRead = rUsageInfoV2.ri_diskio_bytesread;
                this.bytesWritten = rUsageInfoV2.ri_diskio_byteswritten;
            }
        }
        VnodePathInfo vpi = new VnodePathInfo();
        if (0 < SystemB.INSTANCE.proc_pidinfo(getProcessID(), SystemB.PROC_PIDVNODEPATHINFO, 0, vpi, vpi.size())) {
            int len = 0;
            for (byte b : vpi.pvi_cdir.vip_path) {
                if (b == 0) {
                    break;
                }
                len++;
            }
            this.currentWorkingDirectory = new String(vpi.pvi_cdir.vip_path, 0, len, StandardCharsets.US_ASCII);
        }
        return true;
    }
}
