/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;

/**
 * FFM struct layouts for Windows Performance Counter structures read from HKEY_PERFORMANCE_DATA.
 * <p>
 * The registry data is organized as:
 *
 * <pre>
 * [ PERF_DATA_BLOCK ]                          -- data header with timestamps
 *   [ PERF_OBJECT_TYPE ]                       -- object type header (e.g., Process, Thread)
 *     [ PERF_COUNTER_DEFINITION ] × NumCounters -- counter metadata (offset, size, index)
 *   [ PERF_INSTANCE_DEFINITION ] × NumInstances -- per-instance data:
 *     [ instance name (wide string) ]
 *     [ PERF_COUNTER_BLOCK ]                    -- raw counter values for this instance
 *       [ counter data bytes ]
 * </pre>
 *
 * @see <a href="https://learn.microsoft.com/en-us/windows/win32/perfctrs/performance-counters-structures">Performance
 *      Counters Structures</a>
 */
public interface WinPerfFFM {

    // PERF_DATA_BLOCK: describes the performance data block header
    // https://learn.microsoft.com/en-us/windows/win32/api/winperf/ns-winperf-perf_data_block
    //
    // Fields:
    // WCHAR[4] Signature (8 bytes)
    // DWORD LittleEndian (4 bytes)
    // DWORD Version (4 bytes)
    // DWORD Revision (4 bytes)
    // DWORD TotalByteLength (4 bytes)
    // DWORD HeaderLength (4 bytes)
    // DWORD NumObjectTypes (4 bytes)
    // LONG DefaultObject (4 bytes)
    // SYSTEMTIME SystemTime (16 bytes: 8 × WORD)
    // LARGE_INTEGER PerfTime (8 bytes)
    // LARGE_INTEGER PerfFreq (8 bytes)
    // LARGE_INTEGER PerfTime100nSec (8 bytes)
    // DWORD SystemNameLength (4 bytes)
    // DWORD SystemNameOffset (4 bytes)
    StructLayout PERF_DATA_BLOCK = structLayout(JAVA_SHORT.withName("Signature0"), JAVA_SHORT.withName("Signature1"),
            JAVA_SHORT.withName("Signature2"), JAVA_SHORT.withName("Signature3"), JAVA_INT.withName("LittleEndian"),
            JAVA_INT.withName("Version"), JAVA_INT.withName("Revision"), JAVA_INT.withName("TotalByteLength"),
            JAVA_INT.withName("HeaderLength"), JAVA_INT.withName("NumObjectTypes"), JAVA_INT.withName("DefaultObject"),
            // 4 bytes padding for 8-byte alignment of LARGE_INTEGER fields
            MemoryLayout.paddingLayout(4),
            // SYSTEMTIME: 8 × WORD (16 bytes) — we only need the struct size, not individual fields
            MemoryLayout.paddingLayout(16).withName("SystemTime"), JAVA_LONG.withName("PerfTime"),
            JAVA_LONG.withName("PerfFreq"), JAVA_LONG.withName("PerfTime100nSec"),
            JAVA_INT.withName("SystemNameLength"), JAVA_INT.withName("SystemNameOffset"));

    long PERF_DATA_BLOCK_HeaderLength = PERF_DATA_BLOCK
            .byteOffset(MemoryLayout.PathElement.groupElement("HeaderLength"));
    long PERF_DATA_BLOCK_NumObjectTypes = PERF_DATA_BLOCK
            .byteOffset(MemoryLayout.PathElement.groupElement("NumObjectTypes"));
    long PERF_DATA_BLOCK_PerfTime100nSec = PERF_DATA_BLOCK
            .byteOffset(MemoryLayout.PathElement.groupElement("PerfTime100nSec"));

    // PERF_OBJECT_TYPE: describes object-specific performance information
    // https://learn.microsoft.com/en-us/windows/win32/api/winperf/ns-winperf-perf_object_type
    //
    // Fields:
    // DWORD TotalByteLength (4 bytes)
    // DWORD DefinitionLength (4 bytes)
    // DWORD HeaderLength (4 bytes)
    // DWORD ObjectNameTitleIndex (4 bytes)
    // DWORD ObjectNameTitle (4 bytes) — always 32-bit
    // DWORD ObjectHelpTitleIndex (4 bytes)
    // DWORD ObjectHelpTitle (4 bytes) — always 32-bit
    // DWORD DetailLevel (4 bytes)
    // DWORD NumCounters (4 bytes)
    // LONG DefaultCounter (4 bytes)
    // LONG NumInstances (4 bytes)
    // DWORD CodePage (4 bytes)
    // LARGE_INTEGER PerfTime (8 bytes)
    // LARGE_INTEGER PerfFreq (8 bytes)
    StructLayout PERF_OBJECT_TYPE = structLayout(JAVA_INT.withName("TotalByteLength"),
            JAVA_INT.withName("DefinitionLength"), JAVA_INT.withName("HeaderLength"),
            JAVA_INT.withName("ObjectNameTitleIndex"), JAVA_INT.withName("ObjectNameTitle"),
            JAVA_INT.withName("ObjectHelpTitleIndex"), JAVA_INT.withName("ObjectHelpTitle"),
            JAVA_INT.withName("DetailLevel"), JAVA_INT.withName("NumCounters"), JAVA_INT.withName("DefaultCounter"),
            JAVA_INT.withName("NumInstances"), JAVA_INT.withName("CodePage"), JAVA_LONG.withName("PerfTime"),
            JAVA_LONG.withName("PerfFreq"));

