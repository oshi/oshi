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

import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid.CLSID;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Variant.VARIANT;
import com.sun.jna.platform.win32.WTypes;
import com.sun.jna.platform.win32.WTypes.BSTR;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * This header is used by Remote Desktop Services.
 */
public interface Wbemcli {

    public static final int WBEM_FLAG_RETURN_IMMEDIATELY = 0x00000010;
    public static final int WBEM_FLAG_FORWARD_ONLY = 0x00000020;
    public static final int WBEM_INFINITE = 0xFFFFFFFF;

    // Non-error constants
    // https://docs.microsoft.com/en-us/windows/desktop/wmisdk/wmi-non-error-constants
    public static final int WBEM_S_NO_ERROR = 0x0;
    public static final int WBEM_S_FALSE = 0x1;
    public static final int WBEM_S_TIMEDOUT = 0x40004;
    public static final int WBEM_S_NO_MORE_DATA = 0x40005;

    // Error constants
    // https://docs.microsoft.com/en-us/windows/desktop/wmisdk/wmi-error-constants
    public static final int WBEM_E_INVALID_NAMESPACE = 0x8004100e;
    public static final int WBEM_E_INVALID_CLASS = 0x80041010;
    public static final int WBEM_E_INVALID_QUERY = 0x80041017;

    /**
     * Holds a row of results of a WMI query
     */
    class IWbemClassObject extends Unknown {

        public IWbemClassObject(Pointer pvInstance) {
            super(pvInstance);
        }

        public HRESULT Get(WString wszName, int lFlags, VARIANT.ByReference pVal, IntByReference pvtType,
                IntByReference plFlavor) {
            // Get is 5th method of IWbemClassObjectVtbl in WbemCli.h
            return (HRESULT) _invokeNativeObject(4,
                    new Object[] { getPointer(), wszName, lFlags, pVal, pvtType, plFlavor }, HRESULT.class);
        }
    }

    /**
     * Iterates to the next row of results of a WMI query
     */
    class IEnumWbemClassObject extends Unknown {

        public IEnumWbemClassObject(Pointer pvInstance) {
            super(pvInstance);
        }

        public HRESULT Next(int lTimeOut, int uCount, PointerByReference ppObjects, IntByReference puReturned) {
            // Next is 5th method of IEnumWbemClassObjectVtbl in
            // WbemCli.h
            return (HRESULT) _invokeNativeObject(4,
                    new Object[] { getPointer(), lTimeOut, uCount, ppObjects, puReturned }, HRESULT.class);
        }
    }

    /**
     * Locates and connects to a WMI namespace
     */
    class IWbemLocator extends Unknown {
        public static final CLSID CLSID_WbemLocator = new CLSID("4590f811-1d3a-11d0-891f-00aa004b2e24");
        public static final GUID IID_IWbemLocator = new GUID("dc12a687-737f-11cf-884d-00aa004b2e24");

        private IWbemLocator(Pointer pvInstance) {
            super(pvInstance);
        }

        public static IWbemLocator create() {
            PointerByReference pbr = new PointerByReference();

            HRESULT hres = Ole32.INSTANCE.CoCreateInstance(CLSID_WbemLocator, null, WTypes.CLSCTX_INPROC_SERVER,
                    IID_IWbemLocator, pbr);
            if (COMUtils.FAILED(hres)) {
                Ole32.INSTANCE.CoUninitialize();
                throw new WbemcliException("Failed to create WbemLocator object.", hres.intValue());
            }

            return new IWbemLocator(pbr.getValue());
        }

        public HRESULT ConnectServer(BSTR strNetworkResource, BSTR strUser, BSTR strPassword, BSTR strLocale,
                int lSecurityFlags, BSTR strAuthority, Pointer pCtx, PointerByReference ppNamespace) {
            // ConnectServier is 4th method of IWbemLocatorVtbl in WbemCli.h
            return (HRESULT) _invokeNativeObject(3, new Object[] { getPointer(), strNetworkResource, strUser,
                    strPassword, strLocale, lSecurityFlags, strAuthority, pCtx, ppNamespace }, HRESULT.class);
        }
    }

    /**
     * Executes a WMI Query
     */
    class IWbemServices extends Unknown {

        public IWbemServices(Pointer pvInstance) {
            super(pvInstance);
        }

        public HRESULT ExecQuery(BSTR strQueryLanguage, BSTR strQuery, int lFlags, Pointer pCtx,
                PointerByReference ppEnum) {
            // ExecQuery is 21st method of IWbemServicesVtbl in WbemCli.h
            return (HRESULT) _invokeNativeObject(20,
                    new Object[] { getPointer(), strQueryLanguage, strQuery, lFlags, pCtx, ppEnum }, HRESULT.class);
        }
    }

    /**
     * Exception encountered in this class
     */
    @SuppressWarnings("serial")
    class WbemcliException extends RuntimeException {
        private final int errorCode;

        /**
         * Creates a new exception
         * 
         * @param message
         *            The message to display. The error code will be appended to
         *            this message.
         * @param error
         *            The error code.
         */
        public WbemcliException(String message, int error) {
            super(String.format("%s Error code 0x%08x", message, error));
            this.errorCode = error;
        }

        /**
         * @return Returns the errorCode.
         */
        public int getErrorCode() {
            return errorCode;
        }
    }
}
