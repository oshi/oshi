/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.solaris;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FileUtil;

/**
 * Shared utility to parse the pure-Java parts of Solaris {@code /proc/<pid>/psinfo},
 * {@code /proc/<pid>/lwp/<tid>/lwpsinfo}, and {@code /proc/<pid>/usage}.
 * <p>
 * The argument/environment reads differ between implementations and live in the JNA
 * ({@code oshi.driver.unix.solaris.PsInfoJNA}, via {@code pread} on {@code /proc/<pid>/as}) and FFM
 * ({@code oshi.driver.unix.solaris.PsInfoFFM}, via {@code pargs}) drivers respectively.
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
     * @return populated {@link SolarisPsInfo}, or {@code null} if the file isn't readable
     */
    public static SolarisPsInfo queryPsInfo(int pid) {
        String path = String.format(Locale.ROOT, "/proc/%d/psinfo", pid);
        ByteBuffer buff = FileUtil.readAllBytesAsBuffer(path);
        if (buff == null || buff.remaining() == 0) {
            // Short-lived processes (e.g. test forks) commonly disappear from /proc
            // between enumeration and read. Logging at debug avoids CI noise.
            LOG.debug("psinfo file empty or unreadable for pid {} ({})", pid, path);
            return null;
        }
        int sz = buff.remaining();
        try {
            return new SolarisPsInfo(buff);
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
     * @return populated {@link SolarisLwpsInfo}, or {@code null} if not readable
     */
    public static SolarisLwpsInfo queryLwpsInfo(int pid, int tid) {
        ByteBuffer buff = FileUtil
                .readAllBytesAsBuffer(String.format(Locale.ROOT, "/proc/%d/lwp/%d/lwpsinfo", pid, tid));
        if (buff == null || buff.remaining() == 0) {
            return null;
        }
        try {
            return new SolarisLwpsInfo(buff);
        } catch (RuntimeException e) {
            LOG.debug("Failed to parse lwpsinfo for pid {} tid {}", pid, tid, e);
            return null;
        }
    }

    /**
     * Reads {@code /proc/<pid>/usage}.
     *
     * @param pid the process ID
     * @return populated {@link SolarisPrUsage}, or {@code null} if not readable
     */
    public static SolarisPrUsage queryPrUsage(int pid) {
        ByteBuffer buff = FileUtil.readAllBytesAsBuffer(String.format(Locale.ROOT, "/proc/%d/usage", pid));
        if (buff == null || buff.remaining() == 0) {
            return null;
        }
        try {
            return new SolarisPrUsage(buff);
        } catch (RuntimeException e) {
            LOG.debug("Failed to parse usage for pid {}", pid, e);
            return null;
        }
    }

    /**
     * Reads {@code /proc/<pid>/lwp/<tid>/usage}.
     *
     * @param pid the process ID
     * @param tid the thread ID
     * @return populated {@link SolarisPrUsage}, or {@code null} if not readable
     */
    public static SolarisPrUsage queryPrUsage(int pid, int tid) {
        ByteBuffer buff = FileUtil.readAllBytesAsBuffer(String.format(Locale.ROOT, "/proc/%d/lwp/%d/usage", pid, tid));
        if (buff == null || buff.remaining() == 0) {
            return null;
        }
        try {
            return new SolarisPrUsage(buff);
        } catch (RuntimeException e) {
            LOG.debug("Failed to parse lwp usage for pid {} tid {}", pid, tid, e);
            return null;
        }
    }

    /**
     * Trims a NUL-terminated byte array to a UTF-8 String.
     *
     * @param bytes the buffer
     * @return the trimmed String
     */
    public static String bytesToString(byte[] bytes) {
        int len = 0;
        while (len < bytes.length && bytes[len] != 0) {
            len++;
        }
        return new String(Arrays.copyOf(bytes, len), StandardCharsets.UTF_8);
    }
}
