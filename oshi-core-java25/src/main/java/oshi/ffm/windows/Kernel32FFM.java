/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.windows.WinNTFFM.INVALID_HANDLE_VALUE;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Kernel32FFM extends WindowsForeignFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(Kernel32FFM.class);

    private static final SymbolLookup K32 = lib("Kernel32");

    /**
     * Checks if a handle represents INVALID_HANDLE_VALUE.
     *
     * @param handle the handle to check
     * @return true if the handle is null or equals INVALID_HANDLE_VALUE
     */
    public static boolean isInvalidHandle(MemorySegment handle) {
        return handle == null || handle.address() == INVALID_HANDLE_VALUE;
    }

    private static final MethodHandle CloseHandle = downcall(K32, "CloseHandle", JAVA_INT, ADDRESS);

    public static OptionalInt CloseHandle(MemorySegment handle) {
        try {
            return OptionalInt.of((int) CloseHandle.invokeExact(handle));
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.closeHandle failed", t);
            return OptionalInt.empty();
        }
    }

    private static final MethodHandle CreateFile = downcall(K32, "CreateFileW", ADDRESS, ADDRESS, JAVA_INT, JAVA_INT,
            ADDRESS, JAVA_INT, JAVA_INT, ADDRESS);

    public static Optional<MemorySegment> CreateFile(MemorySegment lpFileName, int dwDesiredAccess, int dwShareMode,
            int dwCreationDisposition, int dwFlagsAndAttributes) {
        try {
            MemorySegment handle = (MemorySegment) CreateFile.invokeExact(lpFileName, dwDesiredAccess, dwShareMode,
                    MemorySegment.NULL, dwCreationDisposition, dwFlagsAndAttributes, MemorySegment.NULL);
            if (isInvalidHandle(handle)) {
                return Optional.empty();
            }
            return Optional.of(handle);
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.CreateFile failed", t);
            return Optional.empty();
        }
    }

    private static final MethodHandle DeviceIoControl = downcall(K32, "DeviceIoControl", JAVA_INT, ADDRESS, JAVA_INT,
            ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS);

    public static boolean DeviceIoControl(MemorySegment hDevice, int dwIoControlCode, MemorySegment lpInBuffer,
            int nInBufferSize, MemorySegment lpOutBuffer, int nOutBufferSize) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment lpBytesReturned = arena.allocate(JAVA_INT);
            return isSuccess((int) DeviceIoControl.invokeExact(hDevice, dwIoControlCode, lpInBuffer, nInBufferSize,
                    lpOutBuffer, nOutBufferSize, lpBytesReturned, MemorySegment.NULL));
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.DeviceIoControl failed", t);
            return false;
        }
    }

    private static final MethodHandle FindFirstVolume = downcall(K32, "FindFirstVolumeW", ADDRESS, ADDRESS, JAVA_INT);

    public static Optional<MemorySegment> FindFirstVolume(MemorySegment lpszVolumeName, int cchBufferLength) {
        try {
            MemorySegment handle = (MemorySegment) FindFirstVolume.invokeExact(lpszVolumeName, cchBufferLength);
            if (isInvalidHandle(handle)) {
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

    private static final MethodHandle GetDiskFreeSpaceEx = downcall(K32, "GetDiskFreeSpaceExW", JAVA_INT, ADDRESS,
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

    private static final MethodHandle GetVolumeNameForVolumeMountPoint = downcall(K32,
            "GetVolumeNameForVolumeMountPointW", JAVA_INT, ADDRESS, ADDRESS, JAVA_INT);

    /**
     * Retrieves a volume GUID path for the volume that is associated with the specified volume mount point.
     *
     * @param lpszVolumeMountPoint the path of a mounted folder or a drive letter (e.g., "C:\\")
     * @param lpszVolumeName       buffer to receive the volume GUID path
     * @param cchBufferLength      size of the buffer in characters
     * @return nonzero if successful, zero otherwise
     */
    public static OptionalInt GetVolumeNameForVolumeMountPoint(MemorySegment lpszVolumeMountPoint,
            MemorySegment lpszVolumeName, int cchBufferLength) {
        try {
            int result = (int) GetVolumeNameForVolumeMountPoint.invokeExact(lpszVolumeMountPoint, lpszVolumeName,
                    cchBufferLength);
            return OptionalInt.of(result);
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetVolumeNameForVolumeMountPoint failed: {}", t.getMessage());
            return OptionalInt.empty();
        }
    }

    private static final MethodHandle OpenProcess = downcall(K32, "OpenProcess", ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT);

    /**
     * Opens an existing local process object.
     *
     * @param dwDesiredAccess The access to the process object
     * @param bInheritHandle  If TRUE, processes created by this process will inherit the handle
     * @param dwProcessId     The identifier of the local process to be opened
     * @return Handle to the process, or null segment if failed
     */
    public static Optional<MemorySegment> OpenProcess(int dwDesiredAccess, boolean bInheritHandle, int dwProcessId) {
        try {
            MemorySegment handle = (MemorySegment) OpenProcess.invokeExact(dwDesiredAccess, bInheritHandle ? 1 : 0,
                    dwProcessId);
            if (handle == null || handle.address() == 0) {
                return Optional.empty();
            }
            return Optional.of(handle);
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.OpenProcess failed: {}", t.getMessage());
            return Optional.empty();
        }
    }

    private static final MethodHandle GetProcessAffinityMask = downcall(K32, "GetProcessAffinityMask", JAVA_INT,
            ADDRESS, ADDRESS, ADDRESS);

    /**
     * Retrieves the process affinity mask for the specified process and the system affinity mask.
     *
     * @param hProcess          Handle to the process
     * @param lpProcessAffinity Pointer to receive the affinity mask for the process
     * @param lpSystemAffinity  Pointer to receive the affinity mask for the system
     * @return true if successful
     */
    public static boolean GetProcessAffinityMask(MemorySegment hProcess, MemorySegment lpProcessAffinity,
            MemorySegment lpSystemAffinity) {
        try {
            return isSuccess((int) GetProcessAffinityMask.invokeExact(hProcess, lpProcessAffinity, lpSystemAffinity));
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetProcessAffinityMask failed: {}", t.getMessage());
            return false;
        }
    }

    private static final MethodHandle IsWow64Process = downcall(K32, "IsWow64Process", JAVA_INT, ADDRESS, ADDRESS);

    /**
     * Determines whether the specified process is running under WOW64.
     *
     * @param hProcess     Handle to the process
     * @param Wow64Process Pointer to receive a value indicating WOW64 status
     * @return true if the function succeeds
     */
    public static boolean IsWow64Process(MemorySegment hProcess, MemorySegment Wow64Process) {
        try {
            return isSuccess((int) IsWow64Process.invokeExact(hProcess, Wow64Process));
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.IsWow64Process failed: {}", t.getMessage());
            return false;
        }
    }

    private static final MethodHandle ReadProcessMemory = downcall(K32, "ReadProcessMemory", JAVA_INT, ADDRESS, ADDRESS,
            ADDRESS, JAVA_LONG, ADDRESS);

    /**
     * Reads data from an area of memory in a specified process.
     *
     * @param hProcess            Handle to the process with memory to be read
     * @param lpBaseAddress       Pointer to the base address in the specified process from which to read
     * @param lpBuffer            Buffer to receive the contents
     * @param nSize               Number of bytes to be read
     * @param lpNumberOfBytesRead Pointer to receive the number of bytes transferred
     * @return true if successful
     */
    public static boolean ReadProcessMemory(MemorySegment hProcess, MemorySegment lpBaseAddress, MemorySegment lpBuffer,
            long nSize, MemorySegment lpNumberOfBytesRead) {
        try {
            return isSuccess(
                    (int) ReadProcessMemory.invokeExact(hProcess, lpBaseAddress, lpBuffer, nSize, lpNumberOfBytesRead));
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.ReadProcessMemory failed: {}", t.getMessage());
            return false;
        }
    }

    private static final MethodHandle QueryFullProcessImageNameW = downcall(K32, "QueryFullProcessImageNameW", JAVA_INT,
            ADDRESS, JAVA_INT, ADDRESS, ADDRESS);

    /**
     * Retrieves the full name of the executable image for the specified process.
     *
     * @param hProcess  Handle to the process
     * @param dwFlags   Flags (0 for Win32 path format, PROCESS_NAME_NATIVE for native system path format)
     * @param lpExeName Buffer to receive the path
     * @param lpdwSize  On input, size of the buffer in characters. On output, size of the path in characters
     * @return true if successful
     */
    public static boolean QueryFullProcessImageName(MemorySegment hProcess, int dwFlags, MemorySegment lpExeName,
            MemorySegment lpdwSize) {
        try {
            return isSuccess((int) QueryFullProcessImageNameW.invokeExact(hProcess, dwFlags, lpExeName, lpdwSize));
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.QueryFullProcessImageName failed: {}", t.getMessage());
            return false;
        }
    }

    /**
     * Retrieves the full name of the executable image for the specified process.
     *
     * @param hProcess Handle to the process
     * @param dwFlags  Flags (0 for Win32 path format)
     * @param arena    Arena for memory allocation
     * @return The full process image name, or empty if failed
     */
    public static Optional<String> QueryFullProcessImageName(MemorySegment hProcess, int dwFlags, Arena arena) {
        int maxPath = 260;
        // Retry with growing buffer if needed (up to 32K which is max path on Windows)
        while (maxPath <= 32768) {
            MemorySegment buffer = arena.allocate(JAVA_CHAR, maxPath);
            MemorySegment sizeSegment = arena.allocate(JAVA_INT);
            sizeSegment.set(JAVA_INT, 0, maxPath);

            if (QueryFullProcessImageName(hProcess, dwFlags, buffer, sizeSegment)) {
                return Optional.of(readWideString(buffer));
            }
            // Check if buffer was too small (returned size >= maxPath)
            int returnedSize = sizeSegment.get(JAVA_INT, 0);
            if (returnedSize < maxPath) {
                // Failure wasn't due to buffer size
                break;
            }
            // Double the buffer size and retry
            maxPath *= 2;
        }
        return Optional.empty();
    }

    private static final MethodHandle LocalFree = downcall(K32, "LocalFree", ADDRESS, ADDRESS);

    /**
     * Frees the specified local memory object and invalidates its handle.
     *
     * @param hMem A handle to the local memory object
     * @return If the function succeeds, the return value is NULL
     */
    public static MemorySegment LocalFree(MemorySegment hMem) {
        try {
            return (MemorySegment) LocalFree.invokeExact(hMem);
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.LocalFree failed: {}", t.getMessage());
            return hMem;
        }
    }

    private static final MethodHandle VerSetConditionMask = downcall(K32, "VerSetConditionMask", JAVA_LONG, JAVA_LONG,
            JAVA_INT, JAVA_BYTE);

    /**
     * Sets the bits of a 64-bit value to indicate the comparison operator to use for a specified operating system
     * version attribute.
     *
     * @param conditionMask A value to be passed as the dwlConditionMask parameter of VerifyVersionInfo
     * @param typeMask      A mask that indicates the member of the OSVERSIONINFOEX structure whose comparison operator
     *                      is being set
     * @param condition     The operator to be used for the comparison
     * @return The condition mask value
     */
    public static long VerSetConditionMask(long conditionMask, int typeMask, byte condition) {
        try {
            return (long) VerSetConditionMask.invokeExact(conditionMask, typeMask, condition);
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.VerSetConditionMask failed: {}", t.getMessage());
            return 0L;
        }
    }

    private static final MethodHandle VerifyVersionInfoW = downcall(K32, "VerifyVersionInfoW", JAVA_INT, ADDRESS,
            JAVA_INT, JAVA_LONG);

    /**
     * Compares a set of operating system version requirements to the corresponding values for the currently running
     * version of the system.
     *
     * @param lpVersionInformation A pointer to an OSVERSIONINFOEX structure containing the operating system version
     *                             requirements to compare
     * @param dwTypeMask           A mask that indicates the members of the OSVERSIONINFOEX structure to be tested
     * @param dwlConditionMask     The type of comparison to be used for each lpVersionInformation member being compared
     * @return true if the currently running operating system satisfies the specified requirements
     */
    public static boolean VerifyVersionInfoW(MemorySegment lpVersionInformation, int dwTypeMask,
            long dwlConditionMask) {
        try {
            return isSuccess((int) VerifyVersionInfoW.invokeExact(lpVersionInformation, dwTypeMask, dwlConditionMask));
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.VerifyVersionInfoW failed: {}", t.getMessage());
            return false;
        }
    }
}
