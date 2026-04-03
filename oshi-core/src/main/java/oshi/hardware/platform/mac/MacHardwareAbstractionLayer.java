/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.Printer;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;
import oshi.hardware.platform.unix.UnixPrinter;

/**
 * MacHardwareAbstractionLayer class.
 */
@ThreadSafe
public abstract class MacHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    @Override
    public List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        return MacLogicalVolumeGroup.getLogicalVolumeGroups();
    }

    @Override
    public List<SoundCard> getSoundCards() {
        return MacSoundCard.getSoundCards();
    }

    @Override
    public List<Printer> getPrinters() {
        return UnixPrinter.getPrinters();
    }
}
