/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.unix.solaris;

import java.util.ArrayList;
import java.util.List;

import oshi.jna.platform.linux.Libc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.OSProcess;
import oshi.util.ExecutingCommand;
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem() {
        // TODO
        return new SolarisFileSystem();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess[] getProcesses(int limit, ProcessSort sort) {
        ArrayList<String> procList = ExecutingCommand.runNative("ps -eo s,pid,ppid,nlwp,pri,vsz,rss,etime,time,comm");
        if (procList.isEmpty() || procList.size() < 2) {
            return new OSProcess[0];
        }
        // remove header row
        procList.remove(0);
        // Fill list
        List<OSProcess> procs = new ArrayList<>();
        for (String proc : procList) {
            String[] split = proc.trim().split("\\s+");
            // Elements should match ps command order
            if (split.length < 10) {
                continue;
            }
            String path = split[9];
            long now = System.currentTimeMillis();
            procs.add(new SolarisProcess(path.substring(path.lastIndexOf('/') + 1), // name
                    path, // path
                    split[0].charAt(0), // state, one of OSRTWZ
                    ParseUtil.parseIntOrDefault(split[1], 0), // pid
                    ParseUtil.parseIntOrDefault(split[2], 0), // ppid
                    ParseUtil.parseIntOrDefault(split[3], 0), // thread count
                    ParseUtil.parseIntOrDefault(split[4], 0), // priority
                    ParseUtil.parseLongOrDefault(split[5], 0L), // VSZ in kb
                    ParseUtil.parseLongOrDefault(split[6], 0L), // RSS in kb
                    // DHMS values are in seconds
                    ParseUtil.parseDHMSOrDefault(split[7], 0L), // elapsed time
                    ParseUtil.parseDHMSOrDefault(split[8], 0L), // process time
                    now //
            ));
        }
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[sorted.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess getProcess(int pid) {
        ArrayList<String> procList = ExecutingCommand
                .runNative("ps -o s,pid,ppid,nlwp,pri,vsz,rss,etime,time,comm -p " + pid);
        if (procList.isEmpty() || procList.size() < 2) {
            return null;
        }
        // remove header row
        String[] split = procList.get(1).trim().split("\\s+");
        // Elements should match ps command order
        if (split.length < 10) {
            return null;
        }
        String path = split[9];
        return new SolarisProcess(path.substring(path.lastIndexOf('/') + 1), // name
                path, // path
                split[0].charAt(0), // state, one of OSRTWZ
                pid, // also split[1] but we already have
                ParseUtil.parseIntOrDefault(split[2], 0), // ppid
                ParseUtil.parseIntOrDefault(split[3], 0), // thread count
                ParseUtil.parseIntOrDefault(split[4], 0), // priority
                ParseUtil.parseLongOrDefault(split[5], 0L), // VSZ in kb
                ParseUtil.parseLongOrDefault(split[6], 0L), // RSS in kb
                // The below values are in seconds
                ParseUtil.parseDHMSOrDefault(split[7], 0L), // elapsed time
                ParseUtil.parseDHMSOrDefault(split[8], 0L), // process time
                System.currentTimeMillis() //
        );
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
        ArrayList<String> threadList = ExecutingCommand.runNative("ps -eLo pid");
        if (threadList != null) {
            // Subtract 1 for header
            return threadList.size() - 1;
        }
        return getProcessCount();
    }
}
