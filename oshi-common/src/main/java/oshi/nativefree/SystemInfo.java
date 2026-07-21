/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.nativefree;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.common.platform.linux.nativefree.LinuxHardwareAbstractionLayerNF;
import oshi.hardware.common.platform.unix.netbsd.NetBsdHardwareAbstractionLayer;
import oshi.software.common.os.linux.nativefree.LinuxOperatingSystemNF;
import oshi.software.common.os.unix.netbsd.NetBsdOperatingSystem;
import oshi.software.os.OperatingSystem;
import oshi.spi.SystemInfoProvider;
import oshi.util.PlatformEnum;

/**
 * Native-free {@link SystemInfoProvider} for Linux and NetBSD. Uses only procfs, sysfs, {@code sysctl}, other
 * command-line tools, and standard Java APIs — no JNA, no FFM, no {@code --enable-native-access} required.
 * <p>
 * On Linux this wires the native-free ({@code *NF}) subclasses that read {@code /proc} and {@code /sys}. On NetBSD the
 * shared {@code oshi-common} base classes are already native-free (they query {@code sysctl} and {@code ps} via the
 * command line), so they are used directly; the {@code oshi-core} JNA subclasses only swap in native process/thread
 * identity and a JNA-based processor.
 * <p>
 * This provider is registered at priority 0 (lowest) and is only selected when no higher-priority provider (JNA or FFM)
 * is available on the classpath.
 *
 * @see oshi.spi.SystemInfoFactory
 */
public class SystemInfo implements SystemInfoProvider {

    private final Supplier<OperatingSystem> os = memoize(SystemInfo::createOperatingSystem);
    private final Supplier<HardwareAbstractionLayer> hardware = memoize(SystemInfo::createHardware);

    /**
     * Creates a new native-free {@code SystemInfo} instance.
     */
    public SystemInfo() {
    }

    @Override
    public OperatingSystem getOperatingSystem() {
        return os.get();
    }

    @Override
    public HardwareAbstractionLayer getHardware() {
        return hardware.get();
    }

    private static OperatingSystem createOperatingSystem() {
        switch (PlatformEnum.getCurrentPlatform()) {
            case LINUX:
                return new LinuxOperatingSystemNF();
            case NETBSD:
                return new NetBsdOperatingSystem();
            default:
                throw new UnsupportedOperationException(
                        "Native-free operating system not supported: " + PlatformEnum.getCurrentPlatform().getName());
        }
    }

    private static HardwareAbstractionLayer createHardware() {
        switch (PlatformEnum.getCurrentPlatform()) {
            case LINUX:
                return new LinuxHardwareAbstractionLayerNF();
            case NETBSD:
                return new NetBsdHardwareAbstractionLayer();
            default:
                throw new UnsupportedOperationException(
                        "Native-free hardware not supported: " + PlatformEnum.getCurrentPlatform().getName());
        }
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean isAvailable() {
        PlatformEnum platform = PlatformEnum.getCurrentPlatform();
        return platform == PlatformEnum.LINUX || platform == PlatformEnum.NETBSD;
    }
}
