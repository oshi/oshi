/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.BluetoothDevice;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;

/**
 * MacHardwareAbstractionLayer class.
 */
@ThreadSafe
public abstract class MacHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    /**
     * Default constructor.
     */
    protected MacHardwareAbstractionLayer() {
    }

    @Override
    public List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        return MacLogicalVolumeGroup.getLogicalVolumeGroups();
    }

    @Override
    protected List<SoundCard> createSoundCards() {
        return MacSoundCard.getSoundCards();
    }

    @Override
    public List<BluetoothDevice> getBluetoothDevices() {
        return MacBluetoothDevice.getBluetoothDevices();
    }
}
