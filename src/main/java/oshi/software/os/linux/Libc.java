/*
 * Copyright (c) Daniel Widdis, 2015
 * widdis[at]gmail[dot]com
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.linux;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

public interface Libc extends Library {

	public static final Libc INSTANCE = (Libc) Native.loadLibrary("c",
			Libc.class);

	public static final class Sysinfo extends Structure {
		public NativeLong uptime; // Seconds since boot
		// 1, 5, and 15 minute load averages
		public NativeLong[] loads = new NativeLong[3];
		public NativeLong totalram; // Total usable main memory size
		public NativeLong freeram; // Available memory size
		public NativeLong sharedram; // Amount of shared memory
		public NativeLong bufferram; // Memory used by buffers
		public NativeLong totalswap; // Total swap space size
		public NativeLong freeswap; // swap space still available
		public short procs; // Number of current processes
		public NativeLong totalhigh; // Total high memory size
		public NativeLong freehigh; // Available high memory size
		public int mem_unit; // Memory unit size in bytes
		public byte[] _f = new byte[8]; // Won't be written for 64-bit systems
		@Override
		protected List getFieldOrder() {
			return Arrays.asList(new String[] { "uptime", "loads", "totalram", "freeram",
					"sharedram", "bufferram", "totalswap", "freeswap", "procs",
					"totalhigh", "freehigh", "mem_unit", "_f"});
		}
	}

	int sysinfo(Sysinfo info);

}
