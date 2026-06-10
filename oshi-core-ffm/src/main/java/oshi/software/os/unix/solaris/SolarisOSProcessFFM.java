/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.solaris;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.WAITING;
import static oshi.software.os.OSProcess.State.ZOMBIE;
import static oshi.software.os.OSThread.ThreadFiltering.VALID_THREAD;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.solaris.PsInfoFFM;
import oshi.ffm.platform.unix.solaris.SolarisLibcFunctions;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.UserGroupInfo;
import oshi.util.tuples.Pair;

@ThreadSafe
public class SolarisOSProcessFFM extends AbstractOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(SolarisOSProcessFFM.class);

    private final SolarisOperatingSystemFFM os;

    private Supplier<Integer> bitness = memoize(this::queryBitness);
    private Supplier<PsInfoFFM.PsInfo> psinfo = memoize(this::queryPsInfo, defaultExpiration());
    private Supplier<String> commandLine = memoize(this::queryCommandLine);
    private Supplier<Pair<List<String>, Map<String, String>>> cmdEnv = memoize(this::queryCommandlineEnvironment);
    private Supplier<PsInfoFFM.PrUsage> prusage = memoize(this::queryPrUsage, defaultExpiration());

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
    private long residentSetSizePrivate;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private long bytesRead;
    private long bytesWritten;
    private long minorFaults;
    private long majorFaults;
    private long voluntaryContextSwitches;
    private long involuntaryContextSwitches;

    public SolarisOSProcessFFM(int pid, SolarisOperatingSystemFFM os) {
        super(pid);
        this.os = os;
        updateAttributes();
    }

    private PsInfoFFM.PsInfo queryPsInfo() {
        return PsInfoFFM.queryPsInfo(this.getProcessID());
    }

    private PsInfoFFM.PrUsage queryPrUsage() {
        return PsInfoFFM.queryPrUsage(this.getProcessID());
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
        return PsInfoFFM.queryArgsEnv(getProcessID());
    }

    @Override
    public String getCurrentWorkingDirectory() {
        try {
            String cwdLink = "/proc/" + getProcessID() + "/cwd";
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
    public long getResidentMemory() {
        return this.residentSetSize;
    }

    @Override
    public long getPrivateResidentMemory() {
        return this.residentSetSizePrivate;
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
    public long getMinorFaults() {
        return this.minorFaults;
    }

    @Override
    public long getMajorFaults() {
        return this.majorFaults;
    }

    @Override
    public long getVoluntaryContextSwitches() {
        return this.voluntaryContextSwitches;
    }

    @Override
    public long getInvoluntaryContextSwitches() {
        return this.involuntaryContextSwitches;
    }

    @Override
    public long getOpenFiles() {
        try (Stream<Path> fd = Files.list(Paths.get("/proc/" + getProcessID() + "/fd"))) {
            return fd.count();
        } catch (IOException _) {
            return 0L;
        }
    }

    @Override
    public long getSoftOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            return rlimitNofile(true);
        } else {
            return getProcessOpenFileLimit(getProcessID(), 1);
        }
    }

    @Override
    public long getHardOpenFileLimit() {
        if (getProcessID() == this.os.getProcessId()) {
            return rlimitNofile(false);
        } else {
            return getProcessOpenFileLimit(getProcessID(), 2);
        }
    }

    private static long rlimitNofile(boolean soft) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rlim = arena.allocate(SolarisLibcFunctions.RLIMIT_LAYOUT);
            if (SolarisLibcFunctions.getrlimit(SolarisLibcFunctions.RLIMIT_NOFILE, rlim) != 0) {
                return -1L;
            }
            return soft ? SolarisLibcFunctions.rlimitCur(rlim) : SolarisLibcFunctions.rlimitMax(rlim);
        } catch (Throwable t) {
            LOG.warn("getrlimit failed", t);
            return -1L;
        }
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
                    break;
                }
            }
        }
        return bitMask;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        File directory = new File(String.format(Locale.ROOT, "/proc/%d/lwp", getProcessID()));
        File[] numericFiles = directory.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());
        if (numericFiles == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(numericFiles).parallel()
                .map(lwpidFile -> new SolarisOSThreadFFM(getProcessID(),
                        ParseUtil.parseIntOrDefault(lwpidFile.getName(), 0)))
                .filter(VALID_THREAD).collect(Collectors.toList());
    }

    @Override
    public boolean updateAttributes() {
        PsInfoFFM.PsInfo info = psinfo.get();
        if (info == null) {
            this.state = INVALID;
            return false;
        }
        PsInfoFFM.PrUsage usage = prusage.get();
        long now = System.currentTimeMillis();
        this.state = getStateFromOutput((char) info.pr_lwp.pr_sname);
        this.parentProcessID = info.pr_ppid;
        this.userID = Integer.toString(info.pr_euid);
        this.user = UserGroupInfo.getUser(this.userID);
        this.groupID = Integer.toString(info.pr_egid);
        this.group = UserGroupInfo.getGroupName(this.groupID);
        this.threadCount = info.pr_nlwp;
        this.priority = info.pr_lwp.pr_pri;
        this.virtualSize = info.pr_size * 1024;
        this.residentSetSize = info.pr_rssize * 1024;
        this.residentSetSizePrivate = info.pr_rssizepriv * 1024;
        this.startTime = info.pr_start.toMillis();
        long elapsedTime = now - this.startTime;
        this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
        this.kernelTime = 0L;
        this.userTime = info.pr_time.toMillis();
        this.commandLineBackup = PsInfoFFM.bytesToString(info.pr_psargs);
        String[] parts = ParseUtil.whitespaces.split(commandLineBackup);
        this.path = parts.length > 0 ? parts[0] : "";
        this.name = this.path.substring(this.path.lastIndexOf('/') + 1);
        if (usage != null) {
            this.userTime = usage.pr_utime.toMillis();
            this.kernelTime = usage.pr_stime.toMillis();
            this.majorFaults = usage.pr_majf;
            this.minorFaults = usage.pr_minf;
            this.voluntaryContextSwitches = usage.pr_vctx;
            this.involuntaryContextSwitches = usage.pr_ictx;
        }
        return true;
    }

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

    private long getProcessOpenFileLimit(final long processId, final int index) {
        final List<String> output = ExecutingCommand.runNative("plimit " + processId);
        if (output.isEmpty()) {
            return -1;
        }
        final Optional<String> nofilesLine = output.stream().filter(line -> line.trim().startsWith("nofiles"))
                .findFirst();
        if (!nofilesLine.isPresent()) {
            return -1;
        }
        final String[] split = nofilesLine.get().split("\\D+");
        return ParseUtil.parseLongOrDefault(split[index], -1);
    }
}
