/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static oshi.util.Memoizer.memoize;
import static oshi.util.Memoizer.slowExpiration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.UsbDevice;

/**
 * Common fields or methods used by platform-specific implementations of HardwareAbstractionLayer
 */
@ThreadSafe
public abstract class AbstractHardwareAbstractionLayer implements HardwareAbstractionLayer {

    /**
     * Default constructor.
     */
    protected AbstractHardwareAbstractionLayer() {
    }

    private final Supplier<ComputerSystem> computerSystem = memoize(this::createComputerSystem);

    private final Supplier<CentralProcessor> processor = memoize(this::createProcessor);

    private final Supplier<GlobalMemory> memory = memoize(this::createMemory);

    private final Supplier<Sensors> sensors = memoize(this::createSensors);

    private final Supplier<List<Display>> displays = memoize(this::createDisplays, slowExpiration());

    private final Supplier<List<SoundCard>> soundCards = memoize(this::createSoundCards, slowExpiration());

    private final Supplier<List<GraphicsCard>> graphicsCards = memoize(this::createGraphicsCards, slowExpiration());

    private final Supplier<List<UsbDevice>> usbDevicesTree = memoize(this::createUsbDevices, slowExpiration());

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
    public List<Display> getDisplays() {
        return displays.get();
    }

    /**
     * Instantiates the platform-specific list of {@link Display} objects
     *
     * @return platform-specific list of {@link Display} objects
     */
    protected abstract List<Display> createDisplays();

    @Override
    public List<SoundCard> getSoundCards() {
        return soundCards.get();
    }

    /**
     * Instantiates the platform-specific list of {@link SoundCard} objects
     *
     * @return platform-specific list of {@link SoundCard} objects
     */
    protected abstract List<SoundCard> createSoundCards();

    @Override
    public List<GraphicsCard> getGraphicsCards() {
        return graphicsCards.get();
    }

    /**
     * Instantiates the platform-specific list of {@link GraphicsCard} objects
     *
     * @return platform-specific list of {@link GraphicsCard} objects
     */
    protected abstract List<GraphicsCard> createGraphicsCards();

    @Override
    public List<UsbDevice> getUsbDevices(boolean tree) {
        List<UsbDevice> devices = usbDevicesTree.get();
        if (tree) {
            return devices;
        }
        List<UsbDevice> deviceList = new ArrayList<>();
        for (UsbDevice device : devices) {
            AbstractUsbDevice.addDevicesToList(deviceList, device.getConnectedDevices());
        }
        return deviceList;
    }

    /**
     * Instantiates the platform-specific list of {@link UsbDevice} objects in tree form
     *
     * @return platform-specific list of {@link UsbDevice} objects
     */
    protected abstract List<UsbDevice> createUsbDevices();

    @Override
    public List<NetworkIF> getNetworkIFs() {
        return getNetworkIFs(false);
    }
}
