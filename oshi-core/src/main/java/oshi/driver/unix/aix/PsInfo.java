/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.driver.unix.aix;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory; // NOSONAR squid:S1191
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.aix.AixLibc;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Utility to query /proc/psinfo
 */
@ThreadSafe
public final class PsInfo {
    private static final Logger LOG = LoggerFactory.getLogger(PsInfo.class);

    private static final AixLibc LIBC = AixLibc.INSTANCE;

    enum LwpsInfoT {
        PR_LWPID(8), // thread id
        PR_ADDR(8), // internal address of thread
        PR_WCHAN(8), // wait addr for sleeping thread
        PR_FLAG(4), // thread flags
        PR_WTYPE(1), // type of thread wait
        PR_STATE(1), // numeric scheduling state
        PR_SNAME(1), // printable character representing pr_state
        PR_NICE(1), // nice for cpu usage
        PR_PRI(4), // priority, high value = high priority
        PR_POLICY(4), // scheduling policy
        PR_CLNAME(8), // printable character representing pr_policy
        PR_ONPRO(Native.POINTER_SIZE), // processor on which thread last ran
        PR_BINDPRO(Native.POINTER_SIZE), // processor to which thread is bound
        SIZE(0);

        private int size;

        LwpsInfoT(int bytes) {
            size = bytes;
        }

        public int size() {
            return size;
        }
    }

    private static Map<LwpsInfoT, Integer> lwpsInfoOffsets = initLwpsOffsets();

    enum PsInfoT {
        PR_FLAG(4), // process flags from proc struct p_flag
        PR_FLAG2(4), // process flags from proc struct p_flag2 *
        PR_NLWP(4), // number of threads in process
        PR_PAD1(4), // reserved for future use
        PR_UID(8), // real user id
        PR_EUID(8), // effective user id
        PR_GID(8), // real group id
        PR_EGID(8), // effective group id
        PR_PID(8), // unique process id
        PR_PPID(8), // process id of parent
        PR_PGID(8), // pid of process group leader
        PR_SID(8), // session id
        PR_TTYDEV(8), // controlling tty device
        PR_ADDR(8), // internal address of proc struct
        PR_SIZE(8), // process image size in kb (1024) units
        PR_RSSIZE(8), // resident set size in kb (1024) units
        PR_START(16), // process start time, time since epoch
        PR_TIME(16), // usr+sys cpu time for this process
        PR_CID(2), // corral id
        PR_PAD2(2), // reserved for future use
        PR_ARGC(4), // initial argument count
        PR_ARGV(8), // address of initial argument vector in user process
        PR_ENVP(8), // address of initial environment vector in user process
        PR_FNAME(16), // last component of exec()ed pathname
        PR_PSARGS(80), // initial characters of arg list
        PR_PAD(64), // reserved for future use
        PR_LWP(lwpsInfoOffsets.get(LwpsInfoT.SIZE)), // "representative" thread info
        SIZE(0);

        private int size;

        PsInfoT(int bytes) {
            size = bytes;
        }

        public int size() {
            return size;
        }
    }

    private static Map<PsInfoT, Integer> psInfoOffsets = initPsOffsets();

    private PsInfo() {
    }

    private static Map<LwpsInfoT, Integer> initLwpsOffsets() {
        Map<LwpsInfoT, Integer> infoMap = new EnumMap<>(LwpsInfoT.class);
        int offset = 0;
        for (LwpsInfoT field : LwpsInfoT.values()) {
            infoMap.put(field, offset);
            offset += field.size;
        }
        return infoMap;
    }

    private static Map<PsInfoT, Integer> initPsOffsets() {
        Map<PsInfoT, Integer> infoMap = new EnumMap<>(PsInfoT.class);
        int offset = 0;
        for (PsInfoT field : PsInfoT.values()) {
            infoMap.put(field, offset);
            offset += field.size;
        }
        return infoMap;
    }

