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
package oshi.software.os.unix.freebsd;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.WAITING;
import static oshi.software.os.OSProcess.State.ZOMBIE;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.sun.jna.Memory; // NOSONAR squid:S1191
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.NativeSizeTByReference;
import oshi.jna.platform.unix.freebsd.FreeBsdLibc;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.software.os.unix.freebsd.FreeBsdOperatingSystem.PsKeywords;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.ProcstatUtil;

/**
 * OSProcess implemenation
 */
@ThreadSafe
public class FreeBsdOSProcess extends AbstractOSProcess {

    /*
     * Package-private for use by FreeBsdOSProcess
     */
    enum PsThreadColumns {
        TDNAME, LWP, STATE, ETIMES, SYSTIME, TIME, TDADDR, NIVCSW, NVCSW, MAJFLT, MINFLT, PRI;
    }

    static final String PS_THREAD_COLUMNS = Arrays.stream(PsThreadColumns.values()).map(Enum::name)
            .map(String::toLowerCase).collect(Collectors.joining(","));

    private Supplier<Integer> bitness = memoize(this::queryBitness);

    private String name;
    private String path = "";
    private String commandLine;
    private String user;
    private String userID;
    private String group;
    private String groupID;
    private State state = INVALID;
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
    private long minorFaults;
    private long majorFaults;
    private long contextSwitches;

    public FreeBsdOSProcess(int pid, Map<PsKeywords, String> psMap) {
        super(pid);
        updateAttributes(psMap);
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
        return ProcstatUtil.getCwd(getProcessID());
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
        return ProcstatUtil.getOpenFiles(getProcessID());
    }

    @Override
    public int getBitness() {
        return this.bitness.get();
    }

    @Override
    public long getAffinityMask() {
        long bitMask = 0L;
        // Would prefer to use native cpuset_getaffinity call but variable sizing is
        // kernel-dependent and requires C macros, so we use commandline instead.
        String cpuset = ExecutingCommand.getFirstAnswer("cpuset -gp " + getProcessID());
        // Sample output:
        // pid 8 mask: 0, 1
        // cpuset: getaffinity: No such process
        String[] split = cpuset.split(":");
        if (split.length > 1) {
            String[] bits = split[1].split(",");
            for (String bit : bits) {
                int bitToSet = ParseUtil.parseIntOrDefault(bit.trim(), -1);
                if (bitToSet >= 0) {
                    bitMask |= 1L << bitToSet;
                }
            }
        }
        return bitMask;
    }

    private int queryBitness() {
        // Get process abi vector
        int[] mib = new int[4];
        mib[0] = 1; // CTL_KERN
        mib[1] = 14; // KERN_PROC
        mib[2] = 9; // KERN_PROC_SV_NAME
        mib[3] = getProcessID();
        // Allocate memory for arguments
        Pointer abi = new Memory(32);
        NativeSizeTByReference size = new NativeSizeTByReference(new size_t(32));
        // Fetch abi vector
        if (0 == FreeBsdLibc.INSTANCE.sysctl(mib, mib.length, abi, size, null, size_t.ZERO)) {
            String elf = abi.getString(0);
            if (elf.contains("ELF32")) {
                return 32;
            } else if (elf.contains("ELF64")) {
                return 64;
            }
        }
        return 0;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        List<OSThread> threads = new ArrayList<>();
        String psCommand = "ps -awwxo " + PS_THREAD_COLUMNS + " -H";
        if (getProcessID() >= 0) {
            psCommand += " -p " + getProcessID();
        }
        List<String> threadList = ExecutingCommand.runNative(psCommand);
        if (threadList.size() > 1) {
            // remove header row
            threadList.remove(0);
            // Fill list
            for (String thread : threadList) {
                Map<PsThreadColumns, String> threadMap = ParseUtil.stringToEnumMap(PsThreadColumns.class, thread.trim(),
                        ' ');
                if (threadMap.containsKey(PsThreadColumns.PRI)) {
                    threads.add(new FreeBsdOSThread(getProcessID(), threadMap));
                }
            }
        }
        return threads;
    }

    @Override
    public long getMinorFaults() {
        return this.minorFaults;
    }

    @Override
    public long getMajorFaults() {
        return this.majorFaults;
    }

    @Override
    public long getContextSwitches() {
        return this.contextSwitches;
    }

    @Override
    public boolean updateAttributes() {
        String psCommand = "ps -awwxo " + FreeBsdOperatingSystem.PS_COMMAND_ARGS + " -p " + getProcessID();
        List<String> procList = ExecutingCommand.runNative(psCommand);
        if (procList.size() > 1) {
            // skip header row
            Map<PsKeywords, String> psMap = ParseUtil.stringToEnumMap(PsKeywords.class, procList.get(1).trim(), ' ');
            // Check if last (thus all) value populated
            if (psMap.containsKey(PsKeywords.ARGS)) {
                return updateAttributes(psMap);
            }
        }
        this.state = INVALID;
        return false;
    }

    private boolean updateAttributes(Map<PsKeywords, String> psMap) {
        long now = System.currentTimeMillis();
        switch (psMap.get(PsKeywords.STATE).charAt(0)) {
        case 'R':
            this.state = RUNNING;
            break;
        case 'I':
        case 'S':
            this.state = SLEEPING;
            break;
        case 'D':
        case 'L':
        case 'U':
            this.state = WAITING;
            break;
        case 'Z':
            this.state = ZOMBIE;
            break;
        case 'T':
            this.state = STOPPED;
            break;
        default:
            this.state = OTHER;
            break;
        }
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
        long elapsedTime = ParseUtil.parseDHMSOrDefault(psMap.get(PsKeywords.ETIMES), 0L);
        this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
        this.startTime = now - this.upTime;
        this.kernelTime = ParseUtil.parseDHMSOrDefault(psMap.get(PsKeywords.SYSTIME), 0L);
        this.userTime = ParseUtil.parseDHMSOrDefault(psMap.get(PsKeywords.TIME), 0L) - this.kernelTime;
        this.path = psMap.get(PsKeywords.COMM);
        this.name = this.path.substring(this.path.lastIndexOf('/') + 1);
        this.minorFaults = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.MAJFLT), 0L);
        this.majorFaults = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.MINFLT), 0L);
        long nonVoluntaryContextSwitches = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.NVCSW), 0L);
        long voluntaryContextSwitches = ParseUtil.parseLongOrDefault(psMap.get(PsKeywords.NIVCSW), 0L);
        this.contextSwitches = voluntaryContextSwitches + nonVoluntaryContextSwitches;
        this.commandLine = psMap.get(PsKeywords.ARGS);
        return true;
    }
}
