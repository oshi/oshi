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
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.jna.platform.windows.COM;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

public class EnumWbemClassObject extends Unknown {
    public static final int WBEM_FLAG_RETURN_IMMEDIATELY = 0x00000010;
    public static final int WBEM_FLAG_FORWARD_ONLY = 0x00000020;
    public static final int WBEM_INFINITE = 0xFFFFFFFF;

    public EnumWbemClassObject(Pointer pvInstance) {
        super(pvInstance);
    }

    public HRESULT Next(NativeLong lTimeOut, NativeLong uCount, PointerByReference ppObjects,
            LongByReference puReturned) {
        // Next is 5th method of vtable for EnumWbemClassObject in WbemCli.h
        return (HRESULT) _invokeNativeObject(4, new Object[] { getPointer(), lTimeOut, uCount, ppObjects, puReturned },
                HRESULT.class);
    }
}