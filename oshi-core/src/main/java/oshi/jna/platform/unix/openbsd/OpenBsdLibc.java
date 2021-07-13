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
package oshi.jna.platform.unix.openbsd;

import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

import oshi.jna.platform.unix.CLibrary;

/**
 * C library. This class should be considered non-API as it may be removed
 * if/when its code is incorporated into the JNA project.
 */
public interface OpenBsdLibc extends CLibrary {
    OpenBsdLibc INSTANCE = Native.load(null, OpenBsdLibc.class);

    int CTL_KERN = 1; // "high kernel": proc, limits
    int CTL_VM = 1; // "high kernel": proc, limits
    int CTL_HW = 6; // generic cpu/io
    int CTL_MACHDEP = 7; // machine dependent
    int CTL_VFS = 10; // VFS sysctl's

    int KERN_OSTYPE = 1; // string: system version
    int KERN_OSRELEASE = 2; // string: system release
    int KERN_OSREV = 3; // int: system revision
    int KERN_VERSION = 4; // string: compile time info
    int KERN_MAXVNODES = 5; // int: max vnodes
    int KERN_MAXPROC = 6; // int: max processes
    int KERN_ARGMAX = 8; // int: max arguments to exec
    int KERN_CPTIME = 40; // array: cp_time
    int KERN_CPTIME2 = 71; // array: cp_time2

    int VM_UVMEXP = 4; // struct uvmexp

    int HW_MACHINE = 1; // string: machine class
    int HW_MODEL = 2; // string: specific machine model
    int HW_PAGESIZE = 7; // int: software page size
    int HW_CPUSPEED = 12; // get CPU frequency
    int HW_NCPUFOUND = 21; // CPU found (includes offline)
    int HW_SMT = 24; // enable SMT/HT/CMT
    int HW_NCPUONLINE = 25; // number of cpus being used

    int VFS_GENERIC = 0; // generic filesystem information
    int VFS_BCACHESTAT = 3; // struct: buffer cache statistics given as next argument

    /*
     * CPU state indices
     */
    int CPUSTATES = 5;
    int CP_USER = 0;
    int CP_NICE = 1;
    int CP_SYS = 2;
    int CP_INTR = 3; // 4 on 6.4 and later
    int CP_IDLE = 4; // 5 on 6.4 and later

    int UINT64_SIZE = Native.getNativeSize(long.class);
    int INT_SIZE = Native.getNativeSize(int.class);

    /**
     * OpenBSD Cache stats for memory
     */
    @FieldOrder({ "numbufs", "numbufpages", "numdirtypages", "numcleanpages", "pendingwrites", "pendingreads",
            "numwrites", "numreads", "cachehits", "busymapped", "dmapages", "highpages", "delwribufs", "kvaslots",
            "kvaslots_avail", "highflips", "highflops", "dmaflips" })
    class Bcachestats extends Structure {
        public long numbufs; // number of buffers allocated
        public long numbufpages; // number of pages in buffer cache
        public long numdirtypages; // number of dirty free pages
        public long numcleanpages; // number of clean free pages
        public long pendingwrites; // number of pending writes
        public long pendingreads; // number of pending reads
        public long numwrites; // total writes started
        public long numreads; // total reads started
        public long cachehits; // total reads found in cache
        public long busymapped; // number of busy and mapped buffers
        public long dmapages; // dma reachable pages in buffer cache
        public long highpages; // pages above dma region
        public long delwribufs; // delayed write buffers
        public long kvaslots; // kva slots total
        public long kvaslots_avail; // available kva slots
        public long highflips; // total flips to above DMA
        public long highflops; // total failed flips to above DMA
        public long dmaflips; // total flips from high to DMA

        public Bcachestats(Pointer p) {
            super(p);
            read();
        }
    }

    /**
     * Return type for BSD sysctl kern.boottime
     */
    @FieldOrder({ "tv_sec", "tv_usec" })
    class Timeval extends Structure {
        public long tv_sec; // seconds
        public long tv_usec; // microseconds
    }
}
