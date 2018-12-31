/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
