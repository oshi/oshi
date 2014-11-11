/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.linux.LinuxHardwareAbstractionLayer;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.software.os.mac.MacHardwareAbstractionLayer;
import oshi.software.os.mac.MacOperatingSystem;
import oshi.software.os.windows.WindowsHardwareAbstractionLayer;
import oshi.software.os.windows.WindowsOperatingSystem;

import com.sun.jna.Platform;

/**
 * Current system information.
 * 
 * @author dblock[at]dblock[dot]org
 */
public class SystemInfo {
	private OperatingSystem _os = null;
	private HardwareAbstractionLayer _hardware = null;
	private PlatformEnum currentPlatformEnum;

	{
		if (Platform.isWindows())
			currentPlatformEnum = PlatformEnum.WINDOWS;
		else if (Platform.isLinux())
			currentPlatformEnum = PlatformEnum.LINUX;
		else if (Platform.isMac())
			currentPlatformEnum = PlatformEnum.MACOSX;
		else
			currentPlatformEnum = PlatformEnum.UNKNOWN;
	}

	/**
	 * Retrieves operating system information.
	 * 
	 * @return Operating system information.
	 */
	public OperatingSystem getOperatingSystem() {
		if (_os == null) {
			switch (currentPlatformEnum) {

			case WINDOWS:
				_os = new WindowsOperatingSystem();
				break;
			case LINUX:
				_os = new LinuxOperatingSystem();
				break;
			case MACOSX:
				_os = new MacOperatingSystem();
				break;
			default:
				throw new RuntimeException("Operating system not supported: "
						+ Platform.getOSType());
			}
		}
		return _os;
	}

	/**
	 * Retrieves hardware information.
	 * 
	 * @return Hardware abstraction layer.
	 */
	public HardwareAbstractionLayer getHardware() {
		if (_hardware == null) {
			switch (currentPlatformEnum) {

			case WINDOWS:
				_hardware = new WindowsHardwareAbstractionLayer();
				break;
			case LINUX:
				_hardware = new LinuxHardwareAbstractionLayer();
				break;
			case MACOSX:
				_hardware = new MacHardwareAbstractionLayer();
				break;
			default:
				throw new RuntimeException("Operating system not supported: "
						+ Platform.getOSType());
			}
		}
		return _hardware;
	}
}
