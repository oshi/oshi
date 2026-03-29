/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows.com;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * FFM representation of the Windows GUID structure.
 * <p>
 * A GUID (Globally Unique Identifier) is a 128-bit value used to identify COM interfaces and classes.
 * </p>
 */
public final class GuidFFM {

    /**
     * The memory layout of a GUID structure (16 bytes).
     */
    public static final StructLayout LAYOUT = MemoryLayout.structLayout(JAVA_INT.withName("Data1"),
            JAVA_SHORT.withName("Data2"), JAVA_SHORT.withName("Data3"),
            MemoryLayout.sequenceLayout(8, JAVA_BYTE).withName("Data4"));

    /**
     * Size of a GUID in bytes.
     */
    public static final long SIZE = LAYOUT.byteSize();

    private GuidFFM() {
    }

    /**
     * Creates a GUID memory segment from its components.
     *
     * @param arena the arena for memory allocation
     * @param data1 the first 4 bytes (big-endian)
     * @param data2 the next 2 bytes (big-endian)
     * @param data3 the next 2 bytes (big-endian)
     * @param data4 the remaining 8 bytes
     * @return a memory segment containing the GUID
     */
    public static MemorySegment create(Arena arena, int data1, short data2, short data3, byte... data4) {
        if (data4.length != 8) {
            throw new IllegalArgumentException("Data4 must be exactly 8 bytes");
        }
        MemorySegment guid = arena.allocate(LAYOUT);
        guid.set(JAVA_INT, 0, data1);
        guid.set(JAVA_SHORT, 4, data2);
        guid.set(JAVA_SHORT, 6, data3);
        for (int i = 0; i < 8; i++) {
            guid.set(JAVA_BYTE, 8 + i, data4[i]);
        }
        return guid;
    }

    /**
     * Creates a GUID memory segment from its components using integer values for convenience.
     *
     * @param arena the arena for memory allocation
     * @param data1 the first 4 bytes
     * @param data2 the next 2 bytes
     * @param data3 the next 2 bytes
     * @param d4_0  data4[0]
     * @param d4_1  data4[1]
     * @param d4_2  data4[2]
     * @param d4_3  data4[3]
     * @param d4_4  data4[4]
     * @param d4_5  data4[5]
     * @param d4_6  data4[6]
     * @param d4_7  data4[7]
     * @return a memory segment containing the GUID
     */
    public static MemorySegment create(Arena arena, int data1, int data2, int data3, int d4_0, int d4_1, int d4_2,
            int d4_3, int d4_4, int d4_5, int d4_6, int d4_7) {
        byte[] data4 = new byte[] { (byte) d4_0, (byte) d4_1, (byte) d4_2, (byte) d4_3, (byte) d4_4, (byte) d4_5,
                (byte) d4_6, (byte) d4_7 };
        return create(arena, data1, (short) data2, (short) data3, data4);
    }

    // Well-known GUIDs for WMI

    /**
     * CLSID_WbemLocator: {4590F811-1D3A-11D0-891F-00AA004B2E24}
     *
     * @param arena the arena for memory allocation
     * @return a memory segment containing the CLSID_WbemLocator GUID
     */
    public static MemorySegment CLSID_WbemLocator(Arena arena) {
        return create(arena, 0x4590f811, 0x1d3a, 0x11d0, 0x89, 0x1f, 0x00, 0xaa, 0x00, 0x4b, 0x2e, 0x24);
    }

    /**
     * IID_IWbemLocator: {DC12A687-737F-11CF-884D-00AA004B2E24}
     *
     * @param arena the arena for memory allocation
     * @return a memory segment containing the IID_IWbemLocator GUID
     */
    public static MemorySegment IID_IWbemLocator(Arena arena) {
        return create(arena, 0xdc12a687, 0x737f, 0x11cf, 0x88, 0x4d, 0x00, 0xaa, 0x00, 0x4b, 0x2e, 0x24);
    }

    /**
     * IID_IUnknown: {00000000-0000-0000-C000-000000000046}
     *
     * @param arena the arena for memory allocation
     * @return a memory segment containing the IID_IUnknown GUID
     */
    public static MemorySegment IID_IUnknown(Arena arena) {
        return create(arena, 0x00000000, 0x0000, 0x0000, 0xc0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46);
    }
}
