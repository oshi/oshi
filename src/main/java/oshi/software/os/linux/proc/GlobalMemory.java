/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2015 The Oshi Project Team
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.linux.proc;

import java.io.IOException;
import java.util.List;

import oshi.hardware.Memory;
import oshi.software.os.linux.Libc;
import oshi.software.os.linux.Libc.Sysinfo;
import oshi.util.FileUtil;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;

/**
 * Memory obtained by /proc/meminfo and sysinfo.totalram
 * 
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public class GlobalMemory implements Memory {

	private long totalMemory = 0;

	@Override
	public long getAvailable() {
		long availableMemory = 0;
		List<String> memInfo = null;
		try {
			memInfo = FileUtil.readFile("/proc/meminfo");
		} catch (IOException e) {
			System.err.println("Problem with: /proc/meminfo");
			System.err.println(e.getMessage());
			return availableMemory;
		}
		for (String checkLine : memInfo) {
			// If we have MemAvailable, it trumps all. See code in
			// https://git.kernel.org/cgit/linux/kernel/git/torvalds/
			// linux.git/commit/?id=34e431b0ae398fc54ea69ff85ec700722c9da773
			if (checkLine.startsWith("MemAvailable:")) {
				String[] memorySplit = checkLine.split("\\s+");
				availableMemory = parseMeminfo(memorySplit);
				break;
			} else
			// Otherwise we combine MemFree + Active(file), Inactive(file), and
			// SReclaimable. Free+cached is no longer appropriate. MemAvailable
			// reduces these values using watermarks to estimate when swapping
			// is prevented, omitted here for simplicity (assuming 0 swap).
			if (checkLine.startsWith("MemFree:")) {
				String[] memorySplit = checkLine.split("\\s+");
				availableMemory += parseMeminfo(memorySplit);
			} else if (checkLine.startsWith("Active(file):")) {
				String[] memorySplit = checkLine.split("\\s+");
				availableMemory += parseMeminfo(memorySplit);
			} else if (checkLine.startsWith("Inactive(file):")) {
				String[] memorySplit = checkLine.split("\\s+");
				availableMemory += parseMeminfo(memorySplit);
			} else if (checkLine.startsWith("SReclaimable:")) {
				String[] memorySplit = checkLine.split("\\s+");
				availableMemory += parseMeminfo(memorySplit);
			}
		}
		return availableMemory;
	}

	@Override
	public long getTotal() {
		if (this.totalMemory == 0) {
			Sysinfo info = new Sysinfo();
			if (0 != Libc.INSTANCE.sysinfo(info))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			this.totalMemory = info.totalram.longValue() * info.mem_unit;
		}
		return this.totalMemory;
	}

	private long parseMeminfo(String[] memorySplit) {
		if (memorySplit.length < 2) {
			return 0l;
		}
		long memory = new Long(memorySplit[1]).longValue();
		if (memorySplit.length > 2 && memorySplit[2].equals("kB")) {
			memory *= 1024;
		}
		return memory;
	}
}