    long PERF_OBJECT_TYPE_TotalByteLength = PERF_OBJECT_TYPE
            .byteOffset(MemoryLayout.PathElement.groupElement("TotalByteLength"));
    long PERF_OBJECT_TYPE_DefinitionLength = PERF_OBJECT_TYPE
            .byteOffset(MemoryLayout.PathElement.groupElement("DefinitionLength"));
    long PERF_OBJECT_TYPE_HeaderLength = PERF_OBJECT_TYPE
            .byteOffset(MemoryLayout.PathElement.groupElement("HeaderLength"));
    long PERF_OBJECT_TYPE_ObjectNameTitleIndex = PERF_OBJECT_TYPE
            .byteOffset(MemoryLayout.PathElement.groupElement("ObjectNameTitleIndex"));
    long PERF_OBJECT_TYPE_NumCounters = PERF_OBJECT_TYPE
            .byteOffset(MemoryLayout.PathElement.groupElement("NumCounters"));
    long PERF_OBJECT_TYPE_NumInstances = PERF_OBJECT_TYPE
            .byteOffset(MemoryLayout.PathElement.groupElement("NumInstances"));

    // PERF_COUNTER_DEFINITION: describes a performance counter
    // https://learn.microsoft.com/en-us/windows/win32/api/winperf/ns-winperf-perf_counter_definition
    //
    // Fields:
    // DWORD ByteLength (4 bytes)
    // DWORD CounterNameTitleIndex (4 bytes)
    // DWORD CounterNameTitle (4 bytes) — always 32-bit
    // DWORD CounterHelpTitleIndex (4 bytes)
    // DWORD CounterHelpTitle (4 bytes) — always 32-bit
    // LONG DefaultScale (4 bytes)
    // DWORD DetailLevel (4 bytes)
    // DWORD CounterType (4 bytes)
    // DWORD CounterSize (4 bytes)
    // DWORD CounterOffset (4 bytes)
    StructLayout PERF_COUNTER_DEFINITION = structLayout(JAVA_INT.withName("ByteLength"),
            JAVA_INT.withName("CounterNameTitleIndex"), JAVA_INT.withName("CounterNameTitle"),
            JAVA_INT.withName("CounterHelpTitleIndex"), JAVA_INT.withName("CounterHelpTitle"),
            JAVA_INT.withName("DefaultScale"), JAVA_INT.withName("DetailLevel"), JAVA_INT.withName("CounterType"),
            JAVA_INT.withName("CounterSize"), JAVA_INT.withName("CounterOffset"));

    long PERF_COUNTER_DEF_ByteLength = PERF_COUNTER_DEFINITION
            .byteOffset(MemoryLayout.PathElement.groupElement("ByteLength"));
    long PERF_COUNTER_DEF_CounterNameTitleIndex = PERF_COUNTER_DEFINITION
            .byteOffset(MemoryLayout.PathElement.groupElement("CounterNameTitleIndex"));
    long PERF_COUNTER_DEF_CounterSize = PERF_COUNTER_DEFINITION
            .byteOffset(MemoryLayout.PathElement.groupElement("CounterSize"));
    long PERF_COUNTER_DEF_CounterOffset = PERF_COUNTER_DEFINITION
            .byteOffset(MemoryLayout.PathElement.groupElement("CounterOffset"));

    // PERF_INSTANCE_DEFINITION: describes an instance of a performance object
    // https://learn.microsoft.com/en-us/windows/win32/api/winperf/ns-winperf-perf_instance_definition
    //
    // Fields:
    // DWORD ByteLength (4 bytes)
    // DWORD ParentObjectTitleIndex (4 bytes)
    // DWORD ParentObjectInstance (4 bytes)
    // LONG UniqueID (4 bytes)
    // DWORD NameOffset (4 bytes)
    // DWORD NameLength (4 bytes)
    StructLayout PERF_INSTANCE_DEFINITION = structLayout(JAVA_INT.withName("ByteLength"),
            JAVA_INT.withName("ParentObjectTitleIndex"), JAVA_INT.withName("ParentObjectInstance"),
            JAVA_INT.withName("UniqueID"), JAVA_INT.withName("NameOffset"), JAVA_INT.withName("NameLength"));

    long PERF_INSTANCE_DEF_ByteLength = PERF_INSTANCE_DEFINITION
            .byteOffset(MemoryLayout.PathElement.groupElement("ByteLength"));
    long PERF_INSTANCE_DEF_NameOffset = PERF_INSTANCE_DEFINITION
            .byteOffset(MemoryLayout.PathElement.groupElement("NameOffset"));

    // PERF_COUNTER_BLOCK: header for the raw counter data block following each instance
    // https://learn.microsoft.com/en-us/windows/win32/api/winperf/ns-winperf-perf_counter_block
    //
    // Fields:
    // DWORD ByteLength (4 bytes)
    // Followed by the actual counter data bytes at offsets defined by PERF_COUNTER_DEFINITION.CounterOffset
    StructLayout PERF_COUNTER_BLOCK = structLayout(JAVA_INT.withName("ByteLength"));

    long PERF_COUNTER_BLOCK_ByteLength = 0L;
}
