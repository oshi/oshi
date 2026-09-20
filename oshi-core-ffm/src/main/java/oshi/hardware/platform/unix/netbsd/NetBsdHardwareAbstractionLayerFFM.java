/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.netbsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.hardware.common.platform.unix.netbsd.NetBsdHardwareAbstractionLayer;

/**
 * A HardwareAbstractionLayer for NetBSD that supplies the FFM-capable {@link NetBsdCentralProcessorFFM}. All other
 * hardware components are native-free and inherited unchanged from {@link NetBsdHardwareAbstractionLayer}.
 */
@ThreadSafe
public final class NetBsdHardwareAbstractionLayerFFM extends NetBsdHardwareAbstractionLayer {

    @Override
    public CentralProcessor createProcessor() {
        return new NetBsdCentralProcessorFFM();
    }
}
