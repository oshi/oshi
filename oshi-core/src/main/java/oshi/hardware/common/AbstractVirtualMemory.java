/*
 * Copyright 2019-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.VirtualMemory;
import oshi.util.FormatUtil;

/**
 * Virtual Memory info.
 */
@ThreadSafe
public abstract class AbstractVirtualMemory implements VirtualMemory {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Swap Used/Avail: ");
        sb.append(FormatUtil.formatBytes(getSwapUsed()));
        sb.append("/");
        sb.append(FormatUtil.formatBytes(getSwapTotal()));
        sb.append(", Virtual Memory In Use/Max=");
        sb.append(FormatUtil.formatBytes(getVirtualInUse()));
        sb.append("/");
        sb.append(FormatUtil.formatBytes(getVirtualMax()));
        return sb.toString();
    }
}
