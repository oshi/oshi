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
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.sun.jna.Native;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_process_t; // NOSONAR squid:S1191

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.PsInfo;
import oshi.driver.unix.aix.perfstat.PerfstatCpu;
import oshi.jna.platform.unix.AixLibc.AixPsInfo;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.LsofUtil;
import oshi.util.ParseUtil;
import oshi.util.UserGroupInfo;
import oshi.util.tuples.Pair;

/**
 * OSProcess implementation
 */
@ThreadSafe
public class AixOSProcess extends AbstractOSProcess {
    /*
     * Package-private for use by AIXOSThread
     */
    enum PsThreadColumns {
        USER, PID, PPID, TID, ST, CP, PRI, SC, WCHAN, F, TT, BND, COMMAND;
    }

    private Supplier<Integer> bitness = memoize(this::queryBitness);
    private Supplier<AixPsInfo> psinfo = memoize(this::queryPsInfo, defaultExpiration());
    private Supplier<String> commandLine = memoize(this::queryCommandLine);
    private Supplier<Pair<List<String>, Map<String, String>>> cmdEnv = memoize(this::queryCommandlineEnvironment);
    private final Supplier<Long> affinityMask = memoize(PerfstatCpu::queryCpuAffinityMask, defaultExpiration());

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

    // Memoized copy from OperatingSystem
    private Supplier<perfstat_process_t[]> procCpu;

    public AixOSProcess(int pid, Pair<Long, Long> userSysCpuTime, Supplier<perfstat_process_t[]> procCpu) {
        super(pid);
        this.procCpu = procCpu;
        updateAttributes(userSysCpuTime);
    }

    private AixPsInfo queryPsInfo() {
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
                Map<PsThreadColumns, String> threadMap = ParseUtil.stringToEnumMap(PsThreadColumns.class,
                        processAffinityInfo.trim(), ' ');
                if (threadMap.containsKey(PsThreadColumns.COMMAND)
                        && threadMap.get(PsThreadColumns.ST).charAt(0) != 'Z') { // only non-zombie threads
                    String bnd = threadMap.get(PsThreadColumns.BND);
                    if (bnd.charAt(0) == '-') { // affinity to all processors
                        return this.affinityMask.get();
                    } else {
                        int affinity = ParseUtil.parseIntOrDefault(bnd, 0);
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
                Map<PsThreadColumns, String> threadMap = ParseUtil.stringToEnumMap(PsThreadColumns.class,
                        threadInfo.trim(), ' ');
                if (threadMap.containsKey(PsThreadColumns.COMMAND)) {
                    threads.add(new AixOSThread(getProcessID(), threadMap));
                }
            }
            return threads;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean updateAttributes() {
        perfstat_process_t[] perfstat = procCpu.get();
        for (perfstat_process_t stat : perfstat) {
            int statpid = (int) stat.pid;
            if (statpid == getProcessID()) {
                return updateAttributes(new Pair<>((long) stat.ucpu_time, (long) stat.scpu_time));
            }
        }
        this.state = State.INVALID;
        return false;
    }

    private boolean updateAttributes(Pair<Long, Long> userSysCpuTime) {
        AixPsInfo info = psinfo.get();
        if (info == null) {
            this.state = INVALID;
            return false;
        }

        long now = System.currentTimeMillis();
        this.state = getStateFromOutput((char) info.pr_lwp.pr_sname);
        this.parentProcessID = (int) info.pr_ppid;
        this.userID = Long.toString(info.pr_euid);
        this.user = UserGroupInfo.getUser(this.userID);
        this.groupID = Long.toString(info.pr_egid);
        this.group = UserGroupInfo.getGroupName(this.groupID);
        this.threadCount = info.pr_nlwp;
        this.priority = info.pr_lwp.pr_pri;
        // These are in KB, multiply
        this.virtualSize = info.pr_size * 1024;
        this.residentSetSize = info.pr_rssize * 1024;
        this.startTime = info.pr_start.tv_sec * 1000L + info.pr_start.tv_nsec / 1_000_000L;
        // Avoid divide by zero for processes up less than a millisecond
        long elapsedTime = now - this.startTime;
        this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
        this.userTime = userSysCpuTime.getA();
        this.kernelTime = userSysCpuTime.getB();
        this.commandLineBackup = Native.toString(info.pr_psargs);
        this.path = ParseUtil.whitespaces.split(commandLineBackup)[0];
        this.name = this.path.substring(this.path.lastIndexOf('/') + 1);
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
