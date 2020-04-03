/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
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

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import com.sun.jna.Platform; // NOSONAR squid:S1191

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
 * System information. This is the main entry point to Oshi.
 * <p>
 * This object provides getters which instantiate the appropriate
 * platform-specific implementations of {@link oshi.software.os.OperatingSystem}
 * (software) and {@link oshi.hardware.HardwareAbstractionLayer} (hardware).
 */
public class SystemInfo {

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

    private final Supplier<OperatingSystem> os = memoize(this::createOperatingSystem);

    private final Supplier<HardwareAbstractionLayer> hardware = memoize(this::createHardware);

    /**
     * <p>
     * Getter for the field <code>currentPlatformEnum</code>.
     * </p>
     *
     * @return Returns the currentPlatformEnum.
     */
    public static PlatformEnum getCurrentPlatformEnum() {
        return currentPlatformEnum;
    }

    /**
     * Creates a new instance of the appropriate platform-specific
     * {@link oshi.software.os.OperatingSystem}.
     *
     * @return A new instance of {@link oshi.software.os.OperatingSystem}.
     */
    public OperatingSystem getOperatingSystem() {
        return os.get();
    }

    private OperatingSystem createOperatingSystem() {
        switch (currentPlatformEnum) {

        case WINDOWS:
            return new WindowsOperatingSystem();
        case LINUX:
            return new LinuxOperatingSystem();
        case MACOSX:
            return new MacOperatingSystem();
        case SOLARIS:
            return new SolarisOperatingSystem();
        case FREEBSD:
            return new FreeBsdOperatingSystem();
        default:
            throw new UnsupportedOperationException("Operating system not supported: " + Platform.getOSType());
        }
    }

    /**
     * Creates a new instance of the appropriate platform-specific
     * {@link oshi.hardware.HardwareAbstractionLayer}.
     *
     * @return A new instance of {@link oshi.hardware.HardwareAbstractionLayer}.
     */
    public HardwareAbstractionLayer getHardware() {
        return hardware.get();
    }

    private HardwareAbstractionLayer createHardware() {
        switch (currentPlatformEnum) {

        case WINDOWS:
            return new WindowsHardwareAbstractionLayer();
        case LINUX:
            return new LinuxHardwareAbstractionLayer();
        case MACOSX:
            return new MacHardwareAbstractionLayer();
        case SOLARIS:
            return new SolarisHardwareAbstractionLayer();
        case FREEBSD:
            return new FreeBsdHardwareAbstractionLayer();
        default:
            throw new UnsupportedOperationException("Operating system not supported: " + Platform.getOSType());
        }
    }
}
