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
package oshi.software.os.windows.nt;

import oshi.hardware.Memory;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinBase.MEMORYSTATUSEX;

/**
 * Memory obtained by GlobalMemoryStatusEx.
 * 
 * @author dblock[at]dblock[dot]org
 */
public class GlobalMemory implements Memory {
	MEMORYSTATUSEX _memory = new MEMORYSTATUSEX();

	public GlobalMemory() {
		if (!Kernel32.INSTANCE.GlobalMemoryStatusEx(_memory)) {
			throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
		}
	}

	public long getAvailable() {
		return _memory.ullAvailPhys.longValue();
	}

	public long getTotal() {
		return _memory.ullTotalPhys.longValue();
	}
}
