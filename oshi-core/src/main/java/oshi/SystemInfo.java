/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi;

import java.io.Serializable;

import com.sun.jna.Platform;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.platform.linux.LinuxHardwareAbstractionLayer;
import oshi.hardware.platform.mac.MacHardwareAbstractionLayer;
import oshi.hardware.platform.unix.freebsd.FreeBsdHardwareAbstractionLayer;
import oshi.hardware.platform.unix.solaris.SolarisHardwareAbstractionLayer;
import oshi.hardware.platform.windows.WindowsHardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.software.os.mac.MacOperatingSystem;
import oshi.software.os.unix.freebsd.FreeBsdOperatingSystem;
import oshi.software.os.unix.solaris.SolarisOperatingSystem;
import oshi.software.os.windows.WindowsOperatingSystem;

/**
 * System information. This is the main entry point to Oshi. This object
 * provides getters which instantiate the appropriate platform-specific
 * implementations of {@link OperatingSystem} (software) and
 * {@link HardwareAbstractionLayer} (hardware).
 *
 * @author dblock[at]dblock[dot]org
 */
public class SystemInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private OperatingSystem os = null;

    private HardwareAbstractionLayer hardware = null;

    // The platform isn't going to change, and making this static enables easy
    // access from outside this class
    private static final PlatformEnum currentPlatformEnum;

    static {
        if (Platform.isWindows()) {
            currentPlatformEnum = PlatformEnum.WINDOWS;
        } else if (Platform.isLinux()) {
            currentPlatformEnum = PlatformEnum.LINUX;
        } else if (Platform.isMac()) {
            currentPlatformEnum = PlatformEnum.MACOSX;
        } else if (Platform.isSolaris()) {
            currentPlatformEnum = PlatformEnum.SOLARIS;
        } else if (Platform.isFreeBSD()) {
            currentPlatformEnum = PlatformEnum.FREEBSD;
        } else {
            currentPlatformEnum = PlatformEnum.UNKNOWN;
        }
    }

    /**
     * @return Returns the currentPlatformEnum.
     */
    public static PlatformEnum getCurrentPlatformEnum() {
        return currentPlatformEnum;
    }

    /**
     * Creates a new instance of the appropriate platform-specific
     * {@link OperatingSystem}.
     *
     * @return A new instance of {@link OperatingSystem}.
     */
    public OperatingSystem getOperatingSystem() {
        if (this.os == null) {
            switch (currentPlatformEnum) {

            case WINDOWS:
                this.os = new WindowsOperatingSystem();
                break;
            case LINUX:
                this.os = new LinuxOperatingSystem();
                break;
            case MACOSX:
                this.os = new MacOperatingSystem();
                break;
            case SOLARIS:
                this.os = new SolarisOperatingSystem();
                break;
            case FREEBSD:
                this.os = new FreeBsdOperatingSystem();
                break;
            default:
                throw new UnsupportedOperationException("Operating system not supported: " + Platform.getOSType());
            }
        }
        return this.os;
    }

    /**
     * Creates a new instance of the appropriate platform-specific
     * {@link HardwareAbstractionLayer}.
     *
     * @return A new instance of {@link HardwareAbstractionLayer}.
     */
    public HardwareAbstractionLayer getHardware() {
        if (this.hardware == null) {
            switch (currentPlatformEnum) {

            case WINDOWS:
                this.hardware = new WindowsHardwareAbstractionLayer();
                break;
            case LINUX:
                this.hardware = new LinuxHardwareAbstractionLayer();
                break;
            case MACOSX:
                this.hardware = new MacHardwareAbstractionLayer();
                break;
            case SOLARIS:
                this.hardware = new SolarisHardwareAbstractionLayer();
                break;
            case FREEBSD:
                this.hardware = new FreeBsdHardwareAbstractionLayer();
                break;
            default:
                throw new UnsupportedOperationException("Operating system not supported: " + Platform.getOSType());
            }
        }
        return this.hardware;
    }
}
