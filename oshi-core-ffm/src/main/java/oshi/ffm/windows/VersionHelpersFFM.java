/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static oshi.ffm.windows.WinNTFFM.OSVERSIONINFOEX;
import static oshi.ffm.windows.WinNTFFM.OSVERSIONINFOEX_MAJOR_VERSION_OFFSET;
import static oshi.ffm.windows.WinNTFFM.OSVERSIONINFOEX_MINOR_VERSION_OFFSET;
import static oshi.ffm.windows.WinNTFFM.OSVERSIONINFOEX_PRODUCT_TYPE_OFFSET;
import static oshi.ffm.windows.WinNTFFM.OSVERSIONINFOEX_SP_MAJOR_OFFSET;
import static oshi.ffm.windows.WinNTFFM.VER_EQUAL;
import static oshi.ffm.windows.WinNTFFM.VER_GREATER_EQUAL;
import static oshi.ffm.windows.WinNTFFM.VER_MAJORVERSION;
import static oshi.ffm.windows.WinNTFFM.VER_MINORVERSION;
import static oshi.ffm.windows.WinNTFFM.VER_NT_WORKSTATION;
import static oshi.ffm.windows.WinNTFFM.VER_PRODUCT_TYPE;
import static oshi.ffm.windows.WinNTFFM.VER_SERVICEPACKMAJOR;
import static oshi.ffm.windows.WinNTFFM.WIN32_WINNT_VISTA;
import static oshi.ffm.windows.WinNTFFM.WIN32_WINNT_WIN10;
import static oshi.ffm.windows.WinNTFFM.WIN32_WINNT_WIN7;
import static oshi.ffm.windows.WinNTFFM.WIN32_WINNT_WIN8;
import static oshi.ffm.windows.WinNTFFM.WIN32_WINNT_WINBLUE;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FFM port of the Windows SDK versionhelpers.h inline functions. Uses {@link Kernel32FFM#VerSetConditionMask} and
 * {@link Kernel32FFM#VerifyVersionInfoW} to determine the current operating system version.
 */
public final class VersionHelpersFFM {

    private static final Logger LOG = LoggerFactory.getLogger(VersionHelpersFFM.class);

    private static final int ERROR_OLD_WIN_VERSION = 0x47E;

    private VersionHelpersFFM() {
    }

    /**
     * Tests whether the current OS version matches, or is greater than, the provided version information.
     *
     * @param wMajorVersion     The major version to test
     * @param wMinorVersion     The minor version to test
     * @param wServicePackMajor The service pack to test
     * @return true if the current OS version matches or is greater than the provided version
     */
    public static boolean IsWindowsVersionOrGreater(int wMajorVersion, int wMinorVersion, int wServicePackMajor) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment osvi = arena.allocate(OSVERSIONINFOEX);
            osvi.fill((byte) 0);
            osvi.set(JAVA_INT, 0, (int) OSVERSIONINFOEX.byteSize()); // dwOSVersionInfoSize
            osvi.set(JAVA_INT, OSVERSIONINFOEX_MAJOR_VERSION_OFFSET, wMajorVersion);
            osvi.set(JAVA_INT, OSVERSIONINFOEX_MINOR_VERSION_OFFSET, wMinorVersion);
            osvi.set(JAVA_SHORT, OSVERSIONINFOEX_SP_MAJOR_OFFSET, (short) wServicePackMajor);

            long dwlConditionMask = 0;
            dwlConditionMask = Kernel32FFM.VerSetConditionMask(dwlConditionMask, VER_MAJORVERSION,
                    (byte) VER_GREATER_EQUAL);
            dwlConditionMask = Kernel32FFM.VerSetConditionMask(dwlConditionMask, VER_MINORVERSION,
                    (byte) VER_GREATER_EQUAL);
            dwlConditionMask = Kernel32FFM.VerSetConditionMask(dwlConditionMask, VER_SERVICEPACKMAJOR,
                    (byte) VER_GREATER_EQUAL);

            return Kernel32FFM.VerifyVersionInfoW(osvi, VER_MAJORVERSION | VER_MINORVERSION | VER_SERVICEPACKMAJOR,
                    dwlConditionMask);
        }
    }

    /**
     * @return true if the current OS version matches, or is greater than, the Windows Vista version.
     */
    public static boolean IsWindowsVistaOrGreater() {
        return IsWindowsVersionOrGreater((byte) (WIN32_WINNT_VISTA >>> 8), (byte) WIN32_WINNT_VISTA, 0);
    }

    /**
     * @return true if the current OS version matches, or is greater than, the Windows 7 version.
     */
    public static boolean IsWindows7OrGreater() {
        return IsWindowsVersionOrGreater((byte) (WIN32_WINNT_WIN7 >>> 8), (byte) WIN32_WINNT_WIN7, 0);
    }

    /**
     * @return true if the current OS version matches, or is greater than, the Windows 8 version.
     */
    public static boolean IsWindows8OrGreater() {
        return IsWindowsVersionOrGreater((byte) (WIN32_WINNT_WIN8 >>> 8), (byte) WIN32_WINNT_WIN8, 0);
    }

    /**
     * @return true if the current OS version matches, or is greater than, the Windows 8.1 version.
     */
    public static boolean IsWindows8Point1OrGreater() {
        return IsWindowsVersionOrGreater((byte) (WIN32_WINNT_WINBLUE >>> 8), (byte) WIN32_WINNT_WINBLUE, 0);
    }

    /**
     * @return true if the current OS version matches, or is greater than, the Windows 10 version.
     */
    public static boolean IsWindows10OrGreater() {
        return IsWindowsVersionOrGreater((byte) (WIN32_WINNT_WIN10 >>> 8), (byte) WIN32_WINNT_WIN10, 0);
    }

    /**
     * @return true if the current OS is a Windows Server release.
     */
    public static boolean IsWindowsServer() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment osvi = arena.allocate(OSVERSIONINFOEX);
            osvi.fill((byte) 0);
            osvi.set(JAVA_INT, 0, (int) OSVERSIONINFOEX.byteSize()); // dwOSVersionInfoSize
            osvi.set(JAVA_BYTE, OSVERSIONINFOEX_PRODUCT_TYPE_OFFSET, VER_NT_WORKSTATION);

            long dwlConditionMask = Kernel32FFM.VerSetConditionMask(0, VER_PRODUCT_TYPE, (byte) VER_EQUAL);

            if (Kernel32FFM.VerifyVersionInfoW(osvi, VER_PRODUCT_TYPE, dwlConditionMask)) {
                // Product type IS VER_NT_WORKSTATION, so not a server
                return false;
            }
            // VerifyVersionInfoW returned false; check why
            int error = Kernel32FFM.GetLastError().orElse(0);
            if (error == ERROR_OLD_WIN_VERSION) {
                // Condition not met: product type is not VER_NT_WORKSTATION, so it is a server
                return true;
            }
            LOG.debug("VerifyVersionInfoW failed with unexpected error code: {}", error);
            return false;
        }
    }
}
