/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.dragonflybsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.unix.freebsd.FreeBsdNetworkParamsFFM;
import oshi.util.driver.unix.RouteTableDump;

/**
 * DragonFly BSD network parameters, which are FreeBSD's apart from the routing message layout.
 */
@ThreadSafe
public class DragonFlyBsdNetworkParamsFFM extends FreeBsdNetworkParamsFFM {

    @Override
    protected RouteTableDump.Layout routeLayout() {
        return RouteTableDump.Layout.DRAGONFLY;
    }
}
