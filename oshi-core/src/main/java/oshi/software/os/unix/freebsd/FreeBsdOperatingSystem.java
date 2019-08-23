/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.os.unix.freebsd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sun.jna.Memory; //NOSONAR squid:S1191
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import oshi.jna.platform.unix.CLibrary.Timeval;
import oshi.jna.platform.unix.freebsd.FreeBsdLibc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.util.ExecutingCommand;
import oshi.util.LsofUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * <p>
 * FreeBsdOperatingSystem class.
 * </p>
 */
public class FreeBsdOperatingSystem extends AbstractOperatingSystem {
    private static final long serialVersionUID = 1L;

    private static final long BOOTTIME;
    static {
        Timeval tv = new Timeval();
        if (!BsdSysctlUtil.sysctl("kern.boottime", tv) || tv.tv_sec == 0) {
            // Usually this works. If it doesn't, fall back to text parsing.
            // Boot time will be the first consecutive string of digits.
            BOOTTIME = ParseUtil.parseLongOrDefault(
                    ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                    System.currentTimeMillis() / 1000);
        } else {
            // tv now points to a 128-bit timeval structure for boot time.
            // First 8 bytes are seconds, second 8 bytes are microseconds
            // (we ignore)
            BOOTTIME = tv.tv_sec;
        }
    }

    /**
     * <p>
     * Constructor for FreeBsdOperatingSystem.
     * </p>
     */
    public FreeBsdOperatingSystem() {
        this.manufacturer = "Unix/BSD";
        this.family = BsdSysctlUtil.sysctl("kern.ostype", "FreeBSD");
        this.version = new FreeBsdOSVersionInfoEx();
        initBitness();
    }

    private void initBitness() {
        if (this.bitness < 64 && ExecutingCommand.getFirstAnswer("uname -m").indexOf("64") != -1) {
            this.bitness = 64;
        }
    }

    /** {@inheritDoc} */
    @Override
    public FileSystem getFileSystem() {
        return new FreeBsdFileSystem();
    }

