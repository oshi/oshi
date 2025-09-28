/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.Optional;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public final class Advapi32FFM extends WindowsForeignFunctions {

    private static final SymbolLookup ADV = lib("Advapi32");

    private static final MethodHandle AdjustTokenPrivileges = downcall(ADV, "AdjustTokenPrivileges", JAVA_INT, ADDRESS,
            JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS);

    public static boolean AdjustTokenPrivileges(MemorySegment hToken, MemorySegment tkp) throws Throwable {
        return isSuccess(
                (int) AdjustTokenPrivileges.invokeExact(hToken, 0, tkp, 0, MemorySegment.NULL, MemorySegment.NULL));
    }

    private static final MethodHandle CloseEventLog = downcall(ADV, "CloseEventLog", JAVA_INT, ADDRESS);

    public static boolean CloseEventLog(MemorySegment hEventLog) throws Throwable {
        return isSuccess((int) CloseEventLog.invokeExact(hEventLog));
    }

    private static final MethodHandle GetTokenInformation = downcall(ADV, "GetTokenInformation", JAVA_INT, ADDRESS,
            JAVA_INT, ADDRESS, JAVA_INT, ADDRESS);

    public static boolean GetTokenInformation(MemorySegment hToken, int tokenInfoClass, MemorySegment tokenInfo,
            int tokenInfoLength, MemorySegment returnLength) throws Throwable {
        return isSuccess((int) GetTokenInformation.invokeExact(hToken, tokenInfoClass, tokenInfo, tokenInfoLength,
                returnLength));
    }

    private static final MethodHandle LookupPrivilegeValue = downcall(ADV, "LookupPrivilegeValueW", JAVA_INT, ADDRESS,
            ADDRESS, ADDRESS);

    public static boolean LookupPrivilegeValue(String name, MemorySegment luid, Arena arena) throws Throwable {
        MemorySegment nameSeg = toWideString(arena, name);
        return isSuccess((int) LookupPrivilegeValue.invokeExact(MemorySegment.NULL, nameSeg, luid));
    }

    private static final MethodHandle OpenEventLog = downcall(ADV, "OpenEventLogW", ADDRESS, ADDRESS, ADDRESS);

    public static Optional<MemorySegment> OpenEventLog(MemorySegment serverName, MemorySegment sourceName)
            throws Throwable {
        MemorySegment handle = (MemorySegment) OpenEventLog.invokeExact(serverName, sourceName);
        if (handle == null || handle.equals(MemorySegment.NULL)) {
            return Optional.empty();
        }
        return Optional.of(handle);
    }

    public static Optional<MemorySegment> OpenEventLog(Arena arena, String source) {
        try {
            MemorySegment lpSource = WindowsForeignFunctions.toWideString(arena, source);
            MemorySegment handle = (MemorySegment) OpenEventLog.invokeExact(MemorySegment.NULL, lpSource);
            if (handle.address() == 0) {
                return Optional.empty();
            }
            return Optional.of(handle);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    private static final MethodHandle OpenProcessToken = downcall(ADV, "OpenProcessToken", JAVA_INT, ADDRESS, JAVA_INT,
            ADDRESS);

    public static boolean OpenProcessToken(MemorySegment process, int desiredAccess, MemorySegment hTokenOut)
            throws Throwable {
        return isSuccess((int) OpenProcessToken.invokeExact(process, desiredAccess, hTokenOut));
    }

    private static final MethodHandle ReadEventLog = downcall(ADV, "ReadEventLogW", JAVA_INT, ADDRESS, JAVA_INT,
            JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS);

    public static boolean ReadEventLog(MemorySegment hEventLog, int flags, MemorySegment buffer, int bufSize,
            MemorySegment bytesRead, MemorySegment minBytesNeeded) throws Throwable {
        return isSuccess(
                (int) ReadEventLog.invokeExact(hEventLog, flags, 0, buffer, bufSize, bytesRead, minBytesNeeded));
    }

    private static final MethodHandle RegCloseKey = downcall(ADV, "RegCloseKey", JAVA_INT, ADDRESS);

    public static int RegCloseKey(MemorySegment hKey) throws Throwable {
        return (int) RegCloseKey.invokeExact(hKey);
    }

    private static final MethodHandle RegEnumKeyEx = downcall(ADV, "RegEnumKeyExW", JAVA_INT, ADDRESS, JAVA_INT,
            ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS);

    public static int RegEnumKeyEx(MemorySegment hKey, int dwIndex, MemorySegment lpName, MemorySegment lpcchName,
            MemorySegment lpReserved, MemorySegment lpClass, MemorySegment lpcchClass, MemorySegment lpftLastWriteTime)
            throws Throwable {
        return (int) RegEnumKeyEx.invokeExact(hKey, dwIndex, lpName, lpcchName, lpReserved, lpClass, lpcchClass,
                lpftLastWriteTime);
    }

    private static final MethodHandle RegOpenKeyEx = downcall(ADV, "RegOpenKeyExW", JAVA_INT, ADDRESS, ADDRESS,
            JAVA_INT, JAVA_INT, ADDRESS);

    public static int RegOpenKeyEx(MemorySegment hKey, MemorySegment subKey, int options, int samDesired,
            MemorySegment phkResult) throws Throwable {
        return (int) RegOpenKeyEx.invokeExact(hKey, subKey, options, samDesired, phkResult);
    }

    private static final MethodHandle RegQueryInfoKey = downcall(ADV, "RegQueryInfoKeyW", JAVA_INT, ADDRESS, ADDRESS,
            ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS);

    public static int RegQueryInfoKey(MemorySegment hKey, MemorySegment lpClass, MemorySegment lpcchClass,
            MemorySegment lpReserved, MemorySegment lpcSubKeys, MemorySegment lpcMaxSubKeyLen,
            MemorySegment lpcMaxClassLen, MemorySegment lpcValues, MemorySegment lpcMaxValueNameLen,
            MemorySegment lpcMaxValueLen, MemorySegment lpcbSecurityDescriptor, MemorySegment lpftLastWriteTime)
            throws Throwable {
        return (int) RegQueryInfoKey.invokeExact(hKey, lpClass, lpcchClass, lpReserved, lpcSubKeys, lpcMaxSubKeyLen,
                lpcMaxClassLen, lpcValues, lpcMaxValueNameLen, lpcMaxValueLen, lpcbSecurityDescriptor,
                lpftLastWriteTime);
    }

    private static final MethodHandle RegQueryValueEx = downcall(ADV, "RegQueryValueExW", JAVA_INT, ADDRESS, ADDRESS,
            JAVA_INT, ADDRESS, ADDRESS, ADDRESS);

    public static int RegQueryValueEx(MemorySegment hKey, MemorySegment lpValueName, int reserved, MemorySegment lpType,
            MemorySegment lpData, MemorySegment lpcbData) throws Throwable {
        return (int) RegQueryValueEx.invokeExact(hKey, lpValueName, reserved, lpType, lpData, lpcbData);
    }
}
