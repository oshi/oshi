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

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

/**
 * Windows OS native system information.
 * 
 * @author dblock[at]dblock[dot]org
 */
public class OSNativeSystemInfo {
	private SYSTEM_INFO _si = null;

	public OSNativeSystemInfo() {

		SYSTEM_INFO si = new SYSTEM_INFO();
		Kernel32.INSTANCE.GetSystemInfo(si);

		try {
			IntByReference isWow64 = new IntByReference();
			HANDLE hProcess = Kernel32.INSTANCE.GetCurrentProcess();
			if (Kernel32.INSTANCE.IsWow64Process(hProcess, isWow64)) {
				if (isWow64.getValue() > 0) {
					Kernel32.INSTANCE.GetNativeSystemInfo(si);
				}
			}
		} catch (UnsatisfiedLinkError e) {
			// no WOW64 support
		}

		this._si = si;
	}

	public OSNativeSystemInfo(SYSTEM_INFO si) {
		this._si = si;
	}

	/**
	 * Number of processors.
	 * 
	 * @return Integer.
	 */
	public int getNumberOfProcessors() {
		return this._si.dwNumberOfProcessors.intValue();
	}
}
