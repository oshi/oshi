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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.NativeLong;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.SystemInfo;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.solaris.SolarisLibc;
import oshi.util.tuples.Triplet;

/**
 * Utility to query /proc/psinfo
 */
@ThreadSafe
public final class PsInfo {
    private static final Logger LOG = LoggerFactory.getLogger(PsInfo.class);

    private static final boolean IS_LITTLE_ENDIAN = "little".equals(System.getProperty("sun.cpu.endian"));

    private static final SolarisLibc LIBC = SolarisLibc.INSTANCE;

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
     * Reads the pr_argc, pr_argv, and pr_envp fields from /proc/pid/psinfo
     *
     * @param pid
     *            The process ID
     * @return A triplet containing the argc, argv, and envp values, or null if
     *         unable to read
     */
    public static Triplet<Integer, Long, Long> queryArgsEnv(int pid) {
        File procpsinfo = new File("/proc/" + pid + "/psinfo");
        try (RandomAccessFile psinfo = new RandomAccessFile(procpsinfo, "r");
                FileChannel chan = psinfo.getChannel();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int bufferSize = psInfoOffsets.get(PsInfoT.SIZE);
            if (bufferSize > chan.size()) {
                bufferSize = (int) chan.size();
            }

            ByteBuffer buf = ByteBuffer.allocate(bufferSize);
            if (chan.read(buf) >= psInfoOffsets.get(PsInfoT.PR_DMODEL)) {
                // We've read bytes up to at least the argc,argv,envp
                if (IS_LITTLE_ENDIAN) {
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                }
                // Now grab the elements we want
                int argc = buf.getInt(psInfoOffsets.get(PsInfoT.PR_ARGC));
                long argv = Native.POINTER_SIZE == 8 ? buf.getLong(psInfoOffsets.get(PsInfoT.PR_ARGV))
                        : buf.getInt(psInfoOffsets.get(PsInfoT.PR_ARGV));
                long envp = Native.POINTER_SIZE == 8 ? buf.getLong(psInfoOffsets.get(PsInfoT.PR_ENVP))
                        : buf.getInt(psInfoOffsets.get(PsInfoT.PR_ENVP));
                return new Triplet<>(argc, argv, envp);
            }
        } catch (IOException e) {
            LOG.debug("Failed to read file: {} ", procpsinfo);
        }
        return null;
    }

    public static List<String> queryArgs(int pid, int argc, long argv) {
        // Open a file descriptor to the address space
        int fd = LIBC.open("/proc/" + pid + "/as", 0);
        // Read the pointers to the arg strings
        long[] argp = new long[argc];
        long offset = argv;
        size_t nbyte = new size_t(Native.POINTER_SIZE);
        Memory buf = new Memory(Native.POINTER_SIZE);
        for (int i = 0; i < argc; i++) {
            LIBC.pread(fd, buf, nbyte, new NativeLong(offset));
            argp[i] = Native.POINTER_SIZE == 8 ? buf.getLong(0) : buf.getInt(0);
            offset += Native.POINTER_SIZE;
            System.out.format("Reading at offset 0x%16x: 0x%16x%n", offset, argp[i]);
        }
        // Now read the strings at the pointers, character by character
        List<String> args = new ArrayList<>(argc);
        buf = new Memory(1);
        nbyte = new size_t(1);
        for (int i = 0; i < argp.length; i++) {
            StringBuilder sb = new StringBuilder();
            LIBC.pread(fd, buf, nbyte, new NativeLong(argp[i]));
            byte b = buf.getByte(0);
            if (b == 0) {
                args.add(sb.toString());
                System.out.format("Reading at offset 0x%16x: %s%n", argp[i], sb.toString());
            } else {
                sb.append((char) b);
            }
        }
        LIBC.close(fd);
        return args;
    }

    public static void main(String[] args) {
        int mypid = new SystemInfo().getOperatingSystem().getProcessId();
        Triplet<Integer, Long, Long> vals = queryArgsEnv(mypid);
        System.out.println("ARGC: " + vals.getA());
        System.out.format("ARGV: 0x%16x%n", vals.getB());
        System.out.format("ENVP: 0x%16x%n", vals.getC());
        System.out.println("ARGS: ");
        for (String s : queryArgs(mypid, vals.getA(), vals.getB())) {
            System.out.println(s);
        }
    }
}
