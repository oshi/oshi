/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;

/**
 * Baseboard data
 */
@Immutable
public abstract class AbstractBaseboard implements Baseboard {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("manufacturer=").append(getManufacturer()).append(", ");
        sb.append("model=").append(getModel()).append(", ");
        sb.append("version=").append(getVersion()).append(", ");
        sb.append("serial number=").append(getSerialNumber());
        return sb.toString();
    }

}
