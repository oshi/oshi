/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import oshi.hardware.common.AbstractSensors;

/**
 * Abstract base for the FreeBSD Sensors. The JNA and FFM subclasses supply the coretemp and ACPI sysctl reads.
 */
public abstract class FreeBsdSensors extends AbstractSensors {

    @Override
    public int[] queryFanSpeeds() {
        // Nothing known on FreeBSD for this.
        return new int[0];
    }

    @Override
    public double queryCpuVoltage() {
        // Nothing known on FreeBSD for this.
        return 0d;
    }
}
