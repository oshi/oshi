/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.Printer;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;
import oshi.hardware.platform.unix.UnixDisplay;
import oshi.hardware.platform.unix.UnixPrinter;

/**
 * LinuxHardwareAbstractionLayer class.
 */
@ThreadSafe
public abstract class LinuxHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new LinuxComputerSystem();
    }

    @Override
    public GlobalMemory createMemory() {
        return new LinuxGlobalMemory();
    }

    @Override
    public CentralProcessor createProcessor() {
        return new LinuxCentralProcessor();
    }

    @Override
    public Sensors createSensors() {
        return new LinuxSensors();
    }

    @Override
    public List<Display> getDisplays() {
        return UnixDisplay.getDisplays();
    }

    public List<SoundCard> getSoundCards() {
        return LinuxSoundCard.getSoundCards();
    }

    @Override
    public List<GraphicsCard> getGraphicsCards() {
        return LinuxGraphicsCard.getGraphicsCards();
    }

    @Override
    public List<Printer> getPrinters() {
        return UnixPrinter.getPrinters();
    }
}