    /**
     * Reads the pr_argc, pr_argv, and pr_envp fields from /proc/pid/psinfo
     *
     * @param pid
     *            The process ID
     * @return A triplet containing the argc, argv, and envp values, or null if
     *         unable to read
     */
    public static Triplet<Integer, Long, Long> queryArgsEnvAddrs(int pid) {
        File procpsinfo = new File("/proc/" + pid + "/psinfo");
        try (RandomAccessFile psinfo = new RandomAccessFile(procpsinfo, "r");
                FileChannel chan = psinfo.getChannel();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int bufferSize = psInfoOffsets.get(PsInfoT.SIZE);
            if (bufferSize > chan.size()) {
                bufferSize = (int) chan.size();
            }

            ByteBuffer buf = ByteBuffer.allocate(bufferSize);
            if (chan.read(buf) >= psInfoOffsets.get(PsInfoT.PR_FNAME)) {
                // We've read bytes up to at least the argc,argv,envp
                // Now grab the elements we want
                int argc = buf.getInt(psInfoOffsets.get(PsInfoT.PR_ARGC));
                long argv = buf.getLong(psInfoOffsets.get(PsInfoT.PR_ARGV));
                long envp = buf.getLong(psInfoOffsets.get(PsInfoT.PR_ENVP));
                return new Triplet<>(argc, argv, envp);
            }
        } catch (IOException e) {
            LOG.debug("Failed to read file: {} ", procpsinfo);
        }
        return null;
    }

    public static Pair<List<String>, Map<String, String>> queryArgsEnv(int pid) {
        List<String> args = new ArrayList<>();
        Map<String, String> env = new LinkedHashMap<>();

        // Get the arg count and list of env vars
        Triplet<Integer, Long, Long> addrs = queryArgsEnvAddrs(pid);
        if (addrs != null) {
            int argc = addrs.getA();
            long argv = addrs.getB();
            long envp = addrs.getC();

            // Open a file descriptor to the address space
            int fd = LIBC.open("/proc/" + pid + "/as", 0);
            try {

                // Read the pointers to the arg strings
                // We know argc so we can count them
                long[] argp = new long[argc];
                long offset = argv;
                size_t nbyte = new size_t(Native.POINTER_SIZE);
                Memory buf = new Memory(Native.POINTER_SIZE);
                for (int i = 0; i < argc; i++) {
                    LIBC.pread(fd, buf, nbyte, new NativeLong(offset));
                    argp[i] = Native.POINTER_SIZE == 8 ? buf.getLong(0) : buf.getInt(0);
                    offset += Native.POINTER_SIZE;
                }

                // Now read the strings at the pointers, character by character
                size_t cbyte = new size_t(1);
                Memory cbuf = new Memory(1);
                for (int i = 0; i < argp.length; i++) {
                    StringBuilder sb = new StringBuilder();
                    long c = 0; // character offset
                    byte b = 0;
                    do {
                        LIBC.pread(fd, cbuf, cbyte, new NativeLong(argp[i] + c++));
                        b = cbuf.getByte(0);
                        if (b != 0) {
                            sb.append((char) b);
                        }
                    } while (b != 0 && c < 9999); // null terminated with sanity check
                    args.add(sb.toString());
                }

                // Now read the pointers to the env strings
                // We don't know how many, so stop when we get to null pointer
                offset = envp;
                long addr = 0;
                do {
                    LIBC.pread(fd, buf, nbyte, new NativeLong(offset));
                    addr = Native.POINTER_SIZE == 8 ? buf.getLong(0) : buf.getInt(0);
                    // Non-null addr points to the env string. Read char by char into key and value
                    if (addr != 0) {
                        StringBuilder key = new StringBuilder();
                        StringBuilder value = new StringBuilder();
                        boolean rhs = false; // left and right of = delimiter
                        long c = 0; // character index
                        byte b = 0;
                        do {
                            LIBC.pread(fd, cbuf, cbyte, new NativeLong(addr + c++));
                            b = cbuf.getByte(0);
                            if (b != 0) {
                                if (rhs) {
                                    value.append((char) b);
                                } else if (b == '=') {
                                    rhs = true;
                                } else {
                                    key.append((char) b);
                                }
                            }
                        } while (b != 0 && c < 9999); // null terminated with sanity check
                        env.put(key.toString(), value.toString());
                    }
                    offset += Native.POINTER_SIZE;
                } while (addr != 0);
            } finally {
                LIBC.close(fd);
            }
        }
        return new Pair<>(args, env);
    }
}
