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
package oshi.software.os.mac.local;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import oshi.hardware.Memory;
import oshi.software.os.mac.local.SystemB.VMStatistics;

/**
 * Memory obtained by host_statistics (vm_stat) and sysctl
 * 
 * @author widdis[at]gmail[dot]com
 */
public class GlobalMemory implements Memory {

	long totalMemory = 0;

	@Override
	public long getAvailable() {
		long availableMemory = 0;
		long pageSize = 4096;

		int machPort = SystemB.INSTANCE.mach_host_self();

		LongByReference pPageSize = new LongByReference();
		if (0 != SystemB.INSTANCE.host_page_size(machPort, pPageSize))
			throw new LastErrorException("Error code: " + Native.getLastError());
		pageSize = pPageSize.getValue();

		VMStatistics vmStats = new VMStatistics();
		if (0 != SystemB.INSTANCE.host_statistics(machPort, SystemB.HOST_VM_INFO, vmStats,
				new IntByReference(vmStats.size() / SystemB.INT_SIZE)))
			throw new LastErrorException("Error code: " + Native.getLastError());
		availableMemory = (vmStats.free_count + vmStats.inactive_count) * pageSize;

		return availableMemory;
	}

	@Override
	public long getTotal() {
		if (this.totalMemory == 0) {
			Pointer pMemSize = new com.sun.jna.Memory(SystemB.UINT64_SIZE);
			if (0 != SystemB.INSTANCE.sysctlbyname("hw.memsize", pMemSize, new IntByReference(SystemB.UINT64_SIZE),
					null, 0))
				throw new LastErrorException("Error code: " + Native.getLastError());
			this.totalMemory = pMemSize.getLong(0);
		}
		return this.totalMemory;
	}
}
