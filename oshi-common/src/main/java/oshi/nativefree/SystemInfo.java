/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.nativefree;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.common.platform.linux.nativefree.LinuxHardwareAbstractionLayerNF;
import oshi.software.common.os.linux.nativefree.LinuxOperatingSystemNF;
import oshi.software.os.OperatingSystem;
import oshi.spi.SystemInfoProvider;
import oshi.util.PlatformEnum;

/**
 * Native-free {@link SystemInfoProvider} for Linux. Uses only procfs, sysfs, and standard Java APIs — no JNA, no FFM,
 * no {@code --enable-native-access} required.
 * <p>
 * This provider is registered at priority 0 (lowest) and is only selected when no higher-priority provider (JNA or FFM)
 * is available on the classpath.
 *
 * @see oshi.spi.SystemInfoFactory
 */
public class SystemInfo implements SystemInfoProvider {

    private final Supplier<OperatingSystem> os = memoize(LinuxOperatingSystemNF::new);
    private final Supplier<HardwareAbstractionLayer> hardware = memoize(LinuxHardwareAbstractionLayerNF::new);

    /**
     * Creates a new native-free Linux {@code SystemInfo} instance.
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

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean isAvailable() {
        return PlatformEnum.getCurrentPlatform() == PlatformEnum.LINUX;
    }
}
