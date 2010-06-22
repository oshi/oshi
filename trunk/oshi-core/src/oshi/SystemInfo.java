/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi;

import com.sun.jna.Platform;

import oshi.software.os.OperatingSystem;
import oshi.software.os.windows.WindowsOperatingSystem;

/**
 * Current system information.
 * @author dblock[at]dblock[dot]org
 */
public class SystemInfo {
	private OperatingSystem _os;
	
	public OperatingSystem getOperatingSystem() {
		if (_os == null) {
			if (Platform.isWindows()) {
				_os = new WindowsOperatingSystem();
			} else {
				throw new RuntimeException("Operating system not supported: " + Platform.getOSType());
			}
		}
		return _os;
	}
}
