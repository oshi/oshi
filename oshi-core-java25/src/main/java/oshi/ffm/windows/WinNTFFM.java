/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;

import static java.lang.foreign.ValueLayout.JAVA_INT;

public interface WinNTFFM {

    int SE_PRIVILEGE_ENABLED = 0x00000002;
    int TOKEN_QUERY = 0x0008;
    int TOKEN_ADJUST_PRIVILEGES = 0x0020;

    StructLayout LUID = MemoryLayout.structLayout(JAVA_INT.withName("LowPart"), JAVA_INT.withName("HighPart"));

    StructLayout LUID_AND_ATTRIBUTES = MemoryLayout.structLayout(LUID.withName("Luid"),
            JAVA_INT.withName("Attributes"));

    StructLayout TOKEN_PRIVILEGES = MemoryLayout.structLayout(JAVA_INT.withName("PrivilegeCount"),
            LUID_AND_ATTRIBUTES.withName("Privileges"));

    long TOKEN_PRIVILEGES_PRIVILEGE_COUNT_OFFSET = TOKEN_PRIVILEGES
            .byteOffset(MemoryLayout.PathElement.groupElement("PrivilegeCount"));

    long TOKEN_PRIVILEGES_LUID_OFFSET = TOKEN_PRIVILEGES.byteOffset(MemoryLayout.PathElement.groupElement("Privileges"),
            MemoryLayout.PathElement.groupElement("Luid"));

    long TOKEN_PRIVILEGES_ATTRIBUTES_OFFSET = TOKEN_PRIVILEGES.byteOffset(
            MemoryLayout.PathElement.groupElement("Privileges"), MemoryLayout.PathElement.groupElement("Attributes"));
}
