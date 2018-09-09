/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.jna.platform.windows;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

/**
 * Windows Cfgmgr32Util. This class should be considered non-API as it may be
 * removed if/when its code is incorporated into the JNA project.
 *
 * @author widdis[at]gmail[dot]com
 */
public abstract class Cfgmgr32Util {
    @SuppressWarnings("serial")
    public static class Cfgmgr32Exception extends RuntimeException {
        private final int errorCode;

        public Cfgmgr32Exception(int errorCode) {
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }
    }

    /**
     * Utility method to call Cfgmgr32's CM_Get_Device_ID that allocates the
     * required memory for the Buffer parameter based on the type mapping used,
     * calls to CM_Get_Device_ID, and returns the received string.
     * 
     * @param devInst
     *            Caller-supplied device instance handle that is bound to the
     *            local machine.
     * @return The device instance ID string.
     * @throws Cfgmgr32Exception
     *             if the buffer couldn't be allocated
     */
    public static String CM_Get_Device_ID(int devInst) throws Cfgmgr32Exception {
        int charToBytes = Boolean.getBoolean("w32.ascii") ? 1 : Native.WCHAR_SIZE;

        // Get Device ID character count
        IntByReference pulLen = new IntByReference();
        int ret = Cfgmgr32.INSTANCE.CM_Get_Device_ID_Size(pulLen, devInst, 0);
        if (ret != Cfgmgr32.CR_SUCCESS) {
            throw new Cfgmgr32Exception(ret);
        }

        // Add one to length to allow null terminator
        Memory buffer = new Memory(((long) pulLen.getValue() + 1) * charToBytes);
        // Zero the buffer (including the extra character)
        buffer.clear();
        // Fetch the buffer specifying only the current length
        ret = Cfgmgr32.INSTANCE.CM_Get_Device_ID(devInst, buffer, pulLen.getValue(), 0);
        // In the unlikely event the device id changes this might not be big
        // enough, try again. This happens rarely enough one retry should be
        // sufficient.
        if (ret == Cfgmgr32.CR_BUFFER_SMALL) {
            ret = Cfgmgr32.INSTANCE.CM_Get_Device_ID_Size(pulLen, devInst, 0);
            if (ret != Cfgmgr32.CR_SUCCESS) {
                throw new Cfgmgr32Exception(ret);
            }
            buffer = new Memory(((long) pulLen.getValue() + 1) * charToBytes);
            buffer.clear();
            ret = Cfgmgr32.INSTANCE.CM_Get_Device_ID(devInst, buffer, pulLen.getValue(), 0);
        }
        // If we still aren't successful throw an exception
        if (ret != Cfgmgr32.CR_SUCCESS) {
            throw new Cfgmgr32Exception(ret);
        }
        // Convert buffer to Java String
        if (charToBytes == 1) {
            return buffer.getString(0);
        } else {
            return buffer.getWideString(0);
        }
    }
}