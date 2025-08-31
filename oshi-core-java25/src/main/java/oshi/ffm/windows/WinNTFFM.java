/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;

import static java.lang.foreign.ValueLayout.JAVA_INT;

public interface WinNTFFM {

    int SE_PRIVILEGE_ENABLED = 0x00000002;
    int TOKEN_QUERY = 0x0008;
    int TOKEN_ADJUST_PRIVILEGES = 0x0020;

    GroupLayout LUID = MemoryLayout.structLayout(JAVA_INT.withName("LowPart"), JAVA_INT.withName("HighPart"));

    GroupLayout LUID_AND_ATTRIBUTES = MemoryLayout.structLayout(LUID.withName("Luid"), JAVA_INT.withName("Attributes"));

    GroupLayout TOKEN_PRIVILEGES = MemoryLayout.structLayout(JAVA_INT.withName("PrivilegeCount"),
            LUID_AND_ATTRIBUTES.withName("Privileges"));

    long OFFSET_PRIVILEGE_COUNT = TOKEN_PRIVILEGES.byteOffset(MemoryLayout.PathElement.groupElement("PrivilegeCount"));

    long OFFSET_LUID = TOKEN_PRIVILEGES.byteOffset(MemoryLayout.PathElement.groupElement("Privileges"),
            MemoryLayout.PathElement.groupElement("Luid"));

    long OFFSET_ATTRIBUTES = TOKEN_PRIVILEGES.byteOffset(MemoryLayout.PathElement.groupElement("Privileges"),
            MemoryLayout.PathElement.groupElement("Attributes"));
}
