/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

public interface IOKitHeaders {

    int kIORegistryIterateRecursively = 0x00000001;
    int kIORegistryIterateParents = 0x00000002;
    int kIOReturnNoDevice = 0xe00002c0;
    double kIOPSTimeRemainingUnlimited = -2.0;
    double kIOPSTimeRemainingUnknown = -1.0;

}
