/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi;

import com.sun.jna.Platform;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.windows.WindowsHardwareAbstractionLayer;
import oshi.software.os.windows.WindowsOperatingSystem;

/**
 * Current system information.
 * @author dblock[at]dblock[dot]org
 */
public class SystemInfo {
	private OperatingSystem _os = null;
	private HardwareAbstractionLayer _hardware = null;
	
	/**
	 * Retrieves operating system information.
	 * @return
	 *  Operating system information.
	 */
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
	
	/**
	 * Retrieves hardware information.
	 * @return
	 *  Hardware abstraction layer.
	 */
	public HardwareAbstractionLayer getHardware() {
		if (_hardware == null) {
			if (Platform.isWindows()) {
				_hardware = new WindowsHardwareAbstractionLayer();
			} else {
				throw new RuntimeException("Operating system not supported: " + Platform.getOSType());
			}
		}
		return _hardware;
	}
}
