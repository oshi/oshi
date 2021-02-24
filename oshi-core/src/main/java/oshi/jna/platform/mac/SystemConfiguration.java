/**
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.jna.platform.mac;

import com.sun.jna.Library; // NOSONAR squid:S1191
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;

/**
 * Allow applications to access a device’s network configuration settings.
 * Determine the reachability of the device, such as whether Wi-Fi or cell
 * connectivity are active.
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
