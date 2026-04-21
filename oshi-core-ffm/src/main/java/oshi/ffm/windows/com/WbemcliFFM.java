/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows.com;

/**
 * Constants for WMI (Windows Management Instrumentation) via WbemCli.
 * <p>
 * These include CIM data types, WBEM flags, and error codes.
 * </p>
 */
public final class WbemcliFFM {

    private WbemcliFFM() {
    }

    // Default WMI namespace
    public static final String DEFAULT_NAMESPACE = "ROOT\\CIMV2";

    // CIM data types
    public static final int CIM_ILLEGAL = 0xfff;
    public static final int CIM_EMPTY = 0;
    public static final int CIM_SINT8 = 16;
    public static final int CIM_UINT8 = 17;
    public static final int CIM_SINT16 = 2;
    public static final int CIM_UINT16 = 18;
    public static final int CIM_SINT32 = 3;
    public static final int CIM_UINT32 = 19;
    public static final int CIM_SINT64 = 20;
    public static final int CIM_UINT64 = 21;
    public static final int CIM_REAL32 = 4;
    public static final int CIM_REAL64 = 5;
    public static final int CIM_BOOLEAN = 11;
    public static final int CIM_STRING = 8;
    public static final int CIM_DATETIME = 101;
    public static final int CIM_REFERENCE = 102;
    public static final int CIM_CHAR16 = 103;
    public static final int CIM_OBJECT = 13;
    public static final int CIM_FLAG_ARRAY = 0x2000;

    // WBEM generic flags
    public static final int WBEM_FLAG_RETURN_WBEM_COMPLETE = 0x0;
    public static final int WBEM_FLAG_RETURN_IMMEDIATELY = 0x10;
    public static final int WBEM_FLAG_FORWARD_ONLY = 0x20;
    public static final int WBEM_FLAG_NO_ERROR_OBJECT = 0x40;
    public static final int WBEM_FLAG_SEND_STATUS = 0x80;
    public static final int WBEM_FLAG_ENSURE_LOCATABLE = 0x100;
    public static final int WBEM_FLAG_DIRECT_READ = 0x200;

    // WBEM timeout
    public static final int WBEM_INFINITE = 0xFFFFFFFF;
    public static final int WBEM_NO_WAIT = 0;

    // WBEM error codes
    public static final int WBEM_S_NO_ERROR = 0x0;
    public static final int WBEM_S_FALSE = 0x1;
    public static final int WBEM_S_TIMEDOUT = 0x40004;
    public static final int WBEM_S_NO_MORE_DATA = 0x40005;
    public static final int WBEM_E_FAILED = 0x80041001;
    public static final int WBEM_E_NOT_FOUND = 0x80041002;
    public static final int WBEM_E_ACCESS_DENIED = 0x80041003;
    public static final int WBEM_E_PROVIDER_FAILURE = 0x80041004;
    public static final int WBEM_E_TYPE_MISMATCH = 0x80041005;
    public static final int WBEM_E_OUT_OF_MEMORY = 0x80041006;
    public static final int WBEM_E_INVALID_CONTEXT = 0x80041007;
    public static final int WBEM_E_INVALID_PARAMETER = 0x80041008;
    public static final int WBEM_E_NOT_AVAILABLE = 0x80041009;
    public static final int WBEM_E_CRITICAL_ERROR = 0x8004100a;
    public static final int WBEM_E_INVALID_STREAM = 0x8004100b;
    public static final int WBEM_E_NOT_SUPPORTED = 0x8004100c;
    public static final int WBEM_E_INVALID_SUPERCLASS = 0x8004100d;
    public static final int WBEM_E_INVALID_NAMESPACE = 0x8004100e;
    public static final int WBEM_E_INVALID_OBJECT = 0x8004100f;
    public static final int WBEM_E_INVALID_CLASS = 0x80041010;
    public static final int WBEM_E_PROVIDER_NOT_FOUND = 0x80041011;
    public static final int WBEM_E_INVALID_PROVIDER_REGISTRATION = 0x80041012;
    public static final int WBEM_E_PROVIDER_LOAD_FAILURE = 0x80041013;
    public static final int WBEM_E_INITIALIZATION_FAILURE = 0x80041014;
    public static final int WBEM_E_TRANSPORT_FAILURE = 0x80041015;
    public static final int WBEM_E_INVALID_OPERATION = 0x80041016;
    public static final int WBEM_E_INVALID_QUERY = 0x80041017;
    public static final int WBEM_E_INVALID_QUERY_TYPE = 0x80041018;
    public static final int WBEM_E_ALREADY_EXISTS = 0x80041019;
    public static final int WBEM_E_SHUTTING_DOWN = 0x80041033;

    // IWbemLocator vtable indices
    public static final int IWBEMLOCATOR_CONNECTSERVER = 3;

    // IWbemServices vtable indices
    public static final int IWBEMSERVICES_EXECQUERY = 20;

    // IEnumWbemClassObject vtable indices
    public static final int IENUMWBEMCLASSOBJECT_RESET = 3;
    public static final int IENUMWBEMCLASSOBJECT_NEXT = 4;
    public static final int IENUMWBEMCLASSOBJECT_NEXTASYNC = 5;
    public static final int IENUMWBEMCLASSOBJECT_CLONE = 6;
    public static final int IENUMWBEMCLASSOBJECT_SKIP = 7;

    // IWbemClassObject vtable indices
    public static final int IWBEMCLASSOBJECT_GET = 4;

    // IUnknown vtable indices
    public static final int IUNKNOWN_QUERYINTERFACE = 0;
    public static final int IUNKNOWN_ADDREF = 1;
    public static final int IUNKNOWN_RELEASE = 2;
}
