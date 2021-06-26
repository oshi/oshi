/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.software.os.unix.solaris;

import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.WAITING;
import static oshi.software.os.OSProcess.State.ZOMBIE;
import static oshi.util.Memoizer.memoize;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.LsofUtil;
import oshi.util.ParseUtil;

/**
 * OSProcess implemenation
 */
@ThreadSafe
public class SolarisOSProcess extends AbstractOSProcess {

    private Supplier<Integer> bitness = memoize(this::queryBitness);

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
    private long contextSwitches;

    public SolarisOSProcess(int pid, String[] split) {
        super(pid);
        updateAttributes(split);
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
    public long getContextSwitches() {
        return this.contextSwitches;
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
        long bitMask = 0L;
        String cpuset = ExecutingCommand.getFirstAnswer("pbind -q " + getProcessID());
        // Sample output:
        // <empty string if no binding>
        // pid 101048 strongly bound to processor(s) 0 1 2 3.
        if (cpuset.isEmpty()) {
            List<String> allProcs = ExecutingCommand.runNative("psrinfo");
            for (String proc : allProcs) {
                String[] split = ParseUtil.whitespaces.split(proc);
                int bitToSet = ParseUtil.parseIntOrDefault(split[0], -1);
                if (bitToSet >= 0) {
                    bitMask |= 1L << bitToSet;
                }
            }
            return bitMask;
        } else if (cpuset.endsWith(".") && cpuset.contains("strongly bound to processor(s)")) {
            String parse = cpuset.substring(0, cpuset.length() - 1);
            String[] split = ParseUtil.whitespaces.split(parse);
            for (int i = split.length - 1; i >= 0; i--) {
                int bitToSet = ParseUtil.parseIntOrDefault(split[i], -1);
                if (bitToSet >= 0) {
                    bitMask |= 1L << bitToSet;
                } else {
                    // Once we run into the word processor(s) we're done
                    break;
                }
            }
        }
        return bitMask;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        List<String> threadListInfo1 = ExecutingCommand
                .runNative("ps -o lwp,s,etime,stime,time,addr,pri -p " + getProcessID());
        List<String> threadListInfo2 = ExecutingCommand.runNative("prstat -L -v -p " + getProcessID() + " 1 1");
        Map<Integer, String[]> threadMap = parseAndMergePSandPrstatInfo(threadListInfo1, 0, 7, threadListInfo2, true);
        if (threadMap.keySet().size() > 1) {
            return threadMap.entrySet().stream().map(entry -> new SolarisOSThread(getProcessID(), entry.getValue()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public boolean updateAttributes() {
        int pid = getProcessID();
        List<String> procList = ExecutingCommand
                .runNative("ps -o s,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etime,time,comm,args -p " + pid);
        List<String> procList2 = ExecutingCommand.runNative("prstat -v -p " + pid + " 1 1");
        Map<Integer, String[]> processMap = parseAndMergePSandPrstatInfo(procList, 1, 15, procList2, false);
        if (processMap.containsKey(pid)) {
            return updateAttributes(processMap.get(getProcessID()));
        }
        this.state = State.INVALID;
        return false;
    }

    private boolean updateAttributes(String[] split) {
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
        this.virtualSize = ParseUtil.parseLongOrDefault(split[9], 0) * 1024;
        this.residentSetSize = ParseUtil.parseLongOrDefault(split[10], 0) * 1024;
        // Avoid divide by zero for processes up less than a second
        long elapsedTime = ParseUtil.parseDHMSOrDefault(split[11], 0L);
        this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
        this.startTime = now - this.upTime;
        this.kernelTime = 0L;
        this.userTime = ParseUtil.parseDHMSOrDefault(split[12], 0L);
        this.path = split[13];
        this.name = this.path.substring(this.path.lastIndexOf('/') + 1);
        this.commandLine = split[14];
        long nonVoluntaryContextSwitches = ParseUtil.parseLongOrDefault(split[15], 0L);
        long voluntaryContextSwitches = ParseUtil.parseLongOrDefault(split[16], 0L);
        this.contextSwitches = voluntaryContextSwitches + nonVoluntaryContextSwitches;

        return true;
    }

    /**
     * Merges results of a ps and prstat query, since Solaris process and thread
     * details are not available in a single command. Package private to permit
     * access by SolarisOperatingSystem and SolarisOSThread.
     *
     * @param psInfo
     *            output from ps command.
     * @param psKeyIndex
     *            which field of the ps split should be the key (e.g., pid or tid)
     * @param psLength
     *            how many fields to split
     * @param prstatInfo
     *            output from the prstat command.
     * @param useTid
     *            If true, parses thread id (slash-delimited last field), otherwise
     *            uses process id (field 0)
     * @return a map with key as thread id and an array of command outputs as value
     */
    static Map<Integer, String[]> parseAndMergePSandPrstatInfo(List<String> psInfo, int psKeyIndex, int psLength,
            List<String> prstatInfo, boolean useTid) {
        Map<Integer, String[]> map = new HashMap<>();
        if (psInfo.size() > 1) { // first row is header
            psInfo.stream().skip(1).forEach(info -> {
                String[] psSplit = ParseUtil.whitespaces.split(info.trim(), psLength);
                String[] mergedSplit = new String[psLength + 2];
                if (psSplit.length == psLength) {
                    for (int idx = 0; idx < psLength; idx++) {
                        if (idx == psKeyIndex) {
                            map.put(ParseUtil.parseIntOrDefault(psSplit[idx], 0), mergedSplit);
                        }
                        mergedSplit[idx] = psSplit[idx];
                    }
                }
            });
            // 0-pid, 1-username, 2-usertime, 3-sys, 4-trp, 5-tfl, 6-dfl, 7-lck, 8-slp,
            // 9-lat, 10-vcx, 11-icx, 12-scl, 13-sig, 14-process/lwpid
            if (prstatInfo.size() > 1) { // first row is header
                prstatInfo.stream().skip(1).forEach(threadInfo -> {
                    String[] splitPrstat = ParseUtil.whitespaces.split(threadInfo.trim());
                    if (splitPrstat.length == 15) {
                        String id = splitPrstat[0]; // pid
                        if (useTid) {
                            int idxAfterForwardSlash = splitPrstat[14].lastIndexOf('/') + 1; // format is process/lwpid
                            if (idxAfterForwardSlash > 0 && idxAfterForwardSlash < splitPrstat[14].length()) {
                                id = splitPrstat[14].substring(idxAfterForwardSlash); // getting the thread id
                            }
                        }
                        String[] existingSplit = map.get(Integer.parseInt(id));
                        if (existingSplit != null) { // if thread wasn't in ps command output
                            existingSplit[psLength] = splitPrstat[10]; // voluntary context switch
                            existingSplit[psLength + 1] = splitPrstat[11]; // involuntary context switch
                        }
                    }
                });
            }
        }
        return map;
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
            state = RUNNING;
            break;
        case 'S':
            state = SLEEPING;
            break;
        case 'R':
        case 'W':
            state = WAITING;
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
