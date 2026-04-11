/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;

public interface WinNTFFM {

    int EVENTLOG_BACKWARDS_READ = 0x0008;
    int EVENTLOG_SEQUENTIAL_READ = 0x0001;
    int KEY_READ = 0x20019;
    int KEY_WOW64_64KEY = 0x0100;
    int KEY_WOW64_32KEY = 0x0200;
    int REG_SZ = 1;
    int REG_EXPAND_SZ = 2;
    int REG_DWORD = 4;
    int SE_PRIVILEGE_ENABLED = 0x00000002;
    int TOKEN_QUERY = 0x0008;
    int TOKEN_ADJUST_PRIVILEGES = 0x0020;
    int TokenElevation = 20;

    // Process access rights
    int PROCESS_QUERY_INFORMATION = 0x0400;
    int PROCESS_QUERY_LIMITED_INFORMATION = 0x1000;
    int PROCESS_VM_READ = 0x0010;

    // CreateFile constants
    int GENERIC_READ = 0x80000000;
    int GENERIC_WRITE = 0x40000000;
    int FILE_SHARE_READ = 0x00000001;
    int FILE_SHARE_WRITE = 0x00000002;
    int OPEN_EXISTING = 3;
    int FILE_ATTRIBUTE_NORMAL = 0x00000080;
    long INVALID_HANDLE_VALUE = -1L;

    // Version comparison constants
    int VER_EQUAL = 1;
    int VER_GREATER_EQUAL = 3;
    int VER_MINORVERSION = 0x0000001;
    int VER_MAJORVERSION = 0x0000002;
    int VER_SERVICEPACKMAJOR = 0x0000020;
    int VER_PRODUCT_TYPE = 0x0000080;
    byte VER_NT_WORKSTATION = 0x0000001;

    // Windows version constants (encoded as major << 8 | minor)
    short WIN32_WINNT_WINXP = 0x0501;
    short WIN32_WINNT_VISTA = 0x0600;
    short WIN32_WINNT_WIN7 = 0x0601;
    short WIN32_WINNT_WIN8 = 0x0602;
    short WIN32_WINNT_WINBLUE = 0x0603;
    short WIN32_WINNT_WIN10 = 0x0A00;

    // OSVERSIONINFOEX structure: 284 bytes
    // dwOSVersionInfoSize(4) + dwMajorVersion(4) + dwMinorVersion(4) + dwBuildNumber(4) + dwPlatformId(4)
    // + szCSDVersion(128 chars = 256 bytes) + wServicePackMajor(2) + wServicePackMinor(2)
    // + wSuiteMask(2) + wProductType(1) + wReserved(1)
    StructLayout OSVERSIONINFOEX = structLayout(JAVA_INT.withName("dwOSVersionInfoSize"),
            JAVA_INT.withName("dwMajorVersion"), JAVA_INT.withName("dwMinorVersion"),
            JAVA_INT.withName("dwBuildNumber"), JAVA_INT.withName("dwPlatformId"),
            MemoryLayout.sequenceLayout(128, JAVA_CHAR).withName("szCSDVersion"),
            JAVA_SHORT.withName("wServicePackMajor"), JAVA_SHORT.withName("wServicePackMinor"),
            JAVA_SHORT.withName("wSuiteMask"), JAVA_BYTE.withName("wProductType"), JAVA_BYTE.withName("wReserved"));

    long OSVERSIONINFOEX_MAJOR_VERSION_OFFSET = OSVERSIONINFOEX
            .byteOffset(MemoryLayout.PathElement.groupElement("dwMajorVersion"));
    long OSVERSIONINFOEX_MINOR_VERSION_OFFSET = OSVERSIONINFOEX
            .byteOffset(MemoryLayout.PathElement.groupElement("dwMinorVersion"));
    long OSVERSIONINFOEX_SP_MAJOR_OFFSET = OSVERSIONINFOEX
            .byteOffset(MemoryLayout.PathElement.groupElement("wServicePackMajor"));
    long OSVERSIONINFOEX_PRODUCT_TYPE_OFFSET = OSVERSIONINFOEX
            .byteOffset(MemoryLayout.PathElement.groupElement("wProductType"));

    StructLayout EVENTLOGRECORD = structLayout(JAVA_INT.withName("Length"), JAVA_INT.withName("Reserved"),
            JAVA_INT.withName("RecordNumber"), JAVA_INT.withName("TimeGenerated"), JAVA_INT.withName("TimeWritten"),
            JAVA_INT.withName("EventID"), JAVA_SHORT.withName("EventType"), JAVA_SHORT.withName("NumStrings"),
            JAVA_SHORT.withName("EventCategory"), JAVA_SHORT.withName("ReservedFlags"), MemoryLayout.paddingLayout(4),
            JAVA_INT.withName("ClosingRecordNumber"), JAVA_INT.withName("StringOffset"),
            JAVA_INT.withName("UserSidLength"), JAVA_INT.withName("UserSidOffset"), JAVA_INT.withName("DataLength"),
            JAVA_INT.withName("DataOffset"));

    StructLayout LUID = structLayout(JAVA_INT.withName("LowPart"), JAVA_INT.withName("HighPart"));

    StructLayout LUID_AND_ATTRIBUTES = structLayout(LUID.withName("Luid"), JAVA_INT.withName("Attributes"));

    StructLayout PERFORMANCE_INFORMATION = structLayout(JAVA_INT.withName("cb"), MemoryLayout.paddingLayout(4),
            JAVA_LONG.withName("CommitTotal"), JAVA_LONG.withName("CommitLimit"), JAVA_LONG.withName("CommitPeak"),
            JAVA_LONG.withName("PhysicalTotal"), JAVA_LONG.withName("PhysicalAvailable"),
            JAVA_LONG.withName("SystemCache"), JAVA_LONG.withName("KernelTotal"), JAVA_LONG.withName("KernelPaged"),
            JAVA_LONG.withName("KernelNonpaged"), JAVA_LONG.withName("PageSize"), JAVA_INT.withName("HandleCount"),
            JAVA_INT.withName("ProcessCount"), JAVA_INT.withName("ThreadCount"), MemoryLayout.paddingLayout(4));

    StructLayout TOKEN_PRIVILEGES = structLayout(JAVA_INT.withName("PrivilegeCount"),
            LUID_AND_ATTRIBUTES.withName("Privileges"));

    StructLayout TOKEN_ELEVATION = structLayout(JAVA_INT.withName("TokenIsElevated"));

    long OFFSET_TIME_GENERATED = EVENTLOGRECORD.byteOffset(MemoryLayout.PathElement.groupElement("TimeGenerated"));

    long OFFSET_EVENTID = EVENTLOGRECORD.byteOffset(MemoryLayout.PathElement.groupElement("EventID"));

    long OFFSET_LENGTH = EVENTLOGRECORD.byteOffset(MemoryLayout.PathElement.groupElement("Length"));

    long TOKEN_PRIVILEGES_PRIVILEGE_COUNT_OFFSET = TOKEN_PRIVILEGES
            .byteOffset(MemoryLayout.PathElement.groupElement("PrivilegeCount"));

    long TOKEN_PRIVILEGES_LUID_OFFSET = TOKEN_PRIVILEGES.byteOffset(MemoryLayout.PathElement.groupElement("Privileges"),
            MemoryLayout.PathElement.groupElement("Luid"));

    long TOKEN_PRIVILEGES_ATTRIBUTES_OFFSET = TOKEN_PRIVILEGES.byteOffset(
            MemoryLayout.PathElement.groupElement("Privileges"), MemoryLayout.PathElement.groupElement("Attributes"));

}
