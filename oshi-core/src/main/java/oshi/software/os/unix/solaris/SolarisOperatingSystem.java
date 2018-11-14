/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
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
import oshi.util.MapUtil;
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
    public OSProcess[] getProcesses(int limit, ProcessSort sort) {
        List<OSProcess> procs = getProcessListFromPS(
                "ps -eo s,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etime,time,comm,args", -1);
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[sorted.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess getProcess(int pid) {
        List<OSProcess> procs = getProcessListFromPS(
                "ps -o s,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etime,time,comm,args -p ", pid);
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
                "ps -eo s,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etime,time,comm,args --ppid", parentPid);
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[sorted.size()]);
    }

    private List<OSProcess> getProcessListFromPS(String psCommand, int pid) {
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
            sproc.setCurrentWorkingDirectory(MapUtil.getOrDefault(cwdMap, sproc.getProcessID(), ""));
            // bytes read/written not easily available

            // gets the open files count -- only do for single-PID requests
            if (pid >= 0) {
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
