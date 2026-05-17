/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import java.util.List;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.BluetoothDevice;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;
import oshi.hardware.common.platform.unix.UnixDisplay;
import oshi.util.driver.linux.DrmEdid;

/**
 * LinuxHardwareAbstractionLayer class.
 */
@ThreadSafe
public abstract class LinuxHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    /**
     * Default constructor.
     */
    protected LinuxHardwareAbstractionLayer() {
    }

    @Override
    public ComputerSystem createComputerSystem() {
        return new LinuxComputerSystem();
    }

    @Override
    public abstract GlobalMemory createMemory();

    @Override
    public abstract CentralProcessor createProcessor();

    @Override
    public Sensors createSensors() {
        return new LinuxSensors();
    }

    @Override
    public List<Display> getDisplays() {
        List<byte[]> edids = DrmEdid.getEdidArrays();
        if (!edids.isEmpty()) {
            return edids.stream().map(UnixDisplay::new).collect(Collectors.toList());
        }
        return UnixDisplay.getDisplays();
    }

    @Override
    public List<SoundCard> getSoundCards() {
        return LinuxSoundCard.getSoundCards();
    }

    @Override
    public List<BluetoothDevice> getBluetoothDevices() {
        return LinuxBluetoothDevice.getBluetoothDevices();
    }

    @Override
    public abstract List<GraphicsCard> getGraphicsCards();
}
