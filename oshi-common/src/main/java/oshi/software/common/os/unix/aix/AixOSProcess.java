/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.aix;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSThread.ThreadFiltering.VALID_THREAD;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.aix.AixLwpsInfo;
import oshi.driver.common.unix.aix.AixPsInfo;
import oshi.driver.common.unix.aix.PsInfo;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.UserGroupInfo;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;

/**
 * Abstract base for AIX OSProcess. The /proc parsing, command-line/env memoization, thread enumeration, and field
 * assignment are shared; concrete subclasses (JNA/FFM) provide the perfstat lookup, the {@code rlimit} read, the
 * affinity supplier, and the actual {@code queryArgsEnv} address-space read.
 */
@ThreadSafe
public abstract class AixOSProcess extends AbstractOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(AixOSProcess.class);

    private final Supplier<Integer> bitnessSupplier = memoize(this::queryBitness);
    private final Supplier<AixPsInfo> psinfo = memoize(this::queryPsInfoMemo, defaultExpiration());
    private final Supplier<String> commandLine = memoize(this::queryCommandLine);
    private final Supplier<Pair<List<String>, Map<String, String>>> cmdEnv = memoize(this::queryCommandlineEnvironment);

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
    private long privateResidentMemory;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private long bytesRead;
    private long bytesWritten;

    protected AixOSProcess(int pid) {
        super(pid);
    }

    private AixPsInfo queryPsInfoMemo() {
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
        return queryArgsEnv(getProcessID(), psinfo.get());
    }

    @Override
    public String getCurrentWorkingDirectory() {
        try {
            String cwdLink = "/proc" + getProcessID() + "/cwd";
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

    /**
     * Allows subclasses to set this process's state to {@link State#INVALID} when their perfstat lookup can't find the
     * process. Mirrors the original pre-split {@code AixOSProcess.updateAttributes()} behavior so a stale
     * {@code perfstat_process_t[]} array that briefly omits the pid leaves the OSProcess marked invalid rather than
     * silently retaining whatever fields were last assigned.
     *
     * @param newState the new state
     */
    protected final void setState(State newState) {
        this.state = newState;
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
        return this.privateResidentMemory;
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
        try (Stream<Path> fd = Files.list(Paths.get("/proc/" + getProcessID() + "/fd"))) {
            return fd.count();
        } catch (IOException e) {
            return 0L;
        }
    }

    @Override
    public int getBitness() {
        return this.bitnessSupplier.get();
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
        long mask = 0L;
        File directory = new File(String.format(Locale.ROOT, "/proc/%d/lwp", getProcessID()));
        File[] numericFiles = directory.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());
        if (numericFiles == null) {
            return mask;
        }
        for (File lwpidFile : numericFiles) {
            int lwpidNum = ParseUtil.parseIntOrDefault(lwpidFile.getName(), 0);
            AixLwpsInfo info = PsInfo.queryLwpsInfo(getProcessID(), lwpidNum);
            if (info != null) {
                mask |= info.pr_bindpro;
            }
        }
        mask &= getCpuAffinityMask();
        return mask;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        File directory = new File(String.format(Locale.ROOT, "/proc/%d/lwp", getProcessID()));
        File[] numericFiles = directory.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());
        if (numericFiles == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(numericFiles).parallel()
                .map(lwpidFile -> (OSThread) new AixOSThread(getProcessID(),
                        ParseUtil.parseIntOrDefault(lwpidFile.getName(), 0)))
                .filter(VALID_THREAD).collect(Collectors.toList());
    }

    /**
     * Applies the perfstat-derived CPU and memory quartet to this process. Concrete subclasses arrange a
     * {@code Quartet&lt;ucpu_time(ms), scpu_time(ms), real_inuse(bytes), proc_real_mem_data+text(bytes)&gt;} from their
     * data source and call this. Returns true on success; sets {@code state = INVALID} and returns false otherwise.
     *
     * @param cpuMem the quartet of (userTime, kernelTime, residentSetSize, privateResidentMemory)
     * @return {@code true} if attributes were updated
     */
    protected boolean updateAttributes(Quartet<Long, Long, Long, Long> cpuMem) {
        AixPsInfo info = psinfo.get();
        if (info == null) {
            this.state = INVALID;
            return false;
        }
        long now = System.currentTimeMillis();
        this.state = AixProcessState.getStateFromOutput((char) info.pr_lwp.pr_sname);
        this.parentProcessID = (int) info.pr_ppid;
        this.userID = Long.toString(info.pr_euid);
        this.user = UserGroupInfo.getUser(this.userID);
        this.groupID = Long.toString(info.pr_egid);
        this.group = UserGroupInfo.getGroupName(this.groupID);
        this.threadCount = info.pr_nlwp;
        this.priority = info.pr_lwp.pr_pri;
        this.virtualSize = info.pr_size * 1024;
        this.residentSetSize = info.pr_rssize * 1024;
        this.startTime = info.pr_start.tv_sec * 1000L + info.pr_start.tv_nsec / 1_000_000L;
        long elapsedTime = now - this.startTime;
        this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
        this.userTime = cpuMem.getA();
        this.kernelTime = cpuMem.getB();
        if (cpuMem.getC() > 0) {
            this.residentSetSize = cpuMem.getC();
            this.privateResidentMemory = cpuMem.getD();
        } else {
            this.privateResidentMemory = this.residentSetSize;
        }
        this.commandLineBackup = nulTerminatedToString(info.pr_psargs);
        this.path = ParseUtil.whitespaces.split(commandLineBackup)[0];
        this.name = this.path.substring(this.path.lastIndexOf('/') + 1);
        if (this.name.isEmpty()) {
            this.name = nulTerminatedToString(info.pr_fname);
        }
        return true;
    }

    /**
     * Decodes a NUL-terminated byte slice as US-ASCII. Mirrors JNA's {@code Native.toString(byte[])}.
     *
     * @param bytes a byte array possibly containing a trailing NUL
     * @return decoded string
     */
    private static String nulTerminatedToString(byte[] bytes) {
        int len = 0;
        while (len < bytes.length && bytes[len] != 0) {
            len++;
        }
        return new String(bytes, 0, len, StandardCharsets.US_ASCII);
    }

    /**
     * Performs the address-space read for command-line arguments and environment variables.
     *
     * @param pid    the process id
     * @param psinfo a populated {@link AixPsInfo}
     * @return (argv list, env map) — may be empty if the address-space cannot be read
     */
    protected abstract Pair<List<String>, Map<String, String>> queryArgsEnv(int pid, AixPsInfo psinfo);

    /**
     * Returns the per-process CPU affinity mask (from {@code PerfstatCpu.queryCpuAffinityMask}).
     *
     * @return affinity mask
     */
    protected abstract long getCpuAffinityMask();
}
