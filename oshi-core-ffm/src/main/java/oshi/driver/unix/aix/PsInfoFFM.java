/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.aix.AixPsInfo;
import oshi.driver.common.unix.aix.PsInfo;
import oshi.ffm.platform.unix.aix.AixLibcFunctions;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * FFM-backed driver for the {@code queryArgsEnv} address-space read on AIX. The pure-Java parsing of
 * {@code /proc/<pid>/psinfo} and {@code /proc/<pid>/lwp/<tid>/lwpsinfo} lives in {@link PsInfo} (oshi-common).
 */
@ThreadSafe
public final class PsInfoFFM {

    private static final Logger LOG = LoggerFactory.getLogger(PsInfoFFM.class);

    private static final long PAGE_SIZE = 4096L;

    private PsInfoFFM() {
    }

    /**
     * Reads the argument and environment strings from process address space using FFM-bound libc calls.
     *
     * @param pid    the process id
     * @param psinfo a populated {@link AixPsInfo} containing the offset pointers
     * @return a pair of (argv list, env map)
     */
    public static Pair<List<String>, Map<String, String>> queryArgsEnv(int pid, AixPsInfo psinfo) {
        List<String> args = new ArrayList<>();
        Map<String, String> env = new LinkedHashMap<>();

        Triplet<Integer, Long, Long> addrs = PsInfo.queryArgsEnvAddrs(pid, psinfo);
        if (addrs == null) {
            return new Pair<>(args, env);
        }

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment procasPath = arena.allocateFrom("/proc/" + pid + "/as");
            int fd = AixLibcFunctions.open(procasPath, 0);
            if (fd < 0) {
                LOG.trace("No permission to read file: /proc/{}/as", pid);
                return new Pair<>(args, env);
            }
            try {
                int argc = addrs.getA();
                long argv = addrs.getB();
                long envp = addrs.getC();

                // Data model: 1 = 32-bit, anything else (typically 64-bit) gives 8-byte pointers.
                long increment;
                Path p = Paths.get("/proc/" + pid + "/status");
                try {
                    byte[] status = Files.readAllBytes(p);
                    increment = status[17] == 1 ? 8 : 4;
                } catch (IOException _) {
                    return new Pair<>(args, env);
                }

                MemorySegment buffer = arena.allocate(PAGE_SIZE * 2);

                // Read the pointers to the arg strings
                long bufStart = conditionallyReadPage(fd, buffer, 0, argv);
                long[] argPtr = new long[argc];
                long argp = bufStart == 0 ? 0 : readPointerFromBuffer(buffer, argv - bufStart, increment);
                if (argp > 0) {
                    for (int i = 0; i < argc; i++) {
                        long offset = argp + (long) i * increment;
                        bufStart = conditionallyReadPage(fd, buffer, bufStart, offset);
                        argPtr[i] = bufStart == 0 ? 0 : readPointerFromBuffer(buffer, offset - bufStart, increment);
                    }
                }

                // Also read the pointers to the env strings; stop at first null.
                bufStart = conditionallyReadPage(fd, buffer, bufStart, envp);
                List<Long> envPtrList = new ArrayList<>();
                long addr = bufStart == 0 ? 0 : readPointerFromBuffer(buffer, envp - bufStart, increment);
                int limit = 500;
                long offset = addr;
                while (addr != 0 && --limit > 0) {
                    bufStart = conditionallyReadPage(fd, buffer, bufStart, offset);
                    long envPtr = bufStart == 0 ? 0 : readPointerFromBuffer(buffer, offset - bufStart, increment);
                    if (envPtr != 0) {
                        envPtrList.add(envPtr);
                    } else {
                        break;
                    }
                    offset += increment;
                }

                // Now read the arg strings from the buffer
                for (int i = 0; i < argPtr.length && argPtr[i] != 0; i++) {
                    bufStart = conditionallyReadPage(fd, buffer, bufStart, argPtr[i]);
                    if (bufStart != 0) {
                        String argStr = readStringFromBuffer(buffer, argPtr[i] - bufStart);
                        if (!argStr.isEmpty()) {
                            args.add(argStr);
                        }
                    }
                }

                // And now read the env strings from the buffer
                for (Long envPtr : envPtrList) {
                    bufStart = conditionallyReadPage(fd, buffer, bufStart, envPtr);
                    if (bufStart != 0) {
                        String envStr = readStringFromBuffer(buffer, envPtr - bufStart);
                        int idx = envStr.indexOf('=');
                        if (idx > 0) {
                            env.put(envStr.substring(0, idx), envStr.substring(idx + 1));
                        }
                    }
                }
            } finally {
                AixLibcFunctions.close(fd);
            }
        } catch (Throwable t) {
            LOG.debug("FFM queryArgsEnv failed for pid {}: {}", pid, t.getMessage());
        }
        return new Pair<>(args, env);
    }

    /**
     * Reads the page containing {@code addr} into {@code buffer} unless the buffer already covers it (as indicated by
     * {@code bufStart}). Returns the new buffer start, or 0 on read failure.
     */
    private static long conditionallyReadPage(int fd, MemorySegment buffer, long bufStart, long addr) throws Throwable {
        if (addr < bufStart || addr - bufStart > PAGE_SIZE) {
            long newStart = Math.floorDiv(addr, PAGE_SIZE) * PAGE_SIZE;
            long result = AixLibcFunctions.pread(fd, buffer, buffer.byteSize(), newStart);
            if (result < PAGE_SIZE) {
                LOG.debug("Failed to read page from address space: {} bytes read", result);
                return 0;
            }
            return newStart;
        }
        return bufStart;
    }

    private static long readPointerFromBuffer(MemorySegment buffer, long offset, long increment) {
        return increment == 8 ? buffer.get(JAVA_LONG, offset) : Integer.toUnsignedLong(buffer.get(JAVA_INT, offset));
    }

    /**
     * Reads a NUL-terminated string starting at {@code offset} in the buffer. Falls back to the buffer end if no NUL is
     * found in the buffered window.
     */
    private static String readStringFromBuffer(MemorySegment buffer, long offset) {
        long max = buffer.byteSize();
        if (offset < 0 || offset >= max) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (long i = offset; i < max; i++) {
            byte b = buffer.get(JAVA_BYTE, i);
            if (b == 0) {
                break;
            }
            sb.append((char) (b & 0xFF));
        }
        return sb.toString();
    }
}
