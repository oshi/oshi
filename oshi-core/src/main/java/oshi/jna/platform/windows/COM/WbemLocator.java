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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid.CLSID;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.WTypes;
import com.sun.jna.platform.win32.WTypes.BSTR;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.windows.Ole32;

public class WbemLocator extends Unknown {
    private static final Logger LOG = LoggerFactory.getLogger(WbemLocator.class);

    public static final CLSID CLSID_WbemLocator = new CLSID("4590f811-1d3a-11d0-891f-00aa004b2e24");
    public static final GUID IID_IWbemLocator = new GUID("dc12a687-737f-11cf-884d-00aa004b2e24");

    private WbemLocator(Pointer pvInstance) {
        super(pvInstance);
    }

    public static WbemLocator create() {
        PointerByReference pbr = new PointerByReference();

        HRESULT hres = Ole32.INSTANCE.CoCreateInstance(CLSID_WbemLocator, null, WTypes.CLSCTX_INPROC_SERVER,
                IID_IWbemLocator, pbr);
        if (COMUtils.FAILED(hres)) {
            LOG.error(String.format("Failed to create WbemLocator object. Error code = 0x%08x", hres.intValue()));
            Ole32.INSTANCE.CoUninitialize();
            return null;
        }

        return new WbemLocator(pbr.getValue());
    }

    public HRESULT ConnectServer(BSTR strNetworkResource, BSTR strUser, BSTR strPassword, BSTR strLocale,
            NativeLong lSecurityFlags, BSTR strAuthority, Pointer pCtx, PointerByReference ppNamespace) {
        // ConnectServier is 4th method of vtable for WbemLocator in WbemCli.h
        return (HRESULT) _invokeNativeObject(3, new Object[] { getPointer(), strNetworkResource, strUser, strPassword,
                strLocale, lSecurityFlags, strAuthority, pCtx, ppNamespace }, HRESULT.class);
    }
}