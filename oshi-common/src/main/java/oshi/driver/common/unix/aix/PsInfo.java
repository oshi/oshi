/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.aix;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FileUtil;
import oshi.util.tuples.Triplet;

/**
 * Shared utility to query the pure-Java parts of AIX {@code /proc/<pid>/psinfo} and
 * {@code /proc/<pid>/lwp/<tid>/lwpsinfo}.
 * <p>
 * The address-space reads {@code queryArgsEnv} performs need libc {@code open}/{@code pread} and live in the JNA
 * ({@code PsInfoJNA}) and FFM ({@code PsInfoFFM}) drivers respectively.
 */
@ThreadSafe
public final class PsInfo {

    private static final Logger LOG = LoggerFactory.getLogger(PsInfo.class);

    private PsInfo() {
    }

    /**
     * Reads {@code /proc/<pid>/psinfo} and returns the parsed structure.
     *
     * @param pid the process ID
     * @return a structure containing information for the requested process
     */
    public static AixPsInfo queryPsInfo(int pid) {
        return new AixPsInfo(FileUtil.readAllBytesAsBuffer(String.format(Locale.ROOT, "/proc/%d/psinfo", pid)));
    }

    /**
     * Reads {@code /proc/<pid>/lwp/<tid>/lwpsinfo} and returns the parsed structure.
     *
     * @param pid the process ID
     * @param tid the thread ID (lwpid)
     * @return a structure containing information for the requested thread
     */
    public static AixLwpsInfo queryLwpsInfo(int pid, int tid) {
        return new AixLwpsInfo(
                FileUtil.readAllBytesAsBuffer(String.format(Locale.ROOT, "/proc/%d/lwp/%d/lwpsinfo", pid, tid)));
    }

    /**
     * Reads the {@code pr_argc}, {@code pr_argv}, and {@code pr_envp} fields from a {@code psinfo_t}.
     *
     * @param pid    the process ID (used for diagnostic logging only)
     * @param psinfo a populated {@link AixPsInfo} containing the offset pointers
     * @return a triplet of {@code (argc, argv, envp)}, or {@code null} if the psinfo is unusable
     */
    public static Triplet<Integer, Long, Long> queryArgsEnvAddrs(int pid, AixPsInfo psinfo) {
        if (psinfo != null) {
            int argc = psinfo.pr_argc;
            // Must have at least one argc (the command itself) so failure here means exit
            if (argc > 0) {
                return new Triplet<>(argc, psinfo.pr_argv, psinfo.pr_envp);
            }
            LOG.trace("Failed argc sanity check: argc={}", argc);
            return null;
        }
        LOG.trace("Failed to read psinfo file for pid: {} ", pid);
        return null;
    }
}
