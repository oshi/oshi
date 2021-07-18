/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import static oshi.PlatformEnum.AIX;
import static oshi.PlatformEnum.FREEBSD;
import static oshi.PlatformEnum.LINUX;
import static oshi.PlatformEnum.MACOS;
import static oshi.PlatformEnum.MACOSX;
import static oshi.PlatformEnum.OPENBSD;
import static oshi.PlatformEnum.SOLARIS;
import static oshi.PlatformEnum.UNKNOWN;
import static oshi.PlatformEnum.WINDOWS;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import com.sun.jna.Platform; // NOSONAR squid:S1191

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.platform.linux.LinuxHardwareAbstractionLayer;
import oshi.hardware.platform.mac.MacHardwareAbstractionLayer;
import oshi.hardware.platform.unix.aix.AixHardwareAbstractionLayer;
import oshi.hardware.platform.unix.freebsd.FreeBsdHardwareAbstractionLayer;
import oshi.hardware.platform.unix.openbsd.OpenBsdHardwareAbstractionLayer;
import oshi.hardware.platform.unix.solaris.SolarisHardwareAbstractionLayer;
import oshi.hardware.platform.windows.WindowsHardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.software.os.mac.MacOperatingSystem;
import oshi.software.os.unix.aix.AixOperatingSystem;
import oshi.software.os.unix.freebsd.FreeBsdOperatingSystem;
import oshi.software.os.unix.openbsd.OpenBsdOperatingSystem;
import oshi.software.os.unix.solaris.SolarisOperatingSystem;
import oshi.software.os.windows.WindowsOperatingSystem;

/**
 * System information. This is the main entry point to OSHI.
 * <p>
 * This object provides getters which instantiate the appropriate
 * platform-specific implementations of {@link oshi.software.os.OperatingSystem}
 * (software) and {@link oshi.hardware.HardwareAbstractionLayer} (hardware).
 */
public class SystemInfo {

    // The platform isn't going to change, and making this static enables easy
    // access from outside this class
    private static final PlatformEnum currentPlatform = queryCurrentPlatform();

    private static final String NOT_SUPPORTED = "Operating system not supported: JNA Platform type ";

    private final Supplier<OperatingSystem> os = memoize(SystemInfo::createOperatingSystem);

    private final Supplier<HardwareAbstractionLayer> hardware = memoize(SystemInfo::createHardware);

    /**
     * Create a new instance of {@link SystemInfo}. This is the main entry point to
     * OSHI and provides access to cross-platform code.
     * <p>
     * Platform-specific Hardware and Software objects are retrieved via memoized
     * suppliers. To conserve memory at the cost of additional processing time,
     * create a new version of SystemInfo() for subsequent calls. To conserve
     * processing time at the cost of additional memory usage, re-use the same
     * {@link SystemInfo} object for future queries.
     */
    public SystemInfo() {
        // Intentionally empty, here to enable the constructor javadoc.
        // Trying to access the static currentPlatform variable for OS check caused
        // unexplained problems with initialization.
    }

    /**
     * Gets the {@link PlatformEnum} value representing this system.
     *
     * @return Returns the current platform
     */
    public static PlatformEnum getCurrentPlatform() {
        return currentPlatform;
    }

    private static PlatformEnum queryCurrentPlatform() {
        if (Platform.isWindows()) {
            return WINDOWS;
        } else if (Platform.isLinux()) {
            return LINUX;
        } else if (Platform.isMac()) {
            return MACOS;
        } else if (Platform.isSolaris()) {
            return SOLARIS;
        } else if (Platform.isFreeBSD()) {
            return FREEBSD;
        } else if (Platform.isAIX()) {
            return AIX;
        } else if (Platform.isOpenBSD()) {
            return OPENBSD;
        } else {
            return UNKNOWN;
        }
    }

    /**
     * Gets the {@link PlatformEnum} value representing this system.
     *
     * @return Returns the current platform
     * @deprecated Use {@link #getCurrentPlatform()}
     */
    @Deprecated
    public static PlatformEnum getCurrentPlatformEnum() {
        PlatformEnum platform = getCurrentPlatform();
        return platform.equals(MACOS) ? MACOSX : platform;
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

    private static OperatingSystem createOperatingSystem() {
        switch (currentPlatform) {
        case WINDOWS:
            return new WindowsOperatingSystem();
        case LINUX:
            return new LinuxOperatingSystem();
        case MACOS:
            return new MacOperatingSystem();
        case SOLARIS:
            return new SolarisOperatingSystem();
        case FREEBSD:
            return new FreeBsdOperatingSystem();
        case AIX:
            return new AixOperatingSystem();
        case OPENBSD:
            return new OpenBsdOperatingSystem();
        default:
            throw new UnsupportedOperationException(NOT_SUPPORTED + Platform.getOSType());
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

    private static HardwareAbstractionLayer createHardware() {
        switch (currentPlatform) {
        case WINDOWS:
            return new WindowsHardwareAbstractionLayer();
        case LINUX:
            return new LinuxHardwareAbstractionLayer();
        case MACOS:
            return new MacHardwareAbstractionLayer();
        case SOLARIS:
            return new SolarisHardwareAbstractionLayer();
        case FREEBSD:
            return new FreeBsdHardwareAbstractionLayer();
        case AIX:
            return new AixHardwareAbstractionLayer();
        case OPENBSD:
            return new OpenBsdHardwareAbstractionLayer();
        default:
            throw new UnsupportedOperationException(NOT_SUPPORTED + Platform.getOSType());
        }
    }
}
