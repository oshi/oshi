package oshi.ffm.windows;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;

import static java.lang.foreign.ValueLayout.JAVA_INT;

public final class WinNTFFM {

    private WinNTFFM() {}

    // --- Constants (access rights, privilege flags) ---
    public static final int SE_PRIVILEGE_ENABLED    = 0x00000002;
    public static final int TOKEN_QUERY             = 0x0008;
    public static final int TOKEN_ADJUST_PRIVILEGES = 0x0020;

    // --- Struct layouts ---
    // typedef struct _LUID { DWORD LowPart; LONG HighPart; } LUID;
    public static final GroupLayout LUID = MemoryLayout.structLayout(
        JAVA_INT.withName("LowPart"),
        JAVA_INT.withName("HighPart")
    );

    // typedef struct _LUID_AND_ATTRIBUTES {
    //   LUID Luid;
    //   DWORD Attributes;
    // }
    public static final GroupLayout LUID_AND_ATTRIBUTES = MemoryLayout.structLayout(
        LUID.withName("Luid"),
        JAVA_INT.withName("Attributes")
    );

    // typedef struct _TOKEN_PRIVILEGES {
    //   DWORD PrivilegeCount;
    //   LUID_AND_ATTRIBUTES Privileges[ANYSIZE_ARRAY];
    // }
    // Here we define a layout with 1 privilege entry
    public static final GroupLayout TOKEN_PRIVILEGES_1 = MemoryLayout.structLayout(
        JAVA_INT.withName("PrivilegeCount"),
        LUID_AND_ATTRIBUTES.withName("Privileges")
    );
}
