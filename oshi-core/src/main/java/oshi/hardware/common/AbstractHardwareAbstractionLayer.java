/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.Sensors;

/**
 * Common fields or methods used by platform-specific implementations of HardwareAbstractionLayer
 */
@ThreadSafe
public abstract class AbstractHardwareAbstractionLayer implements HardwareAbstractionLayer {

    private final Supplier<ComputerSystem> computerSystem = memoize(this::createComputerSystem);

    private final Supplier<CentralProcessor> processor = memoize(this::createProcessor);

    private final Supplier<GlobalMemory> memory = memoize(this::createMemory);

    private final Supplier<Sensors> sensors = memoize(this::createSensors);

    @Override
    public ComputerSystem getComputerSystem() {
        return computerSystem.get();
    }

    /**
     * Instantiates the platform-specific {@link ComputerSystem} object
     *
     * @return platform-specific {@link ComputerSystem} object
     */
    protected abstract ComputerSystem createComputerSystem();

    @Override
    public CentralProcessor getProcessor() {
        return processor.get();
    }

    /**
     * Instantiates the platform-specific {@link CentralProcessor} object
     *
     * @return platform-specific {@link CentralProcessor} object
     */
    protected abstract CentralProcessor createProcessor();

    @Override
    public GlobalMemory getMemory() {
        return memory.get();
    }

    /**
     * Instantiates the platform-specific {@link GlobalMemory} object
     *
     * @return platform-specific {@link GlobalMemory} object
     */
    protected abstract GlobalMemory createMemory();

    @Override
    public Sensors getSensors() {
        return sensors.get();
    }

    /**
     * Instantiates the platform-specific {@link Sensors} object
     *
     * @return platform-specific {@link Sensors} object
     */
    protected abstract Sensors createSensors();

    @Override
    public List<NetworkIF> getNetworkIFs() {
        return getNetworkIFs(false);
    }
}
