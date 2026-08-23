/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.aix;

import java.nio.ByteBuffer;
import java.util.Locale;

import org.jspecify.annotations.Nullable;
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
     * Reads {@code /proc/<pid>/psinfo}.
     *
     * @param pid the process ID
     * @return a structure containing information for the requested process, or {@code null} if the file isn't readable
     */
    public static @Nullable AixPsInfo queryPsInfo(int pid) {
        String path = String.format(Locale.ROOT, "/proc/%d/psinfo", pid);
        ByteBuffer buff = FileUtil.readAllBytesAsBuffer(path);
        if (buff.remaining() == 0) {
            // Short-lived processes commonly disappear from /proc between enumeration and read, so this is expected
            // often enough that it belongs at debug rather than warn.
            LOG.debug("psinfo file empty or unreadable for pid {} ({})", pid, path);
            return null;
        }
        int sz = buff.remaining();
        try {
            return new AixPsInfo(buff);
        } catch (RuntimeException e) {
            LOG.warn("Failed to parse psinfo for pid {} (file size {} bytes)", pid, sz, e);
            return null;
        }
    }

    /**
     * Reads {@code /proc/<pid>/lwp/<tid>/lwpsinfo}.
     *
     * @param pid the process ID
     * @param tid the thread ID (lwpid)
     * @return a structure containing information for the requested thread, or {@code null} if the file isn't readable
     */
    public static @Nullable AixLwpsInfo queryLwpsInfo(int pid, int tid) {
        String path = String.format(Locale.ROOT, "/proc/%d/lwp/%d/lwpsinfo", pid, tid);
        ByteBuffer buff = FileUtil.readAllBytesAsBuffer(path);
        if (buff.remaining() == 0) {
            LOG.debug("lwpsinfo file empty or unreadable for pid {} tid {} ({})", pid, tid, path);
            return null;
        }
        try {
            return new AixLwpsInfo(buff);
        } catch (RuntimeException e) {
            LOG.debug("Failed to parse lwpsinfo for pid {} tid {}", pid, tid, e);
            return null;
        }
    }

    /**
     * Reads the {@code pr_argc}, {@code pr_argv}, and {@code pr_envp} fields from a {@code psinfo_t}.
     *
     * @param pid    the process ID (used for diagnostic logging only)
     * @param psinfo a populated {@link AixPsInfo} containing the offset pointers, or {@code null} if absent
     * @return a triplet of {@code (argc, argv, envp)}, or {@code null} if the psinfo is unusable
     */
    public static @Nullable Triplet<Integer, Long, Long> queryArgsEnvAddrs(int pid, @Nullable AixPsInfo psinfo) {
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
