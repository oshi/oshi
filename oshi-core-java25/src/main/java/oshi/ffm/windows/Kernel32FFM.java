/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

public final class Kernel32FFM extends WindowsForeignFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(Kernel32FFM.class);

    private static final SymbolLookup K32 = lib("Kernel32");

    private static final MethodHandle CloseHandle = downcall(K32, "CloseHandle", JAVA_INT, ADDRESS);

    public static OptionalInt CloseHandle(MemorySegment handle) {
        try {
            return OptionalInt.of((int) CloseHandle.invokeExact(handle));
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.closeHandle failed", t);
            return OptionalInt.empty();
        }
    }

    private static final MethodHandle FindFirstVolume = downcall(K32, "FindFirstVolumeW", ADDRESS, ADDRESS, JAVA_INT);

    public static Optional<MemorySegment> FindFirstVolume(MemorySegment lpszVolumeName, int cchBufferLength) {
        try {
            MemorySegment handle = (MemorySegment) FindFirstVolume.invokeExact(lpszVolumeName, cchBufferLength);
            if (handle == null || handle.equals(MemorySegment.NULL)) {
                int error = (int) GetLastError.invokeExact();
                LOG.error("FindFirstVolume failed with error: {}", error);
                return Optional.empty();
            }
            return Optional.of(handle);
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.FindFirstVolume failed", t);
            return Optional.empty();
        }
    }

    private static final MethodHandle FindNextVolume = downcall(K32, "FindNextVolumeW", JAVA_INT, ADDRESS, ADDRESS,
            JAVA_INT);

    public static OptionalInt FindNextVolume(MemorySegment hFindVolume, MemorySegment lpszVolumeName,
            int cchBufferLength) {
        try {
            int result = (int) FindNextVolume.invokeExact(hFindVolume, lpszVolumeName, cchBufferLength);
            return OptionalInt.of(result);
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.FindNextVolume failed: {}", t.getMessage());
            return OptionalInt.empty();
        }
    }

    private static final MethodHandle FindVolumeClose = downcall(K32, "FindVolumeClose", JAVA_INT, ADDRESS);

    public static OptionalInt FindVolumeClose(MemorySegment hFindVolume) {
        try {
            int result = (int) FindVolumeClose.invokeExact(hFindVolume);
            return OptionalInt.of(result);
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.FindVolumeClose failed: {}", t.getMessage());
            return OptionalInt.empty();
        }
    }

    private static final MethodHandle GetComputerName = downcall(K32, "GetComputerNameW", JAVA_INT, ADDRESS, ADDRESS);

    public static Optional<String> GetComputerName() {
        try (Arena arena = Arena.ofConfined()) {
            int maxLen = 32;

            MemorySegment buffer = arena.allocate(JAVA_CHAR, maxLen);
            MemorySegment sizeSegment = arena.allocate(JAVA_INT);
            sizeSegment.set(ValueLayout.JAVA_INT, 0, maxLen);

            boolean success = isSuccess((int) GetComputerName.invokeExact(buffer, sizeSegment));
            if (!success) {
                int errorCode = (int) GetLastError.invokeExact();
                LOG.error("Failed to get computer name name. Error code: {}", errorCode);
                return Optional.empty();
            }

            return Optional.of(readWideString(buffer));
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetComputerName failed: {}", t.getMessage());
            return Optional.empty();
        }
    }

    private static final MethodHandle GetComputerNameEx = downcall(K32, "GetComputerNameExW", JAVA_INT, JAVA_INT,
            ADDRESS, ADDRESS);

    public static Optional<String> GetComputerNameEx() {
        try (Arena arena = Arena.ofConfined()) {
            int maxLen = 256;

            MemorySegment buffer = arena.allocate(JAVA_CHAR, maxLen);
            MemorySegment sizeSegment = arena.allocate(JAVA_INT);
            sizeSegment.set(ValueLayout.JAVA_INT, 0, maxLen);

            final int COMPUTER_NAME_DNS_DOMAIN_FULLY_QUALIFIED = 3;

            boolean success = isSuccess(
                    (int) GetComputerNameEx.invokeExact(COMPUTER_NAME_DNS_DOMAIN_FULLY_QUALIFIED, buffer, sizeSegment));
            if (!success) {
                int errorCode = (int) GetLastError.invokeExact();
                LOG.error("Failed to get DNS domain name. Error code: {}", errorCode);
                return Optional.empty();
            }

            return Optional.of(readWideString(buffer));
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetComputerNameEx failed: {}", t.getMessage());
            return Optional.empty();
        }
    }

    private static final MethodHandle GetCurrentProcess = downcall(K32, "GetCurrentProcess", ADDRESS);

    public static Optional<MemorySegment> GetCurrentProcess() {
        try {
            return Optional.of((MemorySegment) GetCurrentProcess.invokeExact());
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetCurrentProcess failed: {}", t.getMessage());
            return Optional.empty();
        }
    }

    private static final MethodHandle GetCurrentProcessId = downcall(K32, "GetCurrentProcessId", JAVA_INT);

    public static OptionalInt GetCurrentProcessId() {
        try {
            return OptionalInt.of((int) GetCurrentProcessId.invokeExact());
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetCurrentProcessId failed: {}", t.getMessage());
            return OptionalInt.empty();
        }
    }

    private static final MethodHandle GetLastError = downcall(K32, "GetLastError", JAVA_INT);

    public static OptionalInt GetLastError() {
        try {
            return OptionalInt.of((int) GetLastError.invokeExact());
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetLastError failed: {}", t.getMessage());
            return OptionalInt.empty();
        }
    }

    private static final MethodHandle GetCurrentThreadId = downcall(K32, "GetCurrentThreadId", JAVA_INT);

    public static OptionalInt GetCurrentThreadId() {
        try {
            int tid = (int) GetCurrentThreadId.invokeExact();
            return OptionalInt.of(tid);
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetCurrentThreadId failed: {}", t.getMessage());
            return OptionalInt.empty();
        }
    }

    private static final MethodHandle GetDiskFreeSpaceEx = downcall(K32, "GetDiskFreeSpaceExA", JAVA_INT, ADDRESS,
            ADDRESS, ADDRESS, ADDRESS);

    public static OptionalInt GetDiskFreeSpaceEx(MemorySegment lpDirectoryName,
            MemorySegment lpFreeBytesAvailableToCaller, MemorySegment lpTotalNumberOfBytes,
            MemorySegment lpTotalNumberOfFreeBytes) {
        try {
            int result = (int) GetDiskFreeSpaceEx.invokeExact(lpDirectoryName, lpFreeBytesAvailableToCaller,
                    lpTotalNumberOfBytes, lpTotalNumberOfFreeBytes);
            return OptionalInt.of(result);
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetDiskFreeSpaceEx failed: {}", t.getMessage());
            return OptionalInt.empty();
        }
    }

    private static final MethodHandle GetDriveType = downcall(K32, "GetDriveTypeW", JAVA_INT, ADDRESS);

    public static OptionalInt GetDriveType(MemorySegment lpRootPathName) {
        try {
            int result = (int) GetDriveType.invokeExact(lpRootPathName);
            return OptionalInt.of(result);
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetDriveType failed: {}", t.getMessage());
            return OptionalInt.empty();
        }
    }

    private static final MethodHandle GetVolumeInformation = downcall(K32, "GetVolumeInformationW", JAVA_INT, ADDRESS,
            ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT);

    public static OptionalInt GetVolumeInformation(MemorySegment lpRootPathName, MemorySegment lpVolumeNameBuffer,
            int nVolumeNameSize, MemorySegment lpVolumeSerialNumber, MemorySegment lpMaximumComponentLength,
            MemorySegment lpFileSystemFlags, MemorySegment lpFileSystemNameBuffer, int nFileSystemNameSize) {
        try {
            int result = (int) GetVolumeInformation.invokeExact(lpRootPathName, lpVolumeNameBuffer, nVolumeNameSize,
                    lpVolumeSerialNumber, lpMaximumComponentLength, lpFileSystemFlags, lpFileSystemNameBuffer,
                    nFileSystemNameSize);
            return OptionalInt.of(result);
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetVolumeInformation failed: {}", t.getMessage());
            return OptionalInt.empty();
        }
    }

    private static final MethodHandle GetVolumePathNamesForVolumeName = downcall(K32,
            "GetVolumePathNamesForVolumeNameW", JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS);

    public static OptionalInt GetVolumePathNamesForVolumeName(MemorySegment lpszVolumeName,
            MemorySegment lpszVolumePathNames, int cchBufferLength, MemorySegment lpcchReturnLength) {
        try {
            int result = (int) GetVolumePathNamesForVolumeName.invokeExact(lpszVolumeName, lpszVolumePathNames,
                    cchBufferLength, lpcchReturnLength);
            return OptionalInt.of(result);
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetVolumePathNamesForVolumeName failed: {}", t.getMessage());
            return OptionalInt.empty();
        }
    }

    private static final MethodHandle GetTickCount = downcall(K32, "GetTickCount64", JAVA_LONG);

    public static OptionalLong GetTickCount() {
        try {
            return OptionalLong.of((long) GetTickCount.invokeExact());
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetTickCount64 failed: {}", t.getMessage());
            return OptionalLong.empty();
        }
    }

    private static final MethodHandle SetErrorMode = downcall(K32, "SetErrorMode", JAVA_INT, JAVA_INT);

    public static OptionalInt SetErrorMode(int uMode) {
        try {
            return OptionalInt.of((int) SetErrorMode.invokeExact(uMode));
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.SetErrorMode failed", t);
            return OptionalInt.empty();
        }
    }
}
