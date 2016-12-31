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
package oshi.software.os.unix.freebsd;

import java.util.ArrayList;
import java.util.List;

import oshi.jna.platform.linux.Libc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.OSProcess;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * Linux is a family of free operating systems most commonly used on personal
 * computers.
 *
 * @author widdis[at]gmail[dot]com
 */
public class FreeBsdOperatingSystem extends AbstractOperatingSystem {

    private static final long serialVersionUID = 1L;

    public FreeBsdOperatingSystem() {
        this.manufacturer = "Unix/BSD";
        this.family = BsdSysctlUtil.sysctl("kern.ostype", "FreeBSD");
        this.version = new FreeBsdOSVersionInfoEx();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem() {
        return new FreeBsdFileSystem();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess[] getProcesses(int limit, ProcessSort sort) {
        List<String> procList = ExecutingCommand
                .runNative("ps -awwxo state,pid,ppid,nlwp,pri,vsz,rss,etimes,systime,time,command");
        if (procList.isEmpty() || procList.size() < 2) {
            return new OSProcess[0];
        }
        // remove header row
        procList.remove(0);
        // Fill list
        List<OSProcess> procs = new ArrayList<>();
        for (String proc : procList) {
            String[] split = proc.trim().split("\\s+");
            // Elements should match ps command order. Args will make split
            // bigger than 11 but we ignore thems
            if (split.length < 11) {
                continue;
            }
            String path = split[10];
            long now = System.currentTimeMillis();
            procs.add(new FreeBsdProcess(path.substring(path.lastIndexOf('/') + 1), // name
                    path, // path
                    split[0].charAt(0), // state, one of DILRSTWZ
                    ParseUtil.parseIntOrDefault(split[1], 0), // pid
                    ParseUtil.parseIntOrDefault(split[2], 0), // ppid
                    ParseUtil.parseIntOrDefault(split[3], 0), // thread count
                    ParseUtil.parseIntOrDefault(split[4], 0), // priority
                    ParseUtil.parseLongOrDefault(split[5], 0L), // VSZ in kb
                    ParseUtil.parseLongOrDefault(split[6], 0L), // RSS in kb
                    ParseUtil.parseLongOrDefault(split[7], 0L), // elapsed secs
                    ParseUtil.parseDHMSOrDefault(split[8], 0L), // system ms
                    ParseUtil.parseDHMSOrDefault(split[9], 0L), // usr+sys ms
                    0L, 0L, // 'top -bm io' gives read/write counts, not bytes
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
        List<String> procList = ExecutingCommand
                .runNative("ps -awwxo state,pid,ppid,nlwp,pri,vsz,rss,etimes,systime,time,command -p " + pid);
        if (procList.isEmpty() || procList.size() < 2) {
            return null;
        }
        // remove header row
        String[] split = procList.get(1).trim().split("\\s+");
        // Elements should match ps command order
        if (split.length < 11) {
            return null;
        }
        String path = split[10];
        return new FreeBsdProcess(path.substring(path.lastIndexOf('/') + 1), // name
                path, // path
                split[0].charAt(0), // state, one of DILRSTWZ
                pid, // also split[1] but we already have
                ParseUtil.parseIntOrDefault(split[2], 0), // ppid
                ParseUtil.parseIntOrDefault(split[3], 0), // thread count
                ParseUtil.parseIntOrDefault(split[4], 0), // priority
                ParseUtil.parseLongOrDefault(split[5], 0L), // VSZ in kb
                ParseUtil.parseLongOrDefault(split[6], 0L), // RSS in kb
                ParseUtil.parseLongOrDefault(split[7], 0L), // elapsed secs
                ParseUtil.parseDHMSOrDefault(split[8], 0L), // system ms
                ParseUtil.parseDHMSOrDefault(split[9], 0L), // process ms
                0L, 0L, // 'top -bm io' gives read/write counts, not bytes
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
        List<String> procList = ExecutingCommand.runNative("ps -axo pid");
        if (!procList.isEmpty()) {
            // Subtract 1 for header
            return procList.size() - 1;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getThreadCount() {
        int threads = 0;
        for (String proc : ExecutingCommand.runNative("ps -axo nlwp")) {
            threads += ParseUtil.parseIntOrDefault(proc.trim(), 0);
        }
        return threads;
    }
}
