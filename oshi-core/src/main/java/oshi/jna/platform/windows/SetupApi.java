/*
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
package oshi.jna.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.Structure; // NOSONAR squid:S1191
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.win32.ShellAPI;
import com.sun.jna.win32.W32APIOptions;

public interface SetupApi extends com.sun.jna.platform.win32.SetupApi {

    SetupApi INSTANCE = Native.load("setupapi", SetupApi.class, W32APIOptions.DEFAULT_OPTIONS);

    int SP_DEVICE_INTERFACE_DETAIL_DATA_SIZE = new SP_DEVICE_INTERFACE_DETAIL_DATA().size();

    // For use with SetupDiGetDeviceRegistryProperty
    int SPDRP_DEVICEDESC = 0x00000000; // DeviceDesc (R/W)
    int SPDRP_HARDWAREID = 0x00000001; // HardwareID (R/W)
    int SPDRP_COMPATIBLEIDS = 0x00000002; // CompatibleIDs (R/W)
    int SPDRP_SERVICE = 0x00000004; // Service (R/W)
    int SPDRP_CLASS = 0x00000007; // Class (R--tied to ClassGUID)
    int SPDRP_CLASSGUID = 0x00000008; // ClassGUID (R/W)
    int SPDRP_DRIVER = 0x00000009; // Driver (R/W)
    int SPDRP_MFG = 0x0000000B; // Mfg (R/W)
    int SPDRP_FRIENDLYNAME = 0x0000000C; // FriendlyName (R/W)
    int SPDRP_LOCATION_INFORMATION = 0x0000000D; // LocationInformation (R/W)
    int SPDRP_PHYSICAL_DEVICE_OBJECT_NAME = 0x0000000E; // PhysicalDeviceObjectName (R)

    @FieldOrder({ "cbSize", "DevicePath" })
    class SP_DEVICE_INTERFACE_DETAIL_DATA extends Structure {
        public int cbSize;
        public char[] DevicePath = new char[1];

        public SP_DEVICE_INTERFACE_DETAIL_DATA() {
            super(ShellAPI.STRUCTURE_ALIGNMENT);
        }

        public SP_DEVICE_INTERFACE_DETAIL_DATA(int bufferSize) {
            this();
            // Resize char array to account for cbSize int
            if (bufferSize > SP_DEVICE_INTERFACE_DETAIL_DATA_SIZE) {
                this.DevicePath = new char[(bufferSize - Integer.BYTES) / Native.WCHAR_SIZE];
            }
            write();
        }

        @Override
        public void write() {
            this.cbSize = SP_DEVICE_INTERFACE_DETAIL_DATA_SIZE;
            super.write();
        }
    }
}
