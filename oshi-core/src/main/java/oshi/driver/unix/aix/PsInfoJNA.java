/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.aix.AixPsInfo;
import oshi.driver.common.unix.aix.PsInfo;
import oshi.driver.unix.ProcAddressSpaceReader;
import oshi.jna.platform.unix.AixLibc;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * JNA-backed driver for the {@code queryArgsEnv} address-space read on AIX. The pure-Java parsing of
 * {@code /proc/<pid>/psinfo} and {@code /proc/<pid>/lwp/<tid>/lwpsinfo} lives in {@link PsInfo} (oshi-common).
 */
@ThreadSafe
public final class PsInfoJNA {

    private static final AixLibc LIBC = AixLibc.INSTANCE;

    // AIX has multiple page size units, but for purposes of "pages" in perfstat,
    // the docs specify 4KB pages so we hardcode this
    private static final long PAGE_SIZE = 4096L;

    private PsInfoJNA() {
    }

    /**
     * Reads the argument and environment strings from process address space.
     *
     * @param pid    the process id
     * @param psinfo a populated {@link AixPsInfo} containing the offset pointers
     * @return a pair of (argv list, env map)
     */
    public static Pair<List<String>, Map<String, String>> queryArgsEnv(int pid, AixPsInfo psinfo) {
        List<String> args = new ArrayList<>();
        Map<String, String> env = new LinkedHashMap<>();

        // Get the arg count and list of env vars
        Triplet<Integer, Long, Long> addrs = PsInfo.queryArgsEnvAddrs(pid, psinfo);
        if (addrs != null) {
            // Open the address space for reading (buffered a page at a time)
            try (ProcAddressSpaceReader reader = ProcAddressSpaceReader.open(LIBC, pid, PAGE_SIZE)) {
                if (reader == null) {
                    return new Pair<>(args, env);
                }
                // Non-null addrs means argc > 0
                int argc = addrs.getA();
                long argv = addrs.getB();
                long envp = addrs.getC();

                // We need to determine if the process is 32-bit or 64-bit data model.
                long increment;
                Path p = Paths.get("/proc/" + pid + "/status");
                try {
                    byte[] status = Files.readAllBytes(p);
                    increment = status[17] == 1 ? 8 : 4;
                } catch (IOException e) {
                    return new Pair<>(args, env);
                }

                // Read the pointers to the arg strings. On AIX argv points to the pointer array, so dereference once.
                long[] argPtr = new long[argc];
                long argp = reader.readPointer(argv, increment);
                if (argp > 0) {
                    for (int i = 0; i < argc; i++) {
                        argPtr[i] = reader.readPointer(argp + i * increment, increment);
                    }
                }

                // Also read the pointers to the env strings; the envp table is null-terminated,
                // so stop at the first null entry (with a 500-entry safety cap).
                List<Long> envPtrList = new ArrayList<>();
                long addr = reader.readPointer(envp, increment);
                int limit = 500;
                long offset = addr;
                while (addr != 0 && --limit > 0) {
                    long envPtr = reader.readPointer(offset, increment);
                    if (envPtr == 0) {
                        break;
                    }
                    envPtrList.add(envPtr);
                    offset += increment;
                }

                // Now read the arg strings
                for (int i = 0; i < argPtr.length && argPtr[i] != 0; i++) {
                    String argStr = reader.readString(argPtr[i]);
                    if (!argStr.isEmpty()) {
                        args.add(argStr);
                    }
                }

                // And now read the env strings
                for (Long envPtr : envPtrList) {
                    String envStr = reader.readString(envPtr);
                    int idx = envStr.indexOf('=');
                    if (idx > 0) {
                        env.put(envStr.substring(0, idx), envStr.substring(idx + 1));
                    }
                }
            }
        }
        return new Pair<>(args, env);
    }
}
