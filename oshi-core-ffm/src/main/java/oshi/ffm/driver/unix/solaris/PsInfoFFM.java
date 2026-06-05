/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.driver.unix.solaris;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * FFM equivalent of {@link oshi.driver.unix.solaris.PsInfo}. Parses {@code /proc/<pid>/psinfo},
 * {@code /proc/<pid>/lwp/<tid>/lwpsinfo}, and {@code /proc/<pid>/usage} into pure-Java data classes (no JNA structs).
 * <p>
 * The arg/env scan that {@link oshi.driver.unix.solaris.PsInfo#queryArgsEnv} performs via {@code pread} on
 * {@code /proc/<pid>/as} is replaced here with {@code pargs -e}, which is simpler and avoids needing a libc binding for
 * {@code open}/{@code close}/{@code pread}. The functional result is the same (the same argv/envp the kernel exposed),
 * at the cost of forking a child process per query.
 */
@ThreadSafe
public final class PsInfoFFM {

    private static final Logger LOG = LoggerFactory.getLogger(PsInfoFFM.class);

    private PsInfoFFM() {
    }

    /** Pure-Java mirror of {@code struct psinfo_t}, populated from {@code /proc/<pid>/psinfo}. */
    public static final class PsInfo {
        public int pr_flag;
        public int pr_nlwp;
        public int pr_pid;
        public int pr_ppid;
        public int pr_pgid;
        public int pr_sid;
        public int pr_uid;
        public int pr_euid;
        public int pr_gid;
        public int pr_egid;
        public long pr_addr; // user-space pointer as a raw address
        public long pr_size; // Kbytes
        public long pr_rssize;
        public long pr_rssizepriv;
        public long pr_ttydev;
        public short pr_pctcpu;
        public short pr_pctmem;
        public Timestruc pr_start = new Timestruc();
        public Timestruc pr_time = new Timestruc();
        public Timestruc pr_ctime = new Timestruc();
        public byte[] pr_fname = new byte[16];
        public byte[] pr_psargs = new byte[80];
        public int pr_wstat;
        public int pr_argc;
        public long pr_argv; // user-space pointer
        public long pr_envp; // user-space pointer
        public byte pr_dmodel; // 1=32-bit, 2=64-bit
        public int pr_taskid;
        public int pr_projid;
        public int pr_nzomb;
        public int pr_poolid;
        public int pr_zoneid;
        public int pr_contract;
        public LwpsInfo pr_lwp = new LwpsInfo();

        private PsInfo(ByteBuffer buff) {
            this.pr_flag = buff.getInt();
            this.pr_nlwp = buff.getInt();
            this.pr_pid = buff.getInt();
            this.pr_ppid = buff.getInt();
            this.pr_pgid = buff.getInt();
            this.pr_sid = buff.getInt();
            this.pr_uid = buff.getInt();
            this.pr_euid = buff.getInt();
            this.pr_gid = buff.getInt();
            this.pr_egid = buff.getInt();
            this.pr_addr = buff.getLong();
            this.pr_size = buff.getLong();
            this.pr_rssize = buff.getLong();
            this.pr_rssizepriv = buff.getLong();
            this.pr_ttydev = buff.getLong();
            this.pr_pctcpu = buff.getShort();
            this.pr_pctmem = buff.getShort();
            // Align to 8 bytes for following Timestrucs (pad 4 bytes after the two shorts)
            buff.getInt();
            this.pr_start.readFrom(buff);
            this.pr_time.readFrom(buff);
            this.pr_ctime.readFrom(buff);
            buff.get(this.pr_fname);
            buff.get(this.pr_psargs);
            this.pr_wstat = buff.getInt();
            this.pr_argc = buff.getInt();
            this.pr_argv = buff.getLong();
            this.pr_envp = buff.getLong();
            this.pr_dmodel = buff.get();
            // 3-byte pad (pr_pad2)
            buff.get();
            buff.get();
            buff.get();
            this.pr_taskid = buff.getInt();
            this.pr_projid = buff.getInt();
            this.pr_nzomb = buff.getInt();
            this.pr_poolid = buff.getInt();
            this.pr_zoneid = buff.getInt();
            this.pr_contract = buff.getInt();
            buff.getInt(); // pr_filler
            this.pr_lwp.readFrom(buff);
        }
    }

    /** Pure-Java mirror of {@code struct lwpsinfo_t}. */
    public static final class LwpsInfo {
        public int pr_flag;
        public int pr_lwpid;
        public long pr_addr;
        public long pr_wchan;
        public byte pr_stype;
        public byte pr_state;
        public byte pr_sname;
        public byte pr_nice;
        public short pr_syscall;
        public byte pr_oldpri;
        public byte pr_cpu;
        public int pr_pri;
        public short pr_pctcpu;
        public Timestruc pr_start = new Timestruc();
        public Timestruc pr_time = new Timestruc();
        public byte[] pr_clname = new byte[8];
        public byte[] pr_oldname = new byte[16];
        public int pr_onpro;
        public int pr_bindpro;
        public int pr_bindpset;
        public int pr_lgrp;
        public long pr_last_onproc;
        public byte[] pr_name = new byte[32];

        private LwpsInfo() {
        }

        private LwpsInfo(ByteBuffer buff) {
            readFrom(buff);
        }

        void readFrom(ByteBuffer buff) {
            this.pr_flag = buff.getInt();
            this.pr_lwpid = buff.getInt();
            this.pr_addr = buff.getLong();
            this.pr_wchan = buff.getLong();
            this.pr_stype = buff.get();
            this.pr_state = buff.get();
            this.pr_sname = buff.get();
            this.pr_nice = buff.get();
            this.pr_syscall = buff.getShort();
            this.pr_oldpri = buff.get();
            this.pr_cpu = buff.get();
            this.pr_pri = buff.getInt();
            this.pr_pctcpu = buff.getShort();
            buff.getShort(); // pad
            this.pr_start.readFrom(buff);
            this.pr_time.readFrom(buff);
            buff.get(this.pr_clname);
            buff.get(this.pr_oldname);
            this.pr_onpro = buff.getInt();
            this.pr_bindpro = buff.getInt();
            this.pr_bindpset = buff.getInt();
            this.pr_lgrp = buff.getInt();
            this.pr_last_onproc = buff.getLong();
            buff.get(this.pr_name);
        }
    }

    /** Pure-Java mirror of {@code struct prusage_t}. */
    public static final class PrUsage {
        public int pr_lwpid;
        public int pr_count;
        public Timestruc pr_tstamp = new Timestruc();
        public Timestruc pr_create = new Timestruc();
        public Timestruc pr_term = new Timestruc();
        public Timestruc pr_rtime = new Timestruc();
        public Timestruc pr_utime = new Timestruc();
        public Timestruc pr_stime = new Timestruc();
        public Timestruc pr_ttime = new Timestruc();
        public Timestruc pr_tftime = new Timestruc();
        public Timestruc pr_dftime = new Timestruc();
        public Timestruc pr_kftime = new Timestruc();
        public Timestruc pr_ltime = new Timestruc();
        public Timestruc pr_slptime = new Timestruc();
        public Timestruc pr_wtime = new Timestruc();
        public Timestruc pr_stoptime = new Timestruc();
        public long pr_minf;
        public long pr_majf;
        public long pr_vctx;
        public long pr_ictx;

        private PrUsage(ByteBuffer buff) {
            this.pr_lwpid = buff.getInt();
            this.pr_count = buff.getInt();
            this.pr_tstamp.readFrom(buff);
            this.pr_create.readFrom(buff);
            this.pr_term.readFrom(buff);
            this.pr_rtime.readFrom(buff);
            this.pr_utime.readFrom(buff);
            this.pr_stime.readFrom(buff);
            this.pr_ttime.readFrom(buff);
            this.pr_tftime.readFrom(buff);
            this.pr_dftime.readFrom(buff);
            this.pr_kftime.readFrom(buff);
            this.pr_ltime.readFrom(buff);
            this.pr_slptime.readFrom(buff);
            this.pr_wtime.readFrom(buff);
            this.pr_stoptime.readFrom(buff);
            // 6 filler Timestrucs (96 bytes)
            for (int i = 0; i < 6; i++) {
                buff.getLong();
                buff.getLong();
            }
            this.pr_minf = buff.getLong();
            this.pr_majf = buff.getLong();
            buff.getLong(); // pr_nswap
            buff.getLong(); // pr_inblk
            buff.getLong(); // pr_oublk
            buff.getLong(); // pr_msnd
            buff.getLong(); // pr_mrcv
            buff.getLong(); // pr_sigs
            this.pr_vctx = buff.getLong();
            this.pr_ictx = buff.getLong();
            // pr_sysc, pr_ioch, 10 fillers — ignored
        }
    }

    /** Timestruc: tv_sec + tv_nsec, each 8 bytes on 64-bit Solaris. */
    public static final class Timestruc {
        public long tv_sec;
        public long tv_nsec;

        void readFrom(ByteBuffer buff) {
            this.tv_sec = buff.getLong();
            this.tv_nsec = buff.getLong();
        }

        /** Returns this timestamp converted to milliseconds since the epoch. */
        public long toMillis() {
            return tv_sec * 1000L + tv_nsec / 1_000_000L;
        }
    }

    /**
     * Reads {@code /proc/<pid>/psinfo}.
     *
     * @param pid the process ID
     * @return populated {@link PsInfo}, or {@code null} if the file isn't readable
     */
    public static PsInfo queryPsInfo(int pid) {
        ByteBuffer buff = FileUtil.readAllBytesAsBuffer(String.format(Locale.ROOT, "/proc/%d/psinfo", pid));
        if (buff == null || buff.remaining() == 0) {
            return null;
        }
        try {
            return new PsInfo(buff);
        } catch (Throwable t) {
            LOG.debug("Failed to parse psinfo for pid {}", pid, t);
            return null;
        }
    }

    /**
     * Reads {@code /proc/<pid>/lwp/<tid>/lwpsinfo}.
     *
     * @param pid the process ID
     * @param tid the thread ID (lwpid)
     * @return populated {@link LwpsInfo}, or {@code null} if not readable
     */
    public static LwpsInfo queryLwpsInfo(int pid, int tid) {
        ByteBuffer buff = FileUtil
                .readAllBytesAsBuffer(String.format(Locale.ROOT, "/proc/%d/lwp/%d/lwpsinfo", pid, tid));
        if (buff == null || buff.remaining() == 0) {
            return null;
        }
        try {
            return new LwpsInfo(buff);
        } catch (Throwable t) {
            LOG.debug("Failed to parse lwpsinfo for pid {} tid {}", pid, tid, t);
            return null;
        }
    }

    /**
     * Reads {@code /proc/<pid>/usage}.
     *
     * @param pid the process ID
     * @return populated {@link PrUsage}, or {@code null} if not readable
     */
    public static PrUsage queryPrUsage(int pid) {
        ByteBuffer buff = FileUtil.readAllBytesAsBuffer(String.format(Locale.ROOT, "/proc/%d/usage", pid));
        if (buff == null || buff.remaining() == 0) {
            return null;
        }
        try {
            return new PrUsage(buff);
        } catch (Throwable t) {
            LOG.debug("Failed to parse usage for pid {}", pid, t);
            return null;
        }
    }

    /**
     * Reads {@code /proc/<pid>/lwp/<tid>/usage}.
     *
     * @param pid the process ID
     * @param tid the thread ID
     * @return populated {@link PrUsage}, or {@code null} if not readable
     */
    public static PrUsage queryPrUsage(int pid, int tid) {
        ByteBuffer buff = FileUtil.readAllBytesAsBuffer(String.format(Locale.ROOT, "/proc/%d/lwp/%d/usage", pid, tid));
        if (buff == null || buff.remaining() == 0) {
            return null;
        }
        try {
            return new PrUsage(buff);
        } catch (Throwable t) {
            LOG.debug("Failed to parse lwp usage for pid {} tid {}", pid, tid, t);
            return null;
        }
    }

    /**
     * Reads the argument list and environment via {@code pargs -e}. The JNA driver uses {@code pread} on
     * {@code /proc/<pid>/as} directly; the FFM driver substitutes a {@code pargs} invocation for simplicity.
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
        return new String(Arrays.copyOf(bytes, len), java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Pid count via {@code /proc} directory listing.
     *
     * @return the value of {@code wc -l} on a {@code ls /proc} listing, or {@code 0} on failure
     */
    public static int countProcesses() {
        return ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("ls /proc | wc -l").trim(), 0);
    }
}
