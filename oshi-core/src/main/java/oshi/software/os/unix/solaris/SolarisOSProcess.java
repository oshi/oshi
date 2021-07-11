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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.solaris.PsInfo;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.software.os.unix.solaris.SolarisOperatingSystem.PrstatKeywords;
import oshi.software.os.unix.solaris.SolarisOperatingSystem.PsKeywords;
import oshi.util.ExecutingCommand;
import oshi.util.LsofUtil;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * OSProcess implementation
 */
@ThreadSafe
public class SolarisOSProcess extends AbstractOSProcess {
    /*
     * Package-private for use by SolarisOSThread
     */
    enum PsThreadColumns {
        LWP, S, ETIME, TIME, ADDR, PRI;
    }

    static final String PS_THREAD_COLUMNS = Arrays.stream(PsThreadColumns.values()).map(Enum::name)
            .map(String::toLowerCase).collect(Collectors.joining(","));

    private Supplier<Integer> bitness = memoize(this::queryBitness);
    private Supplier<String> commandLine = memoize(this::queryCommandLine);
    private Supplier<Pair<List<String>, Map<String, String>>> cmdEnv = memoize(this::queryCommandlineEnvironment);

    private String name;
    private String path = "";
    private String commandLineBackup;
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
    private long contextSwitches = 0; // default

    public SolarisOSProcess(int pid, Map<PsKeywords, String> psMap, Map<PrstatKeywords, String> prstatMap) {
        super(pid);
        updateAttributes(psMap, prstatMap);
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
        String cl = String.join(" ", getArguments());
        return cl.isEmpty() ? this.commandLineBackup : cl;
    }

    @Override
    public List<String> getArguments() {
        return cmdEnv.get().getA();
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return cmdEnv.get().getB();
    }

    private Pair<List<String>, Map<String, String>> queryCommandlineEnvironment() {
        return PsInfo.queryArgsEnv(getProcessID());
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
        List<OSThread> threads = new ArrayList<>();
        List<String> threadList = ExecutingCommand.runNative("ps -o " + PS_THREAD_COLUMNS + " -p " + getProcessID());
        if (threadList.size() > 1) {
            // Get a map by lwpid of prstat output
            List<String> prstatList = ExecutingCommand.runNative("prstat -L -v -p " + getProcessID() + " 1 1");
            Map<String, String> prstatRowMap = new HashMap<>();
            for (String s : prstatList) {
                String row = s.trim();
                // Last element is PROCESS/LWPID
                int idx = row.lastIndexOf('/');
                if (idx > 0) {
                    prstatRowMap.put(row.substring(idx + 1), row);
                }
            }
            // remove header row and iterate thread list
            threadList.remove(0);
            for (String thread : threadList) {
                Map<PsThreadColumns, String> psMap = ParseUtil.stringToEnumMap(PsThreadColumns.class, thread.trim(),
                        ' ');
                // Check if last (thus all) value populated
                if (psMap.containsKey(PsThreadColumns.PRI)) {
                    String lwpStr = psMap.get(PsThreadColumns.LWP);
                    Map<PrstatKeywords, String> prstatMap = ParseUtil.stringToEnumMap(PrstatKeywords.class,
                            prstatRowMap.getOrDefault(lwpStr, ""), ' ');
                    threads.add(new SolarisOSThread(getProcessID(), psMap, prstatMap));
                }
            }
        }
        return threads;
    }

    @Override
    public boolean updateAttributes() {
        int pid = getProcessID();
        List<String> procList = ExecutingCommand
                .runNative("ps -o " + SolarisOperatingSystem.PS_COMMAND_ARGS + " -p " + pid);
        if (procList.size() > 1) {
            Map<PsKeywords, String> psMap = ParseUtil.stringToEnumMap(PsKeywords.class, procList.get(1).trim(), ' ');
            // Check if last (thus all) value populated
            if (psMap.containsKey(PsKeywords.ARGS)) {
                String pidStr = psMap.get(PsKeywords.PID);
                List<String> prstatList = ExecutingCommand.runNative("prstat -v -p " + pidStr + " 1 1");
                String prstatRow = "";
                for (String s : prstatList) {
                    String row = s.trim();
                    if (row.startsWith(pidStr + " ")) {
                        prstatRow = row;
                        break;
                    }
                }
                Map<PrstatKeywords, String> prstatMap = ParseUtil.stringToEnumMap(PrstatKeywords.class, prstatRow, ' ');
                return updateAttributes(psMap, prstatMap);
            }
        }
        this.state = State.INVALID;
        return false;
    }

    private boolean updateAttributes(Map<PsKeywords, String> psMap, Map<PrstatKeywords, String> prstatMap) {
        long now = System.currentTimeMillis();
        this.state = getStateFromOutput(psMap.get(PsKeywords.S).charAt(0));
        this.parentProcessID = ParseUtil.parseIntOrDefault(psMap.get(PsKeywords.PPID), 0);
        this.user = psMap.get(PsKeywords.USER);
        this.userID = psMap.get(PsKeywords.UID);
        this.group = psMap.get(PsKeywords.GROUP);
        this.groupID = psMap.get(PsKeywords.GID);
        this.threadCount = ParseUtil.parseIntOrDefault(psMap.get(PsKeywords.NLWP), 0);
        this.priority = ParseUtil.parseIntOrDefault(psMap.get(PsKeywords.PRI), 0);
        // These are in KB, multiply
        this.virtualSize = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.VSZ), 0) * 1024;
        this.residentSetSize = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.RSS), 0) * 1024;
        // Avoid divide by zero for processes up less than a second
        long elapsedTime = ParseUtil.parseDHMSOrDefault(psMap.get(PsKeywords.ETIME), 0L);
        this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
        this.startTime = now - this.upTime;
        this.kernelTime = 0L;
        this.userTime = ParseUtil.parseDHMSOrDefault(psMap.get(PsKeywords.TIME), 0L);
        this.path = psMap.get(PsKeywords.COMM);
        this.name = this.path.substring(this.path.lastIndexOf('/') + 1);
        this.commandLineBackup = psMap.get(PsKeywords.ARGS);
        if (prstatMap.containsKey(PrstatKeywords.ICX)) {
            long nonVoluntaryContextSwitches = ParseUtil.parseLongOrDefault(prstatMap.get(PrstatKeywords.ICX), 0L);
            long voluntaryContextSwitches = ParseUtil.parseLongOrDefault(prstatMap.get(PrstatKeywords.VCX), 0L);
            this.contextSwitches = voluntaryContextSwitches + nonVoluntaryContextSwitches;
        }
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
