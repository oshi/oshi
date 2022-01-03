/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.WAITING;
import static oshi.software.os.OSProcess.State.ZOMBIE;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.sun.jna.Native; // NOSONAR squid:S1191

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.solaris.PsInfo;
import oshi.jna.platform.unix.SolarisLibc.SolarisPsInfo;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.software.os.unix.solaris.SolarisOperatingSystem.PrstatKeywords;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.LsofUtil;
import oshi.util.ParseUtil;
import oshi.util.UserGroupInfo;
import oshi.util.tuples.Pair;

/**
 * OSProcess implementation
 */
@ThreadSafe
public class SolarisOSProcess extends AbstractOSProcess {

    enum PrstatLKeywords {
        // prstat -L -v
        PID, USERNAME, USR, SYS, TRP, TFL, DFL, LCK, SLP, LAT, VCX, ICX, SCL, SIG, LWPID, PROCESS_LWPNAME
    }

    private Supplier<Integer> bitness = memoize(this::queryBitness);
    private Supplier<SolarisPsInfo> psinfo = memoize(this::queryPsInfo, defaultExpiration());
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

    public SolarisOSProcess(int pid, Map<PrstatKeywords, String> prstatMap) {
        super(pid);
        updateAttributes(prstatMap);
    }

    private SolarisPsInfo queryPsInfo() {
        return PsInfo.queryPsInfo(this.getProcessID());
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
        return PsInfo.queryArgsEnv(getProcessID(), psinfo.get());
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

        // Get process files in proc
        File directory = new File(String.format("/proc/%d/lwp", getProcessID()));
        File[] numericFiles = directory.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());
        if (numericFiles == null) {
            return threads;
        }

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

        // Iterate files
        for (File lwpidFile : numericFiles) {
            Map<PrstatLKeywords, String> prstatMap = ParseUtil.stringToEnumMap(PrstatLKeywords.class,
                    prstatRowMap.getOrDefault(lwpidFile.getName(), ""), ' ');
            int lwpidNum = ParseUtil.parseIntOrDefault(lwpidFile.getName(), 0);
            OSThread thread = new SolarisOSThread(getProcessID(), lwpidNum, prstatMap);
            if (thread.getState() != INVALID) {
                threads.add(thread);
            }
        }
        return threads;
    }

    @Override
    public boolean updateAttributes() {
        int pid = getProcessID();
        List<String> prstatList = ExecutingCommand.runNative("prstat -v -p " + pid + " 1 1");
        String prstatRow = "";
        for (String s : prstatList) {
            String row = s.trim();
            if (row.startsWith(pid + " ")) {
                prstatRow = row;
                break;
            }
        }
        Map<PrstatKeywords, String> prstatMap = ParseUtil.stringToEnumMap(PrstatKeywords.class, prstatRow, ' ');
        return updateAttributes(prstatMap);
    }

    private boolean updateAttributes(Map<PrstatKeywords, String> prstatMap) {
        SolarisPsInfo info = psinfo.get();
        if (info == null) {
            this.state = INVALID;
            return false;
        }
        long now = System.currentTimeMillis();
        this.state = getStateFromOutput((char) info.pr_lwp.pr_sname);
        this.parentProcessID = info.pr_ppid;
        this.userID = Integer.toString(info.pr_euid);
        this.user = UserGroupInfo.getUser(this.userID);
        this.groupID = Integer.toString(info.pr_egid);
        this.group = UserGroupInfo.getGroupName(this.groupID);
        this.threadCount = info.pr_nlwp;
        this.priority = info.pr_lwp.pr_pri;
        // These are in KB, multiply
        this.virtualSize = info.pr_size.longValue() * 1024;
        this.residentSetSize = info.pr_rssize.longValue() * 1024;
        this.startTime = info.pr_start.tv_sec.longValue() * 1000L + info.pr_start.tv_nsec.longValue() / 1_000_000L;
        // Avoid divide by zero for processes up less than a millisecond
        long elapsedTime = now - this.startTime;
        this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
        this.kernelTime = 0L;
        this.userTime = info.pr_time.tv_sec.longValue() * 1000L + info.pr_time.tv_nsec.longValue() / 1_000_000L;
        // 80 character truncation but enough for path and name (usually)
        this.commandLineBackup = Native.toString(info.pr_psargs);
        this.path = ParseUtil.whitespaces.split(commandLineBackup)[0];
        this.name = this.path.substring(this.path.lastIndexOf('/') + 1);
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
};