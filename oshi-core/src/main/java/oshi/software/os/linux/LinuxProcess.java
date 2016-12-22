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
package oshi.software.os.linux;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.software.common.AbstractProcess;
import oshi.software.os.OSProcess;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcUtil;

/**
 * A process is an instance of a computer program that is being executed. It
 * contains the program code and its current activity. Depending on the
 * operating system (OS), a process may be made up of multiple threads of
 * execution that execute instructions concurrently.
 *
 * @author widdis[at]gmail[dot]com
 */
public class LinuxProcess extends AbstractProcess {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LinuxProcess.class);

    /**
     * Jiffies per second, used for process time counters.
     */
    private static long hz = 1000L;

    /**
     * Boot time in MS.
     */
    private static long bootTime = 0L;

    static {
        init();
    }

    public LinuxProcess(String name, String path, char state, int processID, int parentProcessID, int threadCount,
            int priority, long virtualSize, long residentSetSize, long kernelTime, long userTime, long startTime,
            long bytesRead, long bytesWritten, long now) {
        this.name = name;
        this.path = path;
        switch (state) {
        case 'R':
            this.state = OSProcess.State.RUNNING;
            break;
        case 'S':
            this.state = OSProcess.State.SLEEPING;
            break;
        case 'D':
            this.state = OSProcess.State.WAITING;
            break;
        case 'Z':
            this.state = OSProcess.State.ZOMBIE;
            break;
        case 'T':
            this.state = OSProcess.State.STOPPED;
            break;
        default:
            this.state = OSProcess.State.OTHER;
            break;
        }
        this.processID = processID;
        this.parentProcessID = parentProcessID;
        this.threadCount = threadCount;
        this.priority = priority;
        this.virtualSize = virtualSize;
        this.residentSetSize = residentSetSize;
        this.kernelTime = kernelTime * 1000L / hz;
        this.userTime = userTime * 1000L / hz;
        this.startTime = bootTime + startTime * 1000L / hz;
        this.upTime = now - this.startTime;
        this.bytesRead = bytesRead;
        this.bytesWritten = bytesWritten;
    }

    /**
     * Correlate the youngest process start time in seconds with start time in
     * jiffies
     */
    private static void init() {
        // To correlate a process start time in seconds with the same process
        // start time in jiffies. We prefer the youngest (or close to it) which
        // minimizes its up time (etime)
        // Timeline:
        // BOOT|<----jiffies---->|<----etime---->|NOW
        // BOOT|<------------uptime------------->|NOW

        // To avoid having to check all processes we can just pick the highest
        // PID. This will either be the youngest one or at least big enough.

        // Get all the pid files (guaranteed to be digit-only filenames)
        File[] pids = ProcUtil.getPidFiles();
        // Sort descending "numerically"
        Arrays.sort(pids, (f1, f2) -> Integer.valueOf(f2.getName()).compareTo(Integer.valueOf(f1.getName())));

        // Iterate /proc/[pid]/stat checking the creation time (field 22,
        // jiffies since boot). Since we're working on descending PIDs, we
        // expect the first (higher PIDs) to be younger, but may have processes
        // that ended since we collected the files. The first time we get a
        // value we'll save it as the youngest and quit.
        long youngestJiffies = 0L;
        String youngestPid = "";
        for (File pid : pids) {
            List<String> stat = FileUtil.readFile(String.format("/proc/%s/stat", pid.getName()));
            if (!stat.isEmpty()) {
                String[] split = stat.get(0).split("\\s+");
                if (split.length >= 22) {
                    youngestJiffies = ParseUtil.parseLongOrDefault(split[21], 0L);
                    youngestPid = pid.getName();
                    break;
                }
            }
        }
        LOG.debug("Youngest PID is {} with {} jiffies", youngestPid, youngestJiffies);
        // Shouldn't happen but avoiding Division by zero
        if (youngestJiffies == 0) {
            LOG.error("Couldn't find any running processes, which is odd since we are in a running process. "
                    + "Process time values are in jiffies, not milliseconds.");
            return;
        }

        float startTimeSecsSinceBoot = ProcUtil.getSystemUptimeFromProc();
        bootTime = System.currentTimeMillis() - (long) (1000 * startTimeSecsSinceBoot);

        // This takes advantage of the fact that ps does all the heavy lifting
        // of sorting out HZ internally.
        String etime = ExecutingCommand.getFirstAnswer(String.format("ps -p %s -o etimes=", youngestPid));
        // Since we picked the youngest process, it's safe to assume an
        // etime close to 0 in case this command fails; the longer the system
        // has been up, the less impact this assumption will have
        if (!etime.isEmpty()) {
            LOG.debug("Etime is {} seconds", etime.trim());
            startTimeSecsSinceBoot -= Float.parseFloat(etime.trim());
        }
        // By subtracting etime (secs) from uptime (secs) we get uptime (in
        // secs) when the process was started. This correlates with startTime in
        // jiffies for this process
        LOG.debug("Start time in secs: {}", startTimeSecsSinceBoot);
        if (startTimeSecsSinceBoot <= 0) {
            LOG.warn("Couldn't calculate jiffies per second. "
                    + "Process time values are in jiffies, not milliseconds.");
            return;
        }

        // divide jiffies (since boot) by seconds (since boot)
        hz = (long) (youngestJiffies / startTimeSecsSinceBoot + 0.5f);
    }
}
