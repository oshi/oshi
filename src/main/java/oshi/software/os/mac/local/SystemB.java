/*
 * Copyright (c) Daniel Widdis, 2015
 * widdis[at]gmail[dot]com
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.mac.local;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

/**
 * Memory and CPU stats from vm_stat and sysctl
 * 
 * @author widdis[at]gmail[dot]com
 */
public interface SystemB extends Library {
	// TODO: Submit this class to JNA
	SystemB INSTANCE = (SystemB) Native.loadLibrary("System", SystemB.class);

	// host_statistics()
	static int HOST_LOAD_INFO = 1;// System loading stats
	static int HOST_VM_INFO = 2; // Virtual memory stats
	static int HOST_CPU_LOAD_INFO = 3;// CPU load stats

	// host_statistics64()
	static int HOST_VM_INFO64 = 4; // 64-bit virtual memory stats
	static int HOST_EXTMOD_INFO64 = 5;// External modification stats
	static int HOST_EXPIRED_TASK_INFO = 6; // Statistics for expired tasks

	// sysctl()
	static int CTL_KERN = 1; // "high kernel": proc, limits
	static int KERN_OSVERSION = 65;

	static int CTL_HW = 6; // generic cpu/io
	static int HW_MEMSIZE = 24; // uint64_t: physical ram size
	static int HW_LOGICALCPU = 103;
	static int HW_LOGICALCPU_MAX = 104;
	static int HW_CPU64BIT_CAPABLE = 107;

	static int CTL_MACHDEP = 7; // machine dependent
	static int MACHDEP_CPU = 102; // cpu
	static int MACHDEP_CPU_VENDOR = 103;
	static int MACHDEP_CPU_BRAND_STRING = 104;
	static int MACHDEP_CPU_FAMILY = 105;
	static int MACHDEP_CPU_MODEL = 106;
	static int MACHDEP_CPU_STEPPING = 109;

	// host_cpu_load_info()
	static int CPU_STATE_MAX = 4;
	static int CPU_STATE_USER = 0;
	static int CPU_STATE_SYSTEM = 1;
	static int CPU_STATE_IDLE = 2;
	static int CPU_STATE_NICE = 3;

	// Data size
	static int UINT64_SIZE = Native.getNativeSize(long.class);
	static int INT_SIZE = Native.getNativeSize(int.class);

	public static class HostCpuLoadInfo extends Structure {
		public int cpu_ticks[] = new int[CPU_STATE_MAX];
	}

	public static class HostLoadInfo extends Structure {
		public int[] avenrun = new int[3]; // scaled by LOAD_SCALE
		public int[] mach_factor = new int[3]; // scaled by LOAD_SCALE
	}

	public static class VMStatistics extends Structure {
		public int free_count; // # of pages free
		public int active_count; // # of pages active
		public int inactive_count; // # of pages inactive
		public int wire_count; // # of pages wired down
		public int zero_fill_count; // # of zero fill pages
		public int reactivations; // # of pages reactivated
		public int pageins; // # of pageins
		public int pageouts; // # of pageouts
		public int faults; // # of faults
		public int cow_faults; // # of copy-on-writes
		public int lookups; // object cache lookups
		public int hits; // object cache hits
		public int purgeable_count; // # of pages purgeable
		public int purges; // # of pages purged
		// # of pages speculative (included in free_count)
		public int speculative_count;
	}

	public static class VMStatistics64 extends Structure {
		public int free_count; // # of pages free
		public int active_count; // # of pages active
		public int inactive_count; // # of pages inactive
		public int wire_count; // # of pages wired down
		public long zero_fill_count; // # of zero fill pages
		public long reactivations; // # of pages reactivated
		public long pageins; // # of pageins
		public long pageouts; // # of pageouts
		public long faults; // # of faults
		public long cow_faults; // # of copy-on-writes
		public long lookups; // object cache lookups
		public long hits; // object cache hits
		public long purges; // # of pages purged
		public int purgeable_count; // # of pages purgeable
		// # of pages speculative (included in free_count)
		public int speculative_count;
		public long decompressions; // # of pages decompressed
		public long compressions; // # of pages compressed
		// # of pages swapped in (via compression segments)
		public long swapins;
		// # of pages swapped out (via compression segments)
		public long swapouts;
		// # of pages used by the compressed pager to hold all the
		// compressed data
		public int compressor_page_count;
		public int throttled_count; // # of pages throttled
		// # of pages that are file-backed (non-swap)
		public int external_page_count;
		public int internal_page_count; // # of pages that are anonymous
		// # of pages (uncompressed) held within the compressor.
		public long total_uncompressed_pages_in_compressor;
	}

	int mach_host_self();

	int host_page_size(int machPort, LongByReference pPageSize);

	int host_statistics(int machPort, int hostStat, Object stats,
			IntByReference count);

	int host_statistics64(int machPort, int hostStat, Object stats,
			IntByReference count);

	int sysctl(int[] name, int namelen, Pointer oldp, IntByReference oldlenp,
			Pointer newp, int newlen);

	int sysctlbyname(String name, Pointer oldp, IntByReference oldlenp,
			Pointer newp, int newlen);
}
