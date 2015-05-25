/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
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

		_si = si;
	}

	public OSNativeSystemInfo(SYSTEM_INFO si) {
		_si = si;
	}

	/**
	 * Number of processors.
	 * 
	 * @return Integer.
	 */
	public int getNumberOfProcessors() {
		return _si.dwNumberOfProcessors.intValue();
	}
}
