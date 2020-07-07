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
package oshi.software.os.unix.aix;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.WAITING;
import static oshi.software.os.OSProcess.State.ZOMBIE;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.perfstat.PerfstatCpu;
import oshi.jna.platform.unix.aix.Perfstat.perfstat_process_t;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.LsofUtil;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
public class AixOSProcess extends AbstractOSProcess {

    private Supplier<Integer> bitness = memoize(this::queryBitness);
    private final Supplier<Long> affinityMask = memoize(PerfstatCpu::queryCpuAffinityMask, defaultExpiration());

    private String name;
    private String path = "";
    private String commandLine;
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
    private long majorFaults;

    // Memoized copy from OperatingSystem
    private Supplier<perfstat_process_t[]> procCpu;

    public AixOSProcess(int pid, String[] split, Map<Integer, Pair<Long, Long>> cpuMap,
            Supplier<perfstat_process_t[]> procCpu) {
        super(pid);
        this.procCpu = procCpu;
        updateAttributes(split, cpuMap);
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
        return this.commandLine;
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return LsofUtil.getCwd(getProcessID());
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
        return LsofUtil.getOpenFiles(getProcessID());
    }

    @Override
    public int getBitness() {
        return this.bitness.get();
    }

    private int queryBitness() {
        List<String> pflags = ExecutingCommand.runNative("pflags " + getProcessID());
        for (String line : pflags) {
            if (line.contains("data model")) {
                if (line.contains("LP32")) {
                    return 32;
                } else if (line.contains("LP64")) {
                    return 64;
                }
            }
        }
        return 0;
    }

    @Override
    public long getAffinityMask() {
        // Need to capture BND field from ps
        // ps -m -o THREAD -p 12345
        // BND field for PID is either a dash (all processors) or the processor it's
        // bound to, do 1L << # to get mask
        long mask = 0L;
        List<String> processAffinityInfoList = ExecutingCommand.runNative("ps -m -o THREAD -p " + getProcessID());
        if (processAffinityInfoList.size() > 2) { // what happens when the process has not thread?
            processAffinityInfoList.remove(0); // remove header row
            processAffinityInfoList.remove(0); // remove process row
            for (String processAffinityInfo : processAffinityInfoList) { // affinity information is in thread row
                String[] threadInfoSplit = ParseUtil.whitespaces.split(processAffinityInfo.trim());
                if (threadInfoSplit.length > 13 && threadInfoSplit[4].charAt(0) != 'Z') { // only non-zombie threads
                    if (threadInfoSplit[11].charAt(0) == '-') { // affinity to all processors
                        return this.affinityMask.get();
                    } else {
                        int affinity = ParseUtil.parseIntOrDefault(threadInfoSplit[11], 0);
                        mask |= 1L << affinity;
                    }
                }
            }
        }
        return mask;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        List<String> threadListInfoPs = ExecutingCommand.runNative("ps -m -o THREAD -p " + getProcessID());
        // 1st row is header, 2nd row is process data.
        if (threadListInfoPs.size() > 2) {
            List<OSThread> threads = new ArrayList<>();
            threadListInfoPs.remove(0); // header removed
            threadListInfoPs.remove(0); // process data removed
            for (String threadInfo : threadListInfoPs) {
                // USER,PID,PPID,TID,ST,CP,PRI,SC,WCHAN,F,TT,BND,COMMAND
                String[] threadInfoSplit = ParseUtil.whitespaces.split(threadInfo.trim());
                if (threadInfoSplit.length == 13) {
                    String[] split = new String[3];
                    split[0] = threadInfoSplit[3]; // tid
                    split[1] = threadInfoSplit[4]; // state
                    split[2] = threadInfoSplit[6]; // priority
                    threads.add(new AixOSThread(getProcessID(), split));
                }
            }
            return threads;
        }
        return Collections.emptyList();
    }

    @Override
    public long getMajorFaults() {
        return this.majorFaults;
    }

    @Override
    public boolean updateAttributes() {
        perfstat_process_t[] perfstat = procCpu.get();
        List<String> procList = ExecutingCommand.runNative(
                "ps -o s,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etime,time,comm,args -p " + getProcessID());
        // Parse array to map of user/system times
        Map<Integer, Pair<Long, Long>> cpuMap = new HashMap<>();
        for (perfstat_process_t stat : perfstat) {
            cpuMap.put((int) stat.pid, new Pair<>((long) stat.ucpu_time, (long) stat.scpu_time));
        }
        if (procList.size() > 1) {
            String[] split = ParseUtil.whitespaces.split(procList.get(1).trim(), 15);
            // Elements should match ps command order
            if (split.length == 15) {
                return updateAttributes(split, cpuMap);
            }
        }
        this.state = State.INVALID;
        return false;
    }

    private boolean updateAttributes(String[] split, Map<Integer, Pair<Long, Long>> cpuMap) {
        long now = System.currentTimeMillis();
        this.state = getStateFromOutput(split[0].charAt(0));
        this.parentProcessID = ParseUtil.parseIntOrDefault(split[2], 0);
        this.user = split[3];
        this.userID = split[4];
        this.group = split[5];
        this.groupID = split[6];
        this.threadCount = ParseUtil.parseIntOrDefault(split[7], 0);
        this.priority = ParseUtil.parseIntOrDefault(split[8], 0);
        // These are in KB, multiply
        this.virtualSize = ParseUtil.parseLongOrDefault(split[9], 0) << 10;
        this.residentSetSize = ParseUtil.parseLongOrDefault(split[10], 0) << 10;
        long elapsedTime = ParseUtil.parseDHMSOrDefault(split[11], 0L);
        if (cpuMap.containsKey(getProcessID())) {
            Pair<Long, Long> userSystem = cpuMap.get(getProcessID());
            this.userTime = userSystem.getA();
            this.kernelTime = userSystem.getB();
        } else {
            this.userTime = ParseUtil.parseDHMSOrDefault(split[12], 0L);
            this.kernelTime = 0L;
        }
        // Avoid divide by zero for processes up less than a second
        this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
        while (this.upTime < this.userTime + this.kernelTime) {
            this.upTime += 500L;
        }
        this.startTime = now - this.upTime;
        this.name = split[13];
        this.majorFaults = ParseUtil.parseLongOrDefault(split[14], 0L);
        this.path = ParseUtil.whitespaces.split(split[15])[0];
        this.commandLine = split[15];
        return true;
    }

    /***
     * Returns Enum STATE for the state value obtained from status string of
     * thread/process.
     *
     * @param stateValue
     *            state value from the status string
     * @return The state
     */
    static State getStateFromOutput(char stateValue) {
        State state;
        switch (stateValue) {
        case 'O':
            state = INVALID;
            break;
        case 'R':
        case 'A':
            state = RUNNING;
            break;
        case 'I':
            state = WAITING;
            break;
        case 'S':
        case 'W':
            state = SLEEPING;
            break;
        case 'Z':
            state = ZOMBIE;
            break;
        case 'T':
            state = STOPPED;
            break;
        default:
            state = OTHER;
            break;
        }
        return state;
    }
}
