/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.aix;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.aix.AixLwpsInfo;
import oshi.driver.common.unix.aix.AixPsInfo;
import oshi.driver.common.unix.aix.PsInfo;
import oshi.software.common.os.unix.AbstractProcOSProcess;
import oshi.software.os.OSThread;
import oshi.util.Constants;
import oshi.util.ParseUtil;
import oshi.util.UserGroupInfo;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;

/**
 * Abstract base for AIX OSProcess. The {@code /proc} parsing, command-line/env memoization, thread enumeration, and
 * field assignment are shared (via {@link AbstractProcOSProcess}); concrete subclasses (JNA/FFM) provide the perfstat
 * lookup, the {@code rlimit} read, the affinity supplier, and the actual {@code queryArgsEnv} address-space read.
 */
@ThreadSafe
public abstract class AixOSProcess extends AbstractProcOSProcess {

    private final Supplier<AixPsInfo> psinfo = memoize(this::queryPsInfoMemo, defaultExpiration());

    protected AixOSProcess(int pid) {
        super(pid);
    }

    private AixPsInfo queryPsInfoMemo() {
        return PsInfo.queryPsInfo(this.getProcessID());
    }

    @Override
    protected Pair<List<String>, Map<String, String>> queryCommandlineEnvironment() {
        return queryArgsEnv(getProcessID(), psinfo.get());
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
    protected OSThread createThread(int lwpid) {
        return new AixOSThread(getProcessID(), lwpid);
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
