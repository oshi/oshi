/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;

import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

public interface WinNTFFM {

    int EVENTLOG_BACKWARDS_READ = 0x0008;
    int EVENTLOG_SEQUENTIAL_READ = 0x0001;
    int SE_PRIVILEGE_ENABLED = 0x00000002;
    int TOKEN_QUERY = 0x0008;
    int TOKEN_ADJUST_PRIVILEGES = 0x0020;
    int TokenElevation = 20;

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
