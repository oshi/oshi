/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.SetupApi;
import com.sun.jna.platform.win32.SetupApi.SP_DEVICE_INTERFACE_DATA;
import com.sun.jna.platform.win32.SetupApi.SP_DEVINFO_DATA;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinReg.HKEY;
import com.sun.jna.ptr.IntByReference;

import oshi.hardware.Display;

/**
 * A Display
 * 
 * @author widdis[at]gmail[dot]com
 */
public class WindowsDisplay implements Display {
    private static final Logger LOG = LoggerFactory.getLogger(WindowsDisplay.class);

    private byte[] edid;

    public WindowsDisplay(byte[] edid) {
        this.edid = edid;
        LOG.debug("Initialized WindowsDisplay");
    }

    @Override
    public byte[] getEdid() {
        return Arrays.copyOf(edid, edid.length);
    }

    /**
     * Gets Display Information
     * 
     * @return An array of Display objects representing monitors, etc.
     */
    public static Display[] getDisplays() {
        List<Display> displays = new ArrayList<Display>();

        Guid.GUID monitorGuid = new Guid.GUID("E6F07B5F-EE97-4a90-B076-33F57BF4EAA7");
        WinNT.HANDLE hDevInfo = SetupApi.INSTANCE.SetupDiGetClassDevs(monitorGuid, null, null,
                (SetupApi.DIGCF_PRESENT | SetupApi.DIGCF_DEVICEINTERFACE));
        if (!hDevInfo.equals(WinNT.INVALID_HANDLE_VALUE)) {
            SP_DEVICE_INTERFACE_DATA deviceInterfaceData = new SetupApi.SP_DEVICE_INTERFACE_DATA();
            deviceInterfaceData.cbSize = deviceInterfaceData.size();

            // build a DevInfo Data structure
            SP_DEVINFO_DATA info = new SetupApi.SP_DEVINFO_DATA();

            for (int memberIndex = 0; SetupApi.INSTANCE.SetupDiEnumDeviceInfo(hDevInfo, memberIndex,
                    info); memberIndex++) {
                HKEY key = SetupApi.INSTANCE.SetupDiOpenDevRegKey(hDevInfo, info, SetupApi.DICS_FLAG_GLOBAL, 0,
                        SetupApi.DIREG_DEV, WinNT.KEY_QUERY_VALUE);

                byte[] edid = new byte[1];
                Advapi32 advapi32 = Advapi32.INSTANCE;
                IntByReference pType = new IntByReference();
                IntByReference lpcbData = new IntByReference();

                if (advapi32.RegQueryValueEx(key, "EDID", 0, pType, edid, lpcbData) == WinError.ERROR_MORE_DATA) {
                    edid = new byte[lpcbData.getValue()];
                    if (advapi32.RegQueryValueEx(key, "EDID", 0, pType, edid, lpcbData) == WinError.ERROR_SUCCESS) {
                        Display display = new WindowsDisplay(edid);
                        displays.add(display);
                    }
                }
                Advapi32.INSTANCE.RegCloseKey(key);
            }
        }
        return displays.toArray(new Display[displays.size()]);
    }
}