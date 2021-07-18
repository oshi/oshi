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
package oshi.driver.unix.solaris;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
import com.sun.jna.platform.unix.LibCAPI.ssize_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.solaris.SolarisLibc;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;

/**
 * Utility to query /proc/psinfo
 */
@ThreadSafe
public final class PsInfo {
    private static final Logger LOG = LoggerFactory.getLogger(PsInfo.class);

    private static final boolean IS_LITTLE_ENDIAN = "little".equals(System.getProperty("sun.cpu.endian"));

    private static final SolarisLibc LIBC = SolarisLibc.INSTANCE;

    private static final long PAGE_SIZE = ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("pagesize"),
            4096L);

    enum LwpsInfoT {
        PR_FLAG(4), // lwp flags (DEPRECATED: see below)
        PR_LWPID(4), // lwp id
        PR_ADDR(Native.POINTER_SIZE), // DEPRECATED was internal address of lwp
        PR_WCHAN(Native.POINTER_SIZE), // DEPRECATED was wait addr for sleeping lwp
        PR_STYPE(1), // synchronization event type
        PR_STATE(1), // numeric lwp state
        PR_SNAME(1), // printable character for pr_state
        PR_NICE(1), // nice for cpu usage
        PR_SYSCALL(2), // system call number (if in syscall)
        PR_OLDPRI(1), // pre-SVR4, low value is high priority
        PR_CPU(1), // pre-SVR4, cpu usage for scheduling
        PR_PRI(4), // priority, high value = high priority
        PR_PCTCPU(2), // % of recent cpu time used by this lwp
        PAD(2), // align to 8 byte boundary
        PR_START(2 * NativeLong.SIZE), // lwp start time, from the epoch
        PR_TIME(2 * NativeLong.SIZE), // cpu time for this lwp
        PR_CLNAME(8), // scheduling class name
        PR_NAME(16), // name of system lwp
        PR_ONPRO(4), // processor which last ran this lwp
        PR_BINDPRO(4), // processor to which lwp is bound
        PR_BINDPSET(4), // processor set to which lwp is bound
        PR_LGRP(4), // home lgroup
        PR_LAST_ONPROC(8), // Timestamp of when thread last ran on a processor
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
        PR_FLAG(4), // process flags (DEPRECATED: see below)
        PR_NLWP(4), // number of active lwps in the process
        PR_NZOMB(4), // number of zombie lwps in the process
        PR_PID(4), // process id
        PR_PPID(4), // process id of parent
        PR_PGID(4), // process id of process group leader
        PR_SID(4), // session id
        PR_UID(4), // real user id
        PR_EUID(4), // effective user id
        PR_GID(4), // real group id
        PR_EGID(4), // effective group id
        PAD1(Native.POINTER_SIZE - 4), // align to 8 byte on 64 bit only
        PR_ADDR(Native.POINTER_SIZE), // DEPRECATED was address of process
        PR_SIZE(Native.SIZE_T_SIZE), // size of process image in Kbytes
        PR_RSSIZE(Native.SIZE_T_SIZE), // resident set size in Kbytes
        PR_TTYDEV(NativeLong.SIZE), // controlling tty device (or PRNODEV)
        PR_PCTCPU(2), // % of recent cpu time used by all lwps
        PR_PCTMEM(2), // % of system memory used by process
        PAD2(Native.POINTER_SIZE - 4), // align to 8 byte on 64 bit only
        PR_START(2 * NativeLong.SIZE), // process start time, from the epoch
        PR_TIME(2 * NativeLong.SIZE), // cpu time for this process
        PR_CTIME(2 * NativeLong.SIZE), // cpu time for reaped children
        PR_FNAME(16), // name of exec'ed file
        PR_PSARGS(80), // initial characters of arg list
        PR_WSTAT(4), // if zombie, the wait() status
        PR_ARGC(4), // initial argument count
        PR_ARGV(Native.POINTER_SIZE), // address of initial argument vector
        PR_ENVP(Native.POINTER_SIZE), // address of initial environment vector
        PR_DMODEL(1), // data model of the process
        PAD3(7), // align to 8 byte
        PR_LWP(lwpsInfoOffsets.get(LwpsInfoT.SIZE)), // information for representative lwp
        PR_TASKID(4), // task id
        PR_PROJID(4), // project id
        PR_POOLID(4), // pool id
        PR_ZONEID(4), // zone id
        PR_CONTRACT(4), // process contract id
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
     * Reads the pr_argc, pr_argv, pr_envp, and pr_dmodel fields from
     * /proc/pid/psinfo
     *
     * @param pid
     *            The process ID
     * @return A quartet containing the argc, argv, envp and dmodel values, or null
     *         if unable to read
     */
    public static Quartet<Integer, Long, Long, Byte> queryArgsEnvAddrs(int pid) {
        File procpsinfo = new File("/proc/" + pid + "/psinfo");
        try (RandomAccessFile psinfo = new RandomAccessFile(procpsinfo, "r");
                FileChannel chan = psinfo.getChannel();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int bufferSize = psInfoOffsets.get(PsInfoT.SIZE);
            if (bufferSize > chan.size()) {
                bufferSize = (int) chan.size();
            }

            ByteBuffer buf = ByteBuffer.allocate(bufferSize);
            if (chan.read(buf) > psInfoOffsets.get(PsInfoT.PR_DMODEL)) {
                // We've read bytes up to at least the dmodel
                if (IS_LITTLE_ENDIAN) {
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                }
                // Now grab the elements we want
                int argc = buf.getInt(psInfoOffsets.get(PsInfoT.PR_ARGC));
                // Must have at least one argc (the command itself) so failure here means exit
                if (argc > 0) {
                    long argv = Native.POINTER_SIZE == 8 ? buf.getLong(psInfoOffsets.get(PsInfoT.PR_ARGV))
                            : buf.getInt(psInfoOffsets.get(PsInfoT.PR_ARGV));
                    long envp = Native.POINTER_SIZE == 8 ? buf.getLong(psInfoOffsets.get(PsInfoT.PR_ENVP))
                            : buf.getInt(psInfoOffsets.get(PsInfoT.PR_ENVP));
                    // Process data model 1 = 32 bit, 2 = 64 bit
                    byte dmodel = buf.get(psInfoOffsets.get(PsInfoT.PR_DMODEL));
                    // Sanity check
                    if (dmodel * 4 != (envp - argv) / (argc + 1)) {
                        LOG.trace("Failed data model and offset increment sanity check: dm={} diff={}", dmodel,
                                envp - argv);
                        return null;
                    }
                    return new Quartet<>(argc, argv, envp, dmodel);
                }
                LOG.trace("No permission to read file: {} ", procpsinfo);
            }
        } catch (IOException e) {
            LOG.debug("Failed to read file: {} ", procpsinfo);
        }
        return null;
    }

    /**
     * Read the argument and environment strings from process address space
     *
     * @param pid
     *            the process id
     * @return A pair containing a list of the arguments and a map of environment
     *         variables
     */
    public static Pair<List<String>, Map<String, String>> queryArgsEnv(int pid) {
        List<String> args = new ArrayList<>();
        Map<String, String> env = new LinkedHashMap<>();

        // Get the arg count and list of env vars
        Quartet<Integer, Long, Long, Byte> addrs = queryArgsEnvAddrs(pid);
        if (addrs != null) {
            // Open a file descriptor to the address space
            String procas = "/proc/" + pid + "/as";
            int fd = LIBC.open(procas, 0);
            if (fd < 0) {
                LOG.trace("No permission to read file: {} ", procas);
                return new Pair<>(args, env);
            }
            try {
                // Non-null addrs means argc > 0
                int argc = addrs.getA();
                long argv = addrs.getB();
                long envp = addrs.getC();
                long increment = addrs.getD() * 4L;

                // Reusable buffer
                long bufStart = 0;
                Memory buffer = new Memory(PAGE_SIZE * 2);
                size_t bufSize = new size_t(buffer.size());

                // Read the pointers to the arg strings
                // We know argc so we can count them
                long[] argp = new long[argc];
                long offset = argv;
                for (int i = 0; i < argc; i++) {
                    bufStart = conditionallyReadBufferFromStartOfPage(fd, buffer, bufSize, bufStart, offset);
                    argp[i] = bufStart == 0 ? 0 : getOffsetFromBuffer(buffer, offset - bufStart, increment);
                    offset += increment;
                }

                // Also read the pointers to the env strings
                // We don't know how many, so stop when we get to null pointer
                List<Long> envPtrList = new ArrayList<>();
                offset = envp;
                long addr = 0;
                int limit = 500; // sane max env strings to stop at
                do {
                    bufStart = conditionallyReadBufferFromStartOfPage(fd, buffer, bufSize, bufStart, offset);
                    addr = bufStart == 0 ? 0 : getOffsetFromBuffer(buffer, offset - bufStart, increment);
                    if (addr != 0) {
                        envPtrList.add(addr);
                    }
                    offset += increment;
                } while (addr != 0 && --limit > 0);

                // Now read the arg strings from the buffer
                for (int i = 0; i < argp.length && argp[i] != 0; i++) {
                    bufStart = conditionallyReadBufferFromStartOfPage(fd, buffer, bufSize, bufStart, argp[i]);
                    if (bufStart != 0) {
                        String argStr = buffer.getString(argp[i] - bufStart);
                        if (!argStr.isEmpty()) {
                            args.add(argStr);
                        }
                    }
                }

                // And now read the env strings from the buffer
                for (Long envPtr : envPtrList) {
                    bufStart = conditionallyReadBufferFromStartOfPage(fd, buffer, bufSize, bufStart, envPtr);
                    if (bufStart != 0) {
                        String envStr = buffer.getString(envPtr - bufStart);
                        int idx = envStr.indexOf('=');
                        if (idx > 0) {
                            env.put(envStr.substring(0, idx), envStr.substring(idx + 1));
                        }
                    }
                }
            } finally {
                LIBC.close(fd);
            }
        }
        return new Pair<>(args, env);
    }

    /**
     * Reads the page containing addr into buffer, unless the buffer already
     * contains that page (as indicated by the bufStart address), in which case
     * nothing is changed.
     *
     * @param fd
     *            The file descriptor for the address space
     * @param buffer
     *            An allocated buffer, possibly with data reread from bufStart
     * @param bufSize
     *            The size of the buffer
     * @param bufStart
     *            The start of data currently in bufStart, or 0 if uninitialized
     * @param addr
     *            THe address whose page to read into the buffer
     * @return The new starting pointer for the buffer
     */
    private static long conditionallyReadBufferFromStartOfPage(int fd, Memory buffer, size_t bufSize, long bufStart,
            long addr) {
        // If we don't have the right buffer, update it
        if (addr < bufStart || addr - bufStart > PAGE_SIZE) {
            long newStart = Math.floorDiv(addr, PAGE_SIZE) * PAGE_SIZE;
            ssize_t result = LIBC.pread(fd, buffer, bufSize, new NativeLong(newStart));
            // May return less than asked but should be at least a full page
            if (result.longValue() < PAGE_SIZE) {
                LOG.debug("Failed to read page from address space: {} bytes read", result.longValue());
                return 0;
            }
            return newStart;
        }
        return bufStart;
    }

    private static long getOffsetFromBuffer(Memory buffer, long offset, long increment) {
        return increment == 8 ? buffer.getLong(offset) : buffer.getInt(offset);
    }
}
