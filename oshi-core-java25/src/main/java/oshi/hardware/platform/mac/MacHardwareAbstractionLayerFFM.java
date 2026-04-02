/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.ComputerSystem;
import oshi.hardware.GlobalMemory;
import oshi.hardware.Sensors;

@ThreadSafe
public final class MacHardwareAbstractionLayerFFM extends MacHardwareAbstractionLayer {
    @Override
    public GlobalMemory createMemory() {
        return new MacGlobalMemoryFFM();
    }

    @Override
    public ComputerSystem createComputerSystem() {
        return new MacComputerSystemFFM();
    }

    @Override
    public Sensors createSensors() {
        return new MacSensorsFFM();
    }
}