    /** {@inheritDoc} */
    @Override
    public OSProcess[] getProcesses(int limit, ProcessSort sort, boolean slowFields) {
        List<OSProcess> procs = getProcessListFromPS(
                "ps -awwxo state,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etimes,systime,time,comm,args", -1,
                slowFields);
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[0]);
    }

    /** {@inheritDoc} */
    @Override
    public OSProcess getProcess(int pid) {
        return getProcess(pid, true);
    }

    private OSProcess getProcess(int pid, boolean slowFields) {
        List<OSProcess> procs = getProcessListFromPS(
                "ps -awwxo state,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etimes,systime,time,comm,args -p ", pid,
                slowFields);
        if (procs.isEmpty()) {
            return null;
        }
        return procs.get(0);
    }

    /** {@inheritDoc} */
    @Override
    public OSProcess[] getChildProcesses(int parentPid, int limit, ProcessSort sort) {
        List<OSProcess> procs = getProcessListFromPS(
                "ps -awwxo state,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etimes,systime,time,comm,args --ppid",
                parentPid, true);
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[0]);
    }

    private List<OSProcess> getProcessListFromPS(String psCommand, int pid, boolean slowFields) {
        Map<Integer, String> cwdMap = LsofUtil.getCwdMap(pid);
        List<OSProcess> procs = new ArrayList<>();
        List<String> procList = ExecutingCommand.runNative(psCommand + (pid < 0 ? "" : pid));
        if (procList.isEmpty() || procList.size() < 2) {
            return procs;
        }
        // remove header row
        procList.remove(0);
        // Fill list
        for (String proc : procList) {
            String[] split = ParseUtil.whitespaces.split(proc.trim(), 16);
            // Elements should match ps command order
            if (split.length < 16) {
                continue;
            }
            long now = System.currentTimeMillis();
            OSProcess fproc = new OSProcess(this);
            switch (split[0].charAt(0)) {
            case 'R':
                fproc.setState(OSProcess.State.RUNNING);
                break;
            case 'I':
            case 'S':
                fproc.setState(OSProcess.State.SLEEPING);
                break;
            case 'D':
            case 'L':
            case 'U':
                fproc.setState(OSProcess.State.WAITING);
                break;
            case 'Z':
                fproc.setState(OSProcess.State.ZOMBIE);
                break;
            case 'T':
                fproc.setState(OSProcess.State.STOPPED);
                break;
            default:
                fproc.setState(OSProcess.State.OTHER);
                break;
            }
            fproc.setProcessID(ParseUtil.parseIntOrDefault(split[1], 0));
            fproc.setParentProcessID(ParseUtil.parseIntOrDefault(split[2], 0));
            fproc.setUser(split[3]);
            fproc.setUserID(split[4]);
            fproc.setGroup(split[5]);
            fproc.setGroupID(split[6]);
            fproc.setThreadCount(ParseUtil.parseIntOrDefault(split[7], 0));
            fproc.setPriority(ParseUtil.parseIntOrDefault(split[8], 0));
            // These are in KB, multiply
            fproc.setVirtualSize(ParseUtil.parseLongOrDefault(split[9], 0) * 1024);
            fproc.setResidentSetSize(ParseUtil.parseLongOrDefault(split[10], 0) * 1024);
            // Avoid divide by zero for processes up less than a second
            long elapsedTime = ParseUtil.parseDHMSOrDefault(split[11], 0L);
            fproc.setUpTime(elapsedTime < 1L ? 1L : elapsedTime);
            fproc.setStartTime(now - fproc.getUpTime());
            fproc.setKernelTime(ParseUtil.parseDHMSOrDefault(split[12], 0L));
            fproc.setUserTime(ParseUtil.parseDHMSOrDefault(split[13], 0L) - fproc.getKernelTime());
            fproc.setPath(split[14]);
            fproc.setName(fproc.getPath().substring(fproc.getPath().lastIndexOf('/') + 1));
            fproc.setCommandLine(split[15]);
            fproc.setCurrentWorkingDirectory(cwdMap.getOrDefault(fproc.getProcessID(), ""));

            if (slowFields) {
                List<String> openFilesList = ExecutingCommand.runNative(String.format("lsof -p %d", pid));
                fproc.setOpenFiles(openFilesList.size() - 1L);

                // Get process abi vector
                int[] mib = new int[4];
                mib[0] = 1; // CTL_KERN
                mib[1] = 14; // KERN_PROC
                mib[2] = 9; // KERN_PROC_SV_NAME
                mib[3] = pid;
                // Allocate memory for arguments
                Pointer abi = new Memory(32);
                IntByReference size = new IntByReference(32);
                // Fetch abi vector
                if (0 == FreeBsdLibc.INSTANCE.sysctl(mib, mib.length, abi, size, null, 0)) {
                    String elf = abi.getString(0);
                    if (elf.contains("ELF32")) {
                        fproc.setBitness(32);
                    } else if (elf.contains("ELF64")) {
                        fproc.setBitness(64);
                    }
                }
            }
            procs.add(fproc);
        }
        return procs;
    }

    /** {@inheritDoc} */
    @Override
    public int getProcessId() {
        return FreeBsdLibc.INSTANCE.getpid();
    }

    /** {@inheritDoc} */
    @Override
    public int getProcessCount() {
        List<String> procList = ExecutingCommand.runNative("ps -axo pid");
        if (!procList.isEmpty()) {
            // Subtract 1 for header
            return procList.size() - 1;
        }
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int getThreadCount() {
        int threads = 0;
        for (String proc : ExecutingCommand.runNative("ps -axo nlwp")) {
            threads += ParseUtil.parseIntOrDefault(proc.trim(), 0);
        }
        return threads;
    }

    /** {@inheritDoc} */
    @Override
    public long getSystemUptime() {
        return System.currentTimeMillis() / 1000 - BOOTTIME;
    }

    /** {@inheritDoc} */
    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    /** {@inheritDoc} */
    @Override
    public NetworkParams getNetworkParams() {
        return new FreeBsdNetworkParams();
    }

}
