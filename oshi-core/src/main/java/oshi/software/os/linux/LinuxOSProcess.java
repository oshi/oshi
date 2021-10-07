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
package oshi.software.os.linux;

import static oshi.hardware.platform.linux.LinuxGlobalMemory.PAGE_SIZE;
import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.proc.ProcessStat;
import oshi.driver.linux.proc.UserGroupInfo;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.platform.linux.ProcPath;

/**
 * OSProcess implementation
 */
@ThreadSafe
public class LinuxOSProcess extends AbstractOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxOSProcess.class);

    // Get a list of orders to pass to ParseUtil
    private static final int[] PROC_PID_STAT_ORDERS = new int[ProcPidStat.values().length];
    static {
        for (ProcPidStat stat : ProcPidStat.values()) {
            // The PROC_PID_STAT enum indices are 1-indexed.
            // Subtract one to get a zero-based index
            PROC_PID_STAT_ORDERS[stat.ordinal()] = stat.getOrder() - 1;
        }
    }

    private Supplier<Integer> bitness = memoize(this::queryBitness);
    private Supplier<String> commandLine = memoize(this::queryCommandLine);
    private Supplier<List<String>> arguments = memoize(this::queryArguments);
    private Supplier<Map<String, String>> environmentVariables = memoize(this::queryEnvironmentVariables);

    private String name;
    private String path = "";
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

    public LinuxOSProcess(int pid) {
        super(pid);
        updateAttributes();
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
        return commandLine.get();
    }

    private String queryCommandLine() {
        return Arrays
                .stream(FileUtil.getStringFromFile(String.format(ProcPath.PID_CMDLINE, getProcessID())).split("\0"))
                .collect(Collectors.joining(" "));
    }

    @Override
    public List<String> getArguments() {
        return arguments.get();
    }

    private List<String> queryArguments() {
        return Collections.unmodifiableList(ParseUtil
                .parseByteArrayToStrings(FileUtil.readAllBytes(String.format(ProcPath.PID_CMDLINE, getProcessID()))));
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables.get();
    }

    private Map<String, String> queryEnvironmentVariables() {
        return Collections.unmodifiableMap(ParseUtil
                .parseByteArrayToStringMap(FileUtil.readAllBytes(String.format(ProcPath.PID_ENVIRON, getProcessID()))));
    }

    @Override
    public String getCurrentWorkingDirectory() {
        try {
            String cwdLink = String.format(ProcPath.PID_CWD, getProcessID());
            String cwd = new File(cwdLink).getCanonicalPath();
            if (!cwd.equals(cwdLink)) {
                return cwd;
            }
        } catch (IOException e) {
            LOG.trace("Couldn't find cwd for pid {}: {}", getProcessID(), e.getMessage());
        }
        return "";
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
    public List<OSThread> getThreadDetails() {
        return ProcessStat.getThreadIds(getProcessID()).stream().map(id -> new LinuxOSThread(getProcessID(), id))
                .collect(Collectors.toList());
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
    public long getOpenFiles() {
        return ProcessStat.getFileDescriptorFiles(getProcessID()).length;
    }

    @Override
    public int getBitness() {
        return this.bitness.get();
    }

    private int queryBitness() {
        // get 5th byte of file for 64-bit check
        // https://en.wikipedia.org/wiki/Executable_and_Linkable_Format#File_header
        byte[] buffer = new byte[5];
        if (!path.isEmpty()) {
            try (InputStream is = new FileInputStream(path)) {
                if (is.read(buffer) == buffer.length) {
                    return buffer[4] == 1 ? 32 : 64;
                }
            } catch (IOException e) {
                LOG.warn("Failed to read process file: {}", path);
            }
        }
        return 0;
    }

    @Override
    public long getAffinityMask() {
        // Would prefer to use native sched_getaffinity call but variable sizing is
        // kernel-dependent and requires C macros, so we use command line instead.
        String mask = ExecutingCommand.getFirstAnswer("taskset -p " + getProcessID());
        // Output:
        // pid 3283's current affinity mask: 3
        // pid 9726's current affinity mask: f
        String[] split = ParseUtil.whitespaces.split(mask);
        try {
            return new BigInteger(split[split.length - 1], 16).longValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public boolean updateAttributes() {
        String procPidExe = String.format(ProcPath.PID_EXE, getProcessID());
        try {
            Path link = Paths.get(procPidExe);
            this.path = Files.readSymbolicLink(link).toString();
            // For some services the symbolic link process has terminated
            int index = path.indexOf(" (deleted)");
            if (index != -1) {
                path = path.substring(0, index);
            }
        } catch (InvalidPathException | IOException | UnsupportedOperationException | SecurityException e) {
            LOG.debug("Unable to open symbolic link {}", procPidExe);
        }
        // Fetch all the values here
        // check for terminated process race condition after last one.
        Map<String, String> io = FileUtil.getKeyValueMapFromFile(String.format(ProcPath.PID_IO, getProcessID()), ":");
        Map<String, String> status = FileUtil.getKeyValueMapFromFile(String.format(ProcPath.PID_STATUS, getProcessID()),
                ":");
        String stat = FileUtil.getStringFromFile(String.format(ProcPath.PID_STAT, getProcessID()));
        if (stat.isEmpty()) {
            this.state = INVALID;
            return false;
        }
        // If some details couldn't be read from ProcPath.PID_STATUS try reading it from
        // ProcPath.PID_STAT
        getMissingDetails(status, stat);

        long now = System.currentTimeMillis();

        // We can get name and status more easily from /proc/pid/status which we
        // call later, so just get the numeric bits here
        // See man proc for how to parse /proc/[pid]/stat
        long[] statArray = ParseUtil.parseStringToLongArray(stat, PROC_PID_STAT_ORDERS,
                ProcessStat.PROC_PID_STAT_LENGTH, ' ');

        // BOOTTIME is in seconds and start time from proc/pid/stat is in jiffies.
        // Combine units to jiffies and convert to millijiffies before hz division to
        // avoid precision loss without having to cast
        this.startTime = (LinuxOperatingSystem.BOOTTIME * LinuxOperatingSystem.getHz()
                + statArray[ProcPidStat.START_TIME.ordinal()]) * 1000L / LinuxOperatingSystem.getHz();
        // BOOT_TIME could be up to 500ms off and start time up to 5ms off. A process
        // that has started within last 505ms could produce a future start time/negative
        // up time, so insert a sanity check.
        if (startTime >= now) {
            startTime = now - 1;
        }
        this.parentProcessID = (int) statArray[ProcPidStat.PPID.ordinal()];
        this.threadCount = (int) statArray[ProcPidStat.THREAD_COUNT.ordinal()];
        this.priority = (int) statArray[ProcPidStat.PRIORITY.ordinal()];
        this.virtualSize = statArray[ProcPidStat.VSZ.ordinal()];
        this.residentSetSize = statArray[ProcPidStat.RSS.ordinal()] * PAGE_SIZE;
        this.kernelTime = statArray[ProcPidStat.KERNEL_TIME.ordinal()] * 1000L / LinuxOperatingSystem.getHz();
        this.userTime = statArray[ProcPidStat.USER_TIME.ordinal()] * 1000L / LinuxOperatingSystem.getHz();
        this.minorFaults = statArray[ProcPidStat.MINOR_FAULTS.ordinal()];
        this.majorFaults = statArray[ProcPidStat.MAJOR_FAULTS.ordinal()];
        long nonVoluntaryContextSwitches = ParseUtil.parseLongOrDefault(status.get("nonvoluntary_ctxt_switches"), 0L);
        long voluntaryContextSwitches = ParseUtil.parseLongOrDefault(status.get("voluntary_ctxt_switches"), 0L);
        this.contextSwitches = voluntaryContextSwitches + nonVoluntaryContextSwitches;

        this.upTime = now - startTime;

        // See man proc for how to parse /proc/[pid]/io
        this.bytesRead = ParseUtil.parseLongOrDefault(io.getOrDefault("read_bytes", ""), 0L);
        this.bytesWritten = ParseUtil.parseLongOrDefault(io.getOrDefault("write_bytes", ""), 0L);

        // Don't set open files or bitness or currentWorkingDirectory; fetch on demand.

        this.userID = ParseUtil.whitespaces.split(status.getOrDefault("Uid", ""))[0];
        this.user = UserGroupInfo.getUser(userID);
        this.groupID = ParseUtil.whitespaces.split(status.getOrDefault("Gid", ""))[0];
        this.group = UserGroupInfo.getGroupName(groupID);
        this.name = status.getOrDefault("Name", "");
        this.state = ProcessStat.getState(status.getOrDefault("State", "U").charAt(0));
        return true;
    }

    /**
     * If some details couldn't be read from ProcPath.PID_STATUS try reading it from
     * ProcPath.PID_STAT
     *
     * @param status
     *            status map to fill.
     * @param stat
     *            string to read from.
     */
    private static void getMissingDetails(Map<String, String> status, String stat) {
        if (status == null || stat == null) {
            return;
        }

        int nameStart = stat.indexOf('(');
        int nameEnd = stat.indexOf(')');
        if (Util.isBlank(status.get("Name")) && nameStart > 0 && nameStart < nameEnd) {
            // remove leading and trailing parentheses
            String statName = stat.substring(nameStart + 1, nameEnd);
            status.put("Name", statName);
        }

        // As per man, the next item after the name is the state
        if (Util.isBlank(status.get("State")) && nameEnd > 0 && stat.length() > nameEnd + 2) {
            String statState = String.valueOf(stat.charAt(nameEnd + 2));
            status.put("State", statState);
        }
    }

    /**
     * Enum used to update attributes. The order field represents the 1-indexed
     * numeric order of the stat in /proc/pid/stat per the man file.
     */
    private enum ProcPidStat {
        // The parsing implementation in ParseUtil requires these to be declared
        // in increasing order
        PPID(4), MINOR_FAULTS(10), MAJOR_FAULTS(12), USER_TIME(14), KERNEL_TIME(15), PRIORITY(18), THREAD_COUNT(20),
        START_TIME(22), VSZ(23), RSS(24);

        private int order;

        public int getOrder() {
            return this.order;
        }

        ProcPidStat(int order) {
            this.order = order;
        }
    }
}
