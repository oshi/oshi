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
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

public interface Cfgmgr32 extends com.sun.jna.platform.win32.Cfgmgr32 {

    Cfgmgr32 INSTANCE = Native.load("cfgmgr32", Cfgmgr32.class, W32APIOptions.DEFAULT_OPTIONS);

    // For use with CM_Get_DevNode_Registry_Property()
    int CM_DRP_DEVICEDESC = 0x00000001; // DeviceDesc REG_SZ property (RW)
    int CM_DRP_HARDWAREID = 0x00000002; // HardwareID REG_MULTI_SZ property (RW)
    int CM_DRP_COMPATIBLEIDS = 0x00000003; // CompatibleIDs REG_MULTI_SZ property (RW)
    int CM_DRP_SERVICE = 0x00000005; // Service REG_SZ property (RW)
    int CM_DRP_CLASS = 0x00000008; // Class REG_SZ property (RW)
    int CM_DRP_CLASSGUID = 0x00000009; // ClassGUID REG_SZ property (RW)
    int CM_DRP_DRIVER = 0x0000000A; // Driver REG_SZ property (RW)
    int CM_DRP_MFG = 0x0000000C; // Mfg REG_SZ property (RW)
    int CM_DRP_FRIENDLYNAME = 0x0000000D; // FriendlyName REG_SZ property (RW)
    int CM_DRP_LOCATION_INFORMATION = 0x0000000E; // LocationInformation REG_SZ property (RW)
    int CM_DRP_PHYSICAL_DEVICE_OBJECT_NAME = 0x0000000F; // PhysicalDeviceObjectName REG_SZ property (R)

    boolean CM_Get_DevNode_Registry_Property(int dnDevInst, int ulProperty, IntByReference pulRegDataType,
            Pointer buffer, IntByReference pulLength, int ulFlags);
}
