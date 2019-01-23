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
package oshi.software.os.unix.solaris;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oshi.jna.platform.linux.Libc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.util.ExecutingCommand;
import oshi.util.LsofUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcUtil;

/**
 * Linux is a family of free operating systems most commonly used on personal
 * computers.
 *
 * @author widdis[at]gmail[dot]com
 */
public class SolarisOperatingSystem extends AbstractOperatingSystem {
    private static final long serialVersionUID = 1L;

    public SolarisOperatingSystem() {
        this.manufacturer = "Oracle";
        this.family = "SunOS";
        this.version = new SolarisOSVersionInfoEx();
        initBitness();
    }

    private void initBitness() {
        if (this.bitness < 64) {
            this.bitness = ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("isainfo -b"), 32);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem() {
        return new SolarisFileSystem();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess[] getProcesses(int limit, ProcessSort sort, boolean slowFields) {
        List<OSProcess> procs = getProcessListFromPS(
                "ps -eo s,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etime,time,comm,args", -1, slowFields);
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess getProcess(int pid) {
        return getProcess(pid, true);
    }

    private OSProcess getProcess(int pid, boolean slowFields) {
        List<OSProcess> procs = getProcessListFromPS(
                "ps -o s,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etime,time,comm,args -p ", pid, slowFields);
        if (procs.isEmpty()) {
            return null;
        }
        return procs.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess[] getChildProcesses(int parentPid, int limit, ProcessSort sort) {
        List<OSProcess> procs = getProcessListFromPS(
                "ps -eo s,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etime,time,comm,args --ppid", parentPid, true);
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
            String[] split = ParseUtil.whitespaces.split(proc.trim(), 15);
            // Elements should match ps command order
            if (split.length < 15) {
                continue;
            }
            long now = System.currentTimeMillis();
            OSProcess sproc = new OSProcess();
            switch (split[0].charAt(0)) {
            case 'O':
                sproc.setState(OSProcess.State.RUNNING);
                break;
            case 'S':
                sproc.setState(OSProcess.State.SLEEPING);
                break;
            case 'R':
            case 'W':
                sproc.setState(OSProcess.State.WAITING);
                break;
            case 'Z':
                sproc.setState(OSProcess.State.ZOMBIE);
                break;
            case 'T':
                sproc.setState(OSProcess.State.STOPPED);
                break;
            default:
                sproc.setState(OSProcess.State.OTHER);
                break;
            }
            sproc.setProcessID(ParseUtil.parseIntOrDefault(split[1], 0));
            sproc.setParentProcessID(ParseUtil.parseIntOrDefault(split[2], 0));
            sproc.setUser(split[3]);
            sproc.setUserID(split[4]);
            sproc.setGroup(split[5]);
            sproc.setGroupID(split[6]);
            sproc.setThreadCount(ParseUtil.parseIntOrDefault(split[7], 0));
            sproc.setPriority(ParseUtil.parseIntOrDefault(split[8], 0));
            // These are in KB, multiply
            sproc.setVirtualSize(ParseUtil.parseLongOrDefault(split[9], 0) * 1024);
            sproc.setResidentSetSize(ParseUtil.parseLongOrDefault(split[10], 0) * 1024);
            // Avoid divide by zero for processes up less than a second
            long elapsedTime = ParseUtil.parseDHMSOrDefault(split[11], 0L);
            sproc.setUpTime(elapsedTime < 1L ? 1L : elapsedTime);
            sproc.setStartTime(now - sproc.getUpTime());
            sproc.setUserTime(ParseUtil.parseDHMSOrDefault(split[12], 0L));
            sproc.setPath(split[13]);
            sproc.setName(sproc.getPath().substring(sproc.getPath().lastIndexOf('/') + 1));
            sproc.setCommandLine(split[14]);
            sproc.setCurrentWorkingDirectory(cwdMap.getOrDefault(sproc.getProcessID(), ""));
            // bytes read/written not easily available

            // gets the open files count -- slow
            if (slowFields) {
                List<String> openFilesList = ExecutingCommand.runNative(String.format("lsof -p %d", pid));
                sproc.setOpenFiles(openFilesList.size() - 1L);
            }
            procs.add(sproc);
        }
        return procs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProcessId() {
        return Libc.INSTANCE.getpid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProcessCount() {
        return ProcUtil.getPidFiles().length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getThreadCount() {
        List<String> threadList = ExecutingCommand.runNative("ps -eLo pid");
        if (!threadList.isEmpty()) {
            // Subtract 1 for header
            return threadList.size() - 1;
        }
        return getProcessCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkParams getNetworkParams() {
        return new SolarisNetworkParams();
    }

}
