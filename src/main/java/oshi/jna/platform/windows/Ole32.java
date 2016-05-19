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
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.jna.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WTypes.BSTR;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.win32.W32APIOptions;

/**
 * Windows Kernel32. This class should be considered non-API as it may be
 * removed if/when its code is incorporated into the JNA project.
 * 
 * @author widdis[at]gmail[dot]com
 */
public interface Ole32 extends com.sun.jna.platform.win32.Ole32 {
    Ole32 INSTANCE = (Ole32) Native.loadLibrary("Ole32", Ole32.class, W32APIOptions.DEFAULT_OPTIONS);

    public static final int RPC_C_AUTHN_LEVEL_DEFAULT = 0;
    public static final int RPC_C_AUTHN_WINNT = 10;
    public static final int RPC_C_IMP_LEVEL_IMPERSONATE = 3;
    public static final int RPC_C_AUTHZ_NONE = 0;
    public static final int RPC_C_AUTHN_LEVEL_CALL = 3;

    public static final int RPC_E_TOO_LATE = 0x80010119;

    public static final int EOAC_NONE = 0;

    // TODO: Submit this change to JNA Ole32 class
    /**
     * Registers security and sets the default security values for the process.
     * 
     * @param pSecDesc
     *            [in, optional] The access permissions that a server will use
     *            to receive calls. This parameter is used by COM only when a
     *            server calls CoInitializeSecurity. Its value is a pointer to
     *            one of three types: an AppID, an IAccessControl object, or a
     *            SECURITY_DESCRIPTOR, in absolute format. See the Remarks
     *            section for more information.
     * @param cAuthSvc
     *            [in] The count of entries in the asAuthSvc parameter. This
     *            parameter is used by COM only when a server calls
     *            CoInitializeSecurity. If this parameter is 0, no
     *            authentication services will be registered and the server
     *            cannot receive secure calls. A value of -1 tells COM to choose
     *            which authentication services to register, and if this is the
     *            case, the asAuthSvc parameter must be NULL. However, Schannel
     *            will never be chosen as an authentication service by the
     *            server if this parameter is -1.
     * @param asAuthSvc
     *            [in, optional] An array of authentication services that a
     *            server is willing to use to receive a call. This parameter is
     *            used by COM only when a server calls CoInitializeSecurity. For
     *            more information, see SOLE_AUTHENTICATION_SERVICE.
     * @param pReserved1
     *            [in, optional] This parameter is reserved and must be NULL.
     * @param dwAuthnLevel
     *            [in] The default authentication level for the process. Both
     *            servers and clients use this parameter when they call
     *            CoInitializeSecurity. COM will fail calls that arrive with a
     *            lower authentication level. By default, all proxies will use
     *            at least this authentication level. This value should contain
     *            one of the authentication level constants. By default, all
     *            calls to IUnknown are made at this level.
     * @param dwImpLevel
     *            [in] The default impersonation level for proxies. The value of
     *            this parameter is used only when the process is a client. It
     *            should be a value from the impersonation level constants,
     *            except for RPC_C_IMP_LEVEL_DEFAULT, which is not for use with
     *            CoInitializeSecurity. Outgoing calls from the client always
     *            use the impersonation level as specified. (It is not
     *            negotiated.) Incoming calls to the client can be at any
     *            impersonation level. By default, all IUnknown calls are made
     *            with this impersonation level, so even security-aware
     *            applications should set this level carefully. To determine
     *            which impersonation levels each authentication service
     *            supports, see the description of the authentication services
     *            in COM and Security Packages. For more information about
     *            impersonation levels, see Impersonation.
     * @param pAuthList
     *            [in, optional] A pointer to SOLE_AUTHENTICATION_LIST, which is
     *            an array of SOLE_AUTHENTICATION_INFO structures. This list
     *            indicates the information for each authentication service that
     *            a client can use to call a server. This parameter is used by
     *            COM only when a client calls CoInitializeSecurity.
     * @param dwCapabilities
     *            [in] Additional capabilities of the client or server,
     *            specified by setting one or more
     *            EOLE_AUTHENTICATION_CAPABILITIES values. Some of these value
     *            cannot be used simultaneously, and some cannot be set when
     *            particular authentication services are being used.
     * @param pReserved3
     *            [in, optional] This parameter is reserved and must be NULL.
     * @return This function can return the standard return value E_INVALIDARG,
     *         as well as the following values.
     * 
     *         S_OK Indicates success.
     * 
     *         RPC_E_TOO_LATE CoInitializeSecurity has already been called.
     * 
     *         RPC_E_NO_GOOD_SECURITY_PACKAGES The asAuthSvc parameter was not
     *         NULL, and none of the authentication services in the list could
     *         be registered. Check the results saved in asAuthSvc for
     *         authentication service–specific error codes.
     * 
     *         E_OUT_OF_MEMORY Out of memory.
     */
    HRESULT CoInitializeSecurity(Pointer pSecDesc, NativeLong cAuthSvc, Pointer asAuthSvc, Pointer pReserved1,
            int dwAuthnLevel, int dwImpLevel, Pointer pAuthList, int dwCapabilities, Pointer pReserved3);

