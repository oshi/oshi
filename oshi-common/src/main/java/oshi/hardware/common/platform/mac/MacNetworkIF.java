/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import java.net.NetworkInterface;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractNetworkIF;

/**
 * Base class for macOS NetworkIF implementations. Subclasses provide platform-specific display name resolution and
 * network statistics updates.
 */
@ThreadSafe
public abstract class MacNetworkIF extends AbstractNetworkIF {

    private int ifType;

    /**
     * Creates a MacNetworkIF.
     *
     * @param netint      the network interface
     * @param displayName the display name
     * @throws InstantiationException if the interface cannot be instantiated
     */
    protected MacNetworkIF(NetworkInterface netint, String displayName) throws InstantiationException {
        super(netint, displayName);
    }

    @Override
    public int getIfType() {
        return this.ifType;
    }

    /**
     * Sets the interface type.
     *
     * @param ifType the interface type to set
     */
    protected void setIfType(int ifType) {
        this.ifType = ifType;
    }
}
