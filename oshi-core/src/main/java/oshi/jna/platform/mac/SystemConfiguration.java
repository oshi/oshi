/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;

/**
 * Allow applications to access a device’s network configuration settings. Determine the reachability of the device,
 * such as whether Wi-Fi or cell connectivity are active.
 */
public interface SystemConfiguration extends Library {

    SystemConfiguration INSTANCE = Native.load("SystemConfiguration", SystemConfiguration.class);

    class SCNetworkInterfaceRef extends CFTypeRef {
        public SCNetworkInterfaceRef() {
            super();
        }

        public SCNetworkInterfaceRef(Pointer p) {
            super(p);
        }
    }

    CFArrayRef SCNetworkInterfaceCopyAll();

    CFStringRef SCNetworkInterfaceGetBSDName(SCNetworkInterfaceRef netint);

    CFStringRef SCNetworkInterfaceGetLocalizedDisplayName(SCNetworkInterfaceRef netint);
}