    /**
     * Sets the authentication information that will be used to make calls on
     * the specified proxy. This is a helper function for
     * IClientSecurity::SetBlanket.
     * 
     * @param pProxy
     *            [in] The proxy to be set.
     * @param dwAuthnSvc
     *            [in] The authentication service to be used. For a list of
     *            possible values, see Authentication Service Constants. Use
     *            RPC_C_AUTHN_NONE if no authentication is required. If
     *            RPC_C_AUTHN_DEFAULT is specified, DCOM will pick an
     *            authentication service following its normal security blanket
     *            negotiation algorithm.
     * @param dwAuthzSvc
     *            [in] The authorization service to be used. For a list of
     *            possible values, see Authorization Constants. If
     *            RPC_C_AUTHZ_DEFAULT is specified, DCOM will pick an
     *            authorization service following its normal security blanket
     *            negotiation algorithm. RPC_C_AUTHZ_NONE should be used as the
     *            authorization service if NTLMSSP, Kerberos, or Schannel is
     *            used as the authentication service.
     * @param pServerPrincName
     *            [in, optional] The server principal name to be used with the
     *            authentication service. If COLE_DEFAULT_PRINCIPAL is
     *            specified, DCOM will pick a principal name using its security
     *            blanket negotiation algorithm. If Kerberos is used as the
     *            authentication service, this value must not be NULL. It must
     *            be the correct principal name of the server or the call will
     *            fail. If Schannel is used as the authentication service, this
     *            value must be one of the msstd or fullsic forms described in
     *            Principal Names, or NULL if you do not want mutual
     *            authentication. Generally, specifying NULL will not reset the
     *            server principal name on the proxy; rather, the previous
     *            setting will be retained. You must be careful when using NULL
     *            as pServerPrincName when selecting a different authentication
     *            service for the proxy, because there is no guarantee that the
     *            previously set principal name would be valid for the newly
     *            selected authentication service.
     * @param dwAuthnLevel
     *            [in] The authentication level to be used. For a list of
     *            possible values, see Authentication Level Constants. If
     *            RPC_C_AUTHN_LEVEL_DEFAULT is specified, DCOM will pick an
     *            authentication level following its normal security blanket
     *            negotiation algorithm. If this value is none, the
     *            authentication service must also be none.
     * @param dwImpLevel
     *            [in] The impersonation level to be used. For a list of
     *            possible values, see Impersonation Level Constants. If
     *            RPC_C_IMP_LEVEL_DEFAULT is specified, DCOM will pick an
     *            impersonation level following its normal security blanket
     *            negotiation algorithm. If NTLMSSP is the authentication
     *            service, this value must be RPC_C_IMP_LEVEL_IMPERSONATE or
     *            RPC_C_IMP_LEVEL_IDENTIFY. NTLMSSP also supports delegate-level
     *            impersonation (RPC_C_IMP_LEVEL_DELEGATE) on the same computer.
     *            If Schannel is the authentication service, this parameter must
     *            be RPC_C_IMP_LEVEL_IMPERSONATE.
     * @param pAuthInfo
     *            [in, optional] A pointer to an RPC_AUTH_IDENTITY_HANDLE value
     *            that establishes the identity of the client. The format of the
     *            structure referred to by the handle depends on the provider of
     *            the authentication service. For calls on the same computer,
     *            RPC logs on the user with the supplied credentials and uses
     *            the resulting token for the method call. For NTLMSSP or
     *            Kerberos, the structure is a SEC_WINNT_AUTH_IDENTITY or
     *            SEC_WINNT_AUTH_IDENTITY_EX structure. The client can discard
     *            pAuthInfo after calling the API. RPC does not keep a copy of
     *            the pAuthInfo pointer, and the client cannot retrieve it later
     *            in the CoQueryProxyBlanket method. If this parameter is NULL,
     *            DCOM uses the current proxy identity (which is either the
     *            process token or the impersonation token). If the handle
     *            refers to a structure, that identity is used. For Schannel,
     *            this parameter must be either a pointer to a CERT_CONTEXT
     *            structure that contains the client's X.509 certificate or is
     *            NULL if the client wishes to make an anonymous connection to
     *            the server. If a certificate is specified, the caller must not
     *            free it as long as any proxy to the object exists in the
     *            current apartment. For Snego, this member is either NULL,
     *            points to a SEC_WINNT_AUTH_IDENTITY structure, or points to a
     *            SEC_WINNT_AUTH_IDENTITY_EX structure. If it is NULL, Snego
     *            will pick a list of authentication services based on those
     *            available on the client computer. If it points to a
     *            SEC_WINNT_AUTH_IDENTITY_EX structure, the structure's
     *            PackageList member must point to a string containing a
     *            comma-separated list of authentication service names and the
     *            PackageListLength member must give the number of bytes in the
     *            PackageList string. If PackageList is NULL, all calls using
     *            Snego will fail. If COLE_DEFAULT_AUTHINFO is specified for
     *            this parameter, DCOM will pick the authentication information
     *            following its normal security blanket negotiation algorithm.
     *            CoSetProxyBlanket will fail if pAuthInfo is set and one of the
     *            cloaking flags is set in the dwCapabilities parameter.
     * @param dwCapabilities
     *            [in] The capabilities of this proxy. For a list of possible
     *            values, see the EOLE_AUTHENTICATION_CAPABILITIES enumeration.
     *            The only flags that can be set through this function are
     *            EOAC_MUTUAL_AUTH, EOAC_STATIC_CLOAKING, EOAC_DYNAMIC_CLOAKING,
     *            EOAC_ANY_AUTHORITY (this flag is deprecated),
     *            EOAC_MAKE_FULLSIC, and EOAC_DEFAULT. Either
     *            EOAC_STATIC_CLOAKING or EOAC_DYNAMIC_CLOAKING can be set if
     *            pAuthInfo is not set and Schannel is not the authentication
     *            service. (See Cloaking for more information.) If any
     *            capability flags other than those mentioned here are set,
     *            CoSetProxyBlanket will fail.
     * @return This function can return the following values.
     * 
     *         S_OK The function was successful.
     * 
     *         E_INVALIDARG One or more arguments is invalid.
     */
    HRESULT CoSetProxyBlanket(Pointer pProxy, //
            int dwAuthnSvc, //
            int dwAuthzSvc, //
            BSTR pServerPrincName, // OLECHAR
            int dwAuthnLevel, //
            int dwImpLevel, //
            Pointer pAuthInfo, // RPC_AUTH_IDENTITY_HANDLE
            int dwCapabilities//
    );

}
