/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.solaris;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.solaris.SolarisPsInfo;
import oshi.driver.unix.ProcAddressSpaceReader;
import oshi.jna.platform.unix.SolarisLibc;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;

/**
 * JNA-backed reader of a Solaris process's argument list and environment from {@code /proc/<pid>/as}.
 * <p>
 * The {@code psinfo}/{@code lwpsinfo}/{@code usage} structures are parsed by the shared
 * {@link oshi.driver.common.unix.solaris.PsInfo} driver; only the address-space read (which needs libc
 * {@code open}/{@code pread}/{@code close}) lives here.
 */
@ThreadSafe
public final class PsInfoJNA {
    private static final Logger LOG = LoggerFactory.getLogger(PsInfoJNA.class);

    private static final SolarisLibc LIBC = SolarisLibc.INSTANCE;

    private static final long PAGE_SIZE = ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("pagesize"),
            4096L);

    private PsInfoJNA() {
    }

    /**
     * Reads the {@code pr_argc}, {@code pr_argv}, {@code pr_envp}, and {@code pr_dmodel} fields from a populated
     * {@link SolarisPsInfo}.
     *
     * @param pid    The process ID
     * @param psinfo A populated {@link SolarisPsInfo} containing the offset pointers for these fields
     * @return A quartet containing the argc, argv, envp and dmodel values, or null if unable to read
     */
    public static Quartet<Integer, Long, Long, Byte> queryArgsEnvAddrs(int pid, SolarisPsInfo psinfo) {
        if (psinfo != null) {
            int argc = psinfo.pr_argc;
            // Must have at least one argc (the command itself) so failure here means exit
            if (argc > 0) {
                long argv = psinfo.pr_argv;
                long envp = psinfo.pr_envp;
                // Process data model 1 = 32 bit, 2 = 64 bit
                byte dmodel = psinfo.pr_dmodel;
                // Sanity check
                if (dmodel * 4 == (envp - argv) / (argc + 1)) {
                    return new Quartet<>(argc, argv, envp, dmodel);
                }
                LOG.trace("Failed data model and offset increment sanity check: dm={} diff={}", dmodel, envp - argv);
                return null;
            }
            LOG.trace("Failed argc sanity check: argc={}", argc);
            return null;
        }
        LOG.trace("Failed to read psinfo file for pid: {} ", pid);
        return null;
    }

    /**
     * Read the argument and environment strings from process address space
     *
     * @param pid    the process id
     * @param psinfo A populated {@link SolarisPsInfo} containing the offset pointers for these fields
     * @return A pair containing a list of the arguments and a map of environment variables
     */
    public static Pair<List<String>, Map<String, String>> queryArgsEnv(int pid, SolarisPsInfo psinfo) {
        List<String> args = new ArrayList<>();
        Map<String, String> env = new LinkedHashMap<>();

        // Get the arg count and list of env vars
        Quartet<Integer, Long, Long, Byte> addrs = queryArgsEnvAddrs(pid, psinfo);
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
                long increment = addrs.getD() * 4L;

                // Read the pointers to the arg strings. We know argc so we can count them.
                long[] argp = new long[argc];
                long offset = argv;
                for (int i = 0; i < argc; i++) {
                    argp[i] = reader.readPointer(offset, increment);
                    offset += increment;
                }

                // Also read the pointers to the env strings. We don't know how many, so stop at the null pointer.
                List<Long> envPtrList = new ArrayList<>();
                offset = envp;
                long addr = 0;
                int limit = 500; // sane max env strings to stop at
                do {
                    addr = reader.readPointer(offset, increment);
                    if (addr != 0) {
                        envPtrList.add(addr);
                    }
                    offset += increment;
                } while (addr != 0 && --limit > 0);

                // Now read the arg strings
                for (int i = 0; i < argp.length && argp[i] != 0; i++) {
                    String argStr = reader.readString(argp[i]);
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
