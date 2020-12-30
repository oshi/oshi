/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
import com.sun.jna.ptr.IntByReference;

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
    int CTL_MACHDEP = 7;// machine dependent
    int CTL_VFS = 10; // VFS sysctl's

    int KERN_OSTYPE = 1; // string: system version
    int KERN_OSRELEASE = 2; // string: system release
    int KERN_OSREV = 3; // int: system revision
    int KERN_VERSION = 4; // string: compile time info
    int KERN_MAXVNODES = 5; // int: max vnodes
    int KERN_MAXPROC = 6; // int: max processes
    int KERN_MAXFILES = 7; // int: max open files
    int KERN_ARGMAX = 8; // int: max arguments to exec
    int KERN_CPTIME = 40; // array: cp_time
    int KERN_CPTIME2 = 71; // array: cp_time2

    int VM_UVMEXP = 4; // struct uvmexp

    int HW_MACHINE = 1; // string: machine class
    int HW_MODEL = 2; // string: specific machine model
    int HW_PAGESIZE = 7; // int: software page size
    int HW_CPUSPEED = 12; // get CPU frequency

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

    @FieldOrder({ "pagesize", "pagemask", "pageshift", "npages", "free", "active", "inactive", "paging", "wired",
            "zeropages", "reserve_pagedaemon", "reserve_kernel", "unused01", "vnodepages", "vtextpages", "freemin",
            "freetarg", "inactarg", "wiredmax", "anonmin", "vtextmin", "vnodemin", "anonminpct", "vtextminpct",
            "vnodeminpct", "nswapdev", "swpages", "swpginuse", "swpgonly", "nswget", "nanon", "unused05", "unused06",
            "faults", "traps", "intrs", "swtch", "softs", "syscalls", "pageins", "unused07", "unused08", "pgswapin",
            "pgswapout", "forks", "forks_ppwait", "forks_sharevm", "pga_zerohit", "pga_zeromiss", "unused09",
            "fltnoram", "fltnoanon", "fltnoamap", "fltpgwait", "fltpgrele", "fltrelck", "fltrelckok", "fltanget",
            "fltanretry", "fltamcopy", "fltnamap", "fltnomap", "fltlget", "fltget", "flt_anon", "flt_acow", "flt_obj",
            "flt_prcopy", "flt_przero", "pdwoke", "pdrevs", "pdswout", "pdfreed", "pdscans", "pdanscan", "pdobscan",
            "pdreact", "pdbusy", "pdpageouts", "pdpending", "pddeact", "unused11", "unused12", "unused13", "fpswtch",
            "kmapent" })
    class Uvmexp extends Structure {
        // vm_page constants
        public int pagesize; // size of a page (PAGE_SIZE): must be power of 2
        public int pagemask; // page mask
        public int pageshift; // page shift

        // vm_page counters
        public int npages; // [I] number of pages we manage
        public int free; // [F] number of free pages
        public int active; // number of active pages
        public int inactive; // number of pages that we free'd but may want back
        public int paging; // number of pages in the process of being paged out
        public int wired; // number of wired pages

        public int zeropages; // [F] number of zero'd pages
        public int reserve_pagedaemon; // [I] # of pages reserved for pagedaemon
        public int reserve_kernel; // [I] # of pages reserved for kernel
        public int unused01; // formerly anonpages
        public int vnodepages; // XXX # of pages used by vnode page cache
        public int vtextpages; // XXX # of pages used by vtext vnodes

        // pageout params
        public int freemin; // min number of free pages
        public int freetarg; // target number of free pages
        public int inactarg; // target number of inactive pages
        public int wiredmax; // max number of wired pages
        public int anonmin; // min threshold for anon pages
        public int vtextmin; // min threshold for vtext pages
        public int vnodemin; // min threshold for vnode pages
        public int anonminpct; // min percent anon pages
        public int vtextminpct;// min percent vtext pages
        public int vnodeminpct;// min percent vnode pages

        // swap
        public int nswapdev; // number of configured swap devices in system
        public int swpages; // [K] number of PAGE_SIZE'ed swap pages
        public int swpginuse; // number of swap pages in use
        public int swpgonly; // [K] number of swap pages in use, not also in RAM
        public int nswget; // number of swap pages moved from disk to RAM
        public int nanon; // XXX number total of anon's in system
        public int unused05; // formerly nanonneeded
        public int unused06; // formerly nfreeanon

        // stat counters
        public int faults; // page fault count
        public int traps; // trap count
        public int intrs; // public interrupt count
        public int swtch; // context switch count
        public int softs; // software public interrupt count
        public int syscalls; // system calls
        public int pageins; // pagein operation count
        // pageouts are in pdpageouts below
        public int unused07; // formerly obsolete_swapins
        public int unused08; // formerly obsolete_swapouts
        public int pgswapin; // pages swapped in
        public int pgswapout; // pages swapped out
        public int forks; // forks
        public int forks_ppwait; // forks where parent waits
        public int forks_sharevm; // forks where vmspace is shared
        public int pga_zerohit; // pagealloc where zero wanted and zero was available
        public int pga_zeromiss; // pagealloc where zero wanted and zero not available
        public int unused09; // formerly zeroaborts

        // fault subcounters
        public int fltnoram; // number of times fault was out of ram
        public int fltnoanon; // number of times fault was out of anons
        public int fltnoamap; // number of times fault was out of amap chunks
        public int fltpgwait; // number of times fault had to wait on a page
        public int fltpgrele; // number of times fault found a released page
        public int fltrelck; // number of times fault relock called
        public int fltrelckok; // number of times fault relock is a success
        public int fltanget; // number of times fault gets anon page
        public int fltanretry; // number of times fault retrys an anon get
        public int fltamcopy; // number of times fault clears "needs copy"
        public int fltnamap; // number of times fault maps a neighbor anon page
        public int fltnomap; // number of times fault maps a neighbor obj page
        public int fltlget; // number of times fault does a locked pgo_get
        public int fltget; // number of times fault does an unlocked get
        public int flt_anon; // number of times fault anon (case 1a)
        public int flt_acow; // number of times fault anon cow (case 1b)
        public int flt_obj; // number of times fault is on object page (2a)
        public int flt_prcopy; // number of times fault promotes with copy (2b)
        public int flt_przero; // number of times fault promotes with zerofill (2b)

        // daemon counters
        public int pdwoke; // number of times daemon woke up
        public int pdrevs; // number of times daemon rev'd clock hand
        public int pdswout; // number of times daemon called for swapout
        public int pdfreed; // number of pages daemon freed since boot
        public int pdscans; // number of pages daemon scanned since boot
        public int pdanscan; // number of anonymous pages scanned by daemon
        public int pdobscan; // number of object pages scanned by daemon
        public int pdreact; // number of pages daemon reactivated since boot
        public int pdbusy; // number of times daemon found a busy page
        public int pdpageouts; // number of times daemon started a pageout
        public int pdpending; // number of times daemon got a pending pagout
        public int pddeact; // number of pages daemon deactivates
        public int unused11; // formerly pdreanon
        public int unused12; // formerly pdrevnode
        public int unused13; // formerly pdrevtext

        public int fpswtch; // FPU context switches
        public int kmapent; // number of kernel map entries

        public Uvmexp(Pointer p) {
            super(p);
            read();
        }
    }

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

    @FieldOrder({ "cpu_ticks" })
    class CpTime extends Structure {
        public long[] cpu_ticks = new long[CPUSTATES];

        public CpTime(Pointer p) {
            super(p);
            read();
        }
    }

    @FieldOrder({ "cpu_ticks" })
    class CpTimeNew extends Structure {
        public long[] cpu_ticks = new long[CPUSTATES + 1];

        public CpTimeNew(Pointer p) {
            super(p);
            read();
        }

    }

    int sysctl(int[] name, int namelen, Pointer oldp, IntByReference oldlenp, Pointer newp, int newlen);
}
