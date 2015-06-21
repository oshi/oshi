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
 * System information. This is the main entry point to Oshi. This object
 * provides getters which instantiate the appropriate platform-specific
 * implementations of {@link OperatingSystem} (software) and
 * {@link HardwareAbstractionLayer} (hardware).
 * 
 * @author dblock[at]dblock[dot]org
 */
public class SystemInfo {
	private OperatingSystem _os = null;
	private HardwareAbstractionLayer _hardware = null;
	private PlatformEnum currentPlatformEnum;

	{
		if (Platform.isWindows())
			this.currentPlatformEnum = PlatformEnum.WINDOWS;
		else if (Platform.isLinux())
			this.currentPlatformEnum = PlatformEnum.LINUX;
		else if (Platform.isMac())
			this.currentPlatformEnum = PlatformEnum.MACOSX;
		else
			this.currentPlatformEnum = PlatformEnum.UNKNOWN;
	}

	/**
	 * Creates a new instance of the appropriate platform-specific
	 * {@link OperatingSystem}.
	 * 
	 * @return A new instance of {@link OperatingSystem}.
	 */
	public OperatingSystem getOperatingSystem() {
		if (this._os == null) {
			switch (this.currentPlatformEnum) {

			case WINDOWS:
				this._os = new WindowsOperatingSystem();
				break;
			case LINUX:
				this._os = new LinuxOperatingSystem();
				break;
			case MACOSX:
				this._os = new MacOperatingSystem();
				break;
			default:
				throw new RuntimeException("Operating system not supported: "
						+ Platform.getOSType());
			}
		}
		return this._os;
	}

	/**
	 * Creates a new instance of the appropriate platform-specific
	 * {@link HardwareAbstractionLayer}.
	 * 
	 * @return A new instance of {@link HardwareAbstractionLayer}.
	 */
	public HardwareAbstractionLayer getHardware() {
		if (this._hardware == null) {
			switch (this.currentPlatformEnum) {

			case WINDOWS:
				this._hardware = new WindowsHardwareAbstractionLayer();
				break;
			case LINUX:
				this._hardware = new LinuxHardwareAbstractionLayer();
				break;
			case MACOSX:
				this._hardware = new MacHardwareAbstractionLayer();
				break;
			default:
				throw new RuntimeException("Operating system not supported: "
						+ Platform.getOSType());
			}
		}
		return this._hardware;
	}
}
