/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.paddingLayout;
import static java.lang.foreign.MemoryLayout.sequenceLayout;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FFM bindings for NtDll functions and structures used for process information queries.
 */
public final class NtDllFFM extends WindowsForeignFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(NtDllFFM.class);

    private static final SymbolLookup NTDLL = lib("NtDll");

    public static final int PROCESS_BASIC_INFORMATION = 0;

    // UNICODE_STRING structure (16 bytes on 64-bit)
    public static final StructLayout UNICODE_STRING = structLayout(JAVA_SHORT.withName("Length"),
            JAVA_SHORT.withName("MaximumLength"), paddingLayout(4), // alignment padding
            ADDRESS.withName("Buffer"));

    public static final long UNICODE_STRING_LENGTH_OFFSET = UNICODE_STRING.byteOffset(groupElement("Length"));
    public static final long UNICODE_STRING_BUFFER_OFFSET = UNICODE_STRING.byteOffset(groupElement("Buffer"));

    // CURDIR structure
    public static final StructLayout CURDIR = structLayout(UNICODE_STRING.withName("DosPath"),
            ADDRESS.withName("Handle"));

    // STRING structure for ANSI strings
    public static final StructLayout STRING = structLayout(JAVA_SHORT.withName("Length"),
            JAVA_SHORT.withName("MaximumLength"), paddingLayout(4), ADDRESS.withName("Buffer"));

    // RTL_DRIVE_LETTER_CURDIR structure (24 bytes)
    public static final StructLayout RTL_DRIVE_LETTER_CURDIR = structLayout(JAVA_SHORT.withName("Flags"),
            JAVA_SHORT.withName("Length"), JAVA_INT.withName("TimeStamp"), STRING.withName("DosPath"));

    // PROCESS_BASIC_INFORMATION structure (48 bytes on 64-bit)
    public static final StructLayout PROCESS_BASIC_INFORMATION_STRUCT = structLayout(ADDRESS.withName("Reserved1"),
            ADDRESS.withName("PebBaseAddress"), sequenceLayout(4, ADDRESS).withName("Reserved2"));

    public static final long PBI_PEB_BASE_ADDRESS_OFFSET = PROCESS_BASIC_INFORMATION_STRUCT
            .byteOffset(groupElement("PebBaseAddress"));

    // PEB structure (partial - we only need ProcessParameters)
    // On 64-bit Windows, ProcessParameters is at offset 0x20
    public static final StructLayout PEB = structLayout(sequenceLayout(4, JAVA_INT).withName("pad"), // 16 bytes of
                                                                                                     // padding
                                                                                                     // (InheritedAddressSpace,
                                                                                                     // ReadImageFileExecOptions,
                                                                                                     // BeingDebugged,
                                                                                                     // BitField,
                                                                                                     // Mutant)
            sequenceLayout(2, ADDRESS).withName("pad2"), // 16 bytes (2 pointers: ImageBaseAddress, Ldr)
            ADDRESS.withName("ProcessParameters"));

    public static final long PEB_PROCESS_PARAMETERS_OFFSET = PEB.byteOffset(groupElement("ProcessParameters"));

    // RTL_USER_PROCESS_PARAMETERS structure (partial - we need key fields)
    // This is a large structure, we define the parts we need
    public static final StructLayout RTL_USER_PROCESS_PARAMETERS = structLayout(JAVA_INT.withName("MaximumLength"),
            JAVA_INT.withName("Length"), JAVA_INT.withName("Flags"), JAVA_INT.withName("DebugFlags"),
            ADDRESS.withName("ConsoleHandle"), JAVA_INT.withName("ConsoleFlags"), paddingLayout(4),
            ADDRESS.withName("StandardInput"), ADDRESS.withName("StandardOutput"), ADDRESS.withName("StandardError"),
            CURDIR.withName("CurrentDirectory"), UNICODE_STRING.withName("DllPath"),
            UNICODE_STRING.withName("ImagePathName"), UNICODE_STRING.withName("CommandLine"),
            ADDRESS.withName("Environment"), JAVA_INT.withName("StartingX"), JAVA_INT.withName("StartingY"),
            JAVA_INT.withName("CountX"), JAVA_INT.withName("CountY"), JAVA_INT.withName("CountCharsX"),
            JAVA_INT.withName("CountCharsY"), JAVA_INT.withName("FillAttribute"), JAVA_INT.withName("WindowFlags"),
            JAVA_INT.withName("ShowWindowFlags"), paddingLayout(4), UNICODE_STRING.withName("WindowTitle"),
            UNICODE_STRING.withName("DesktopInfo"), UNICODE_STRING.withName("ShellInfo"),
            UNICODE_STRING.withName("RuntimeData"),
            sequenceLayout(32, RTL_DRIVE_LETTER_CURDIR).withName("CurrentDirectories"),
            JAVA_LONG.withName("EnvironmentSize"), JAVA_LONG.withName("EnvironmentVersion"),
            ADDRESS.withName("PackageDependencyData"), JAVA_INT.withName("ProcessGroupId"),
            JAVA_INT.withName("LoaderThreads"), UNICODE_STRING.withName("RedirectionDllName"),
            UNICODE_STRING.withName("HeapPartitionName"), JAVA_LONG.withName("DefaultThreadpoolCpuSetMasks"),
            JAVA_INT.withName("DefaultThreadpoolCpuSetMaskCount"));

    public static final long UPP_CURRENT_DIRECTORY_OFFSET = RTL_USER_PROCESS_PARAMETERS
            .byteOffset(groupElement("CurrentDirectory"));
    public static final long UPP_COMMAND_LINE_OFFSET = RTL_USER_PROCESS_PARAMETERS
            .byteOffset(groupElement("CommandLine"));
    public static final long UPP_ENVIRONMENT_OFFSET = RTL_USER_PROCESS_PARAMETERS
            .byteOffset(groupElement("Environment"));
    public static final long UPP_ENVIRONMENT_SIZE_OFFSET = RTL_USER_PROCESS_PARAMETERS
            .byteOffset(groupElement("EnvironmentSize"));

    // NtQueryInformationProcess - returns NTSTATUS (int)
    // NTSTATUS NtQueryInformationProcess(HANDLE ProcessHandle, PROCESSINFOCLASS ProcessInformationClass,
    // PVOID ProcessInformation, ULONG ProcessInformationLength, PULONG ReturnLength)
    private static final MethodHandle NtQueryInformationProcess = downcall(NTDLL, "NtQueryInformationProcess", JAVA_INT,
            ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS);

    /**
     * Retrieves information about the specified process.
     *
     * @param processHandle            A handle to the process
     * @param processInformationClass  The type of process information to be retrieved
     * @param processInformation       Buffer to receive the requested information
     * @param processInformationLength Size of the buffer
     * @param returnLength             Pointer to receive the actual size of the information returned
     * @return NTSTATUS code (0 indicates success)
     */
    public static int NtQueryInformationProcess(MemorySegment processHandle, int processInformationClass,
            MemorySegment processInformation, int processInformationLength, MemorySegment returnLength) {
        try {
            return (int) NtQueryInformationProcess.invokeExact(processHandle, processInformationClass,
                    processInformation, processInformationLength, returnLength);
        } catch (Throwable t) {
            LOG.debug("NtDllFFM.NtQueryInformationProcess failed", t);
            return -1;
        }
    }

    /**
     * Reads a UNICODE_STRING from process memory.
     *
     * @param processHandle Handle to the process
     * @param unicodeString The UNICODE_STRING segment containing Length and Buffer pointer
     * @param arena         Arena for memory allocation
     * @return The string content, or empty string on failure
     */
    public static String readUnicodeString(MemorySegment processHandle, MemorySegment unicodeString, Arena arena) {
        short length = unicodeString.get(JAVA_SHORT, UNICODE_STRING_LENGTH_OFFSET);
        if (length <= 0) {
            return "";
        }

        MemorySegment bufferPtr = unicodeString.get(ADDRESS, UNICODE_STRING_BUFFER_OFFSET);
        if (bufferPtr.address() == 0) {
            return "";
        }

        // Allocate buffer for the string content plus null terminator
        MemorySegment buffer = arena.allocate(length + 2L);
        buffer.fill((byte) 0);

        MemorySegment bytesRead = arena.allocate(JAVA_LONG);
        boolean success = Kernel32FFM.ReadProcessMemory(processHandle, bufferPtr, buffer, length, bytesRead);
        if (success && bytesRead.get(JAVA_LONG, 0) > 0) {
            return readWideString(buffer);
        }
        return "";
    }
}
