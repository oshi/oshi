/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.solaris;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.tuples.Pair;

/**
 * FFM-backed reader of a Solaris process's argument list and environment.
 * <p>
 * The {@code psinfo}/{@code lwpsinfo}/{@code usage} structures are parsed by the shared
 * {@link oshi.driver.common.unix.solaris.PsInfo} driver. The arg/env scan that the JNA driver performs via
 * {@code pread} on {@code /proc/<pid>/as} is replaced here with {@code pargs -e}, which is simpler and avoids needing a
 * libc binding for {@code open}/{@code close}/{@code pread}. The functional result is the same (the same argv/envp the
 * kernel exposed), at the cost of forking a child process per query.
 */
@ThreadSafe
public final class PsInfoFFM {

    private PsInfoFFM() {
    }

    /**
     * Reads the argument list and environment via {@code pargs -e}.
     *
     * @param pid the process ID
     * @return a pair of (arg list, env map). Either may be empty if {@code pargs} fails or has insufficient permission.
     */
    public static Pair<List<String>, Map<String, String>> queryArgsEnv(int pid) {
        List<String> args = new ArrayList<>();
        Map<String, String> env = new LinkedHashMap<>();
        // `pargs -ae <pid>` emits one line per arg/env entry:
        // argv[0]: /bin/foo
        // envp[0]: FOO=bar
        List<String> lines = ExecutingCommand.runNative("pargs -ae " + pid);
        for (String line : lines) {
            String s = line.trim();
            if (s.startsWith("argv[")) {
                int colon = s.indexOf(": ");
                if (colon > 0 && colon + 2 < s.length()) {
                    args.add(s.substring(colon + 2));
                }
            } else if (s.startsWith("envp[")) {
                int colon = s.indexOf(": ");
                if (colon > 0 && colon + 2 < s.length()) {
                    String entry = s.substring(colon + 2);
                    int eq = entry.indexOf('=');
                    if (eq > 0) {
                        env.put(entry.substring(0, eq), entry.substring(eq + 1));
                    }
                }
            }
        }
        return new Pair<>(args, env);
    }
}
