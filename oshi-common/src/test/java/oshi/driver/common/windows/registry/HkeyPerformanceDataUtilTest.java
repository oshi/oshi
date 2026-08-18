/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.driver.common.windows.perfmon.PdhCounterWildcardProperty;
import oshi.util.tuples.Triplet;

/**
 * Exercises the HKEY_PERFORMANCE_DATA walk against a synthetic block laid out by hand.
 * <p>
 * The block is written at the literal field offsets in {@code winperf.h}, chosen independently of the constants the
 * walk uses, so a wrong constant reads the wrong place and fails here rather than silently returning bad counters on
 * Windows. The walk is offset arithmetic over a buffer and makes no native call, so this runs on every platform - the
 * only way this code gets tested at all.
 */
class HkeyPerformanceDataUtilTest {

    // winperf.h, 64-bit
    private static final int DATA_HEADER_LENGTH = 24;
    private static final int DATA_NUM_OBJECT_TYPES = 28;
    private static final int DATA_PERF_TIME_100N_SEC = 72;
    private static final int OBJ_TOTAL_BYTE_LENGTH = 0;
    private static final int OBJ_DEFINITION_LENGTH = 4;
    private static final int OBJ_HEADER_LENGTH = 8;
    private static final int OBJ_NAME_TITLE_INDEX = 12;
    private static final int OBJ_NUM_COUNTERS = 32;
    private static final int OBJ_NUM_INSTANCES = 40;
    private static final int DEF_BYTE_LENGTH = 0;
    private static final int DEF_NAME_TITLE_INDEX = 4;
    private static final int DEF_COUNTER_SIZE = 32;
    private static final int DEF_COUNTER_OFFSET = 36;
    private static final int INST_BYTE_LENGTH = 0;
    private static final int INST_NAME_OFFSET = 16;
    private static final int BLOCK_BYTE_LENGTH = 0;

    private static final int PERF_DATA_BLOCK_SIZE = 88;
    private static final int OBJECT_HEADER_SIZE = 64;
    private static final int COUNTER_DEF_SIZE = 40;
    private static final int INSTANCE_DEF_SIZE = 40;
    private static final int COUNTER_BLOCK_SIZE = 32;

    private static final int WANTED_OBJECT = 230;
    private static final int DECOY_OBJECT = 238;
    private static final int PROC_COUNTER = 780;
    private static final int TIME_COUNTER = 784;
    private static final long PERF_TIME = 133_000_000_000_000_000L;

    /** Counters to extract; the first constant names the instance rather than a counter. */
    enum TestProperty implements PdhCounterWildcardProperty {
        INSTANCE("Name"), PROC_ID("ID Process"), ELAPSED("Elapsed Time");

        private final String counter;

        TestProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return this.counter;
        }
    }

    /** A {@link PerfDataBuffer} over a plain byte array, standing in for JNA's Memory or an FFM MemorySegment. */
    private static final class ArrayBuffer implements PerfDataBuffer {
        private final ByteBuffer bb;

        private ArrayBuffer(byte[] bytes) {
            this.bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        }

        @Override
        public int getInt(long offset) {
            return this.bb.getInt((int) offset);
        }

        @Override
        public long getLong(long offset) {
            return this.bb.getLong((int) offset);
        }

        @Override
        public String getWideString(long offset) {
            int end = (int) offset;
            while (this.bb.getShort(end) != 0) {
                end += 2;
            }
            byte[] chars = new byte[end - (int) offset];
            // ByteBuffer.position(int) returns Buffer before Java 9, so these cannot be chained here
            this.bb.position((int) offset);
            this.bb.get(chars);
            return new String(chars, StandardCharsets.UTF_16LE);
        }
    }

    /**
     * Builds a block holding a decoy object followed by the wanted one, so the walk has to skip by TotalByteLength to
     * reach it, and two instances, so it has to step by the counter block length to reach the second.
     */
    private static byte[] buildBlock() {
        int decoyAt = PERF_DATA_BLOCK_SIZE;
        int decoyLen = OBJECT_HEADER_SIZE;
        int objAt = decoyAt + decoyLen;
        int defsAt = objAt + OBJECT_HEADER_SIZE;
        int definitionLength = OBJECT_HEADER_SIZE + 2 * COUNTER_DEF_SIZE;
        int instancesAt = objAt + definitionLength;
        int inst1Block = instancesAt + INSTANCE_DEF_SIZE;
        int inst2At = inst1Block + COUNTER_BLOCK_SIZE;
        int inst2Block = inst2At + INSTANCE_DEF_SIZE;
        byte[] bytes = new byte[inst2Block + COUNTER_BLOCK_SIZE];
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        bb.putInt(DATA_HEADER_LENGTH, PERF_DATA_BLOCK_SIZE);
        bb.putInt(DATA_NUM_OBJECT_TYPES, 2);
        bb.putLong(DATA_PERF_TIME_100N_SEC, PERF_TIME);

        // Decoy object: only its title index and total length are ever read
        bb.putInt(decoyAt + OBJ_NAME_TITLE_INDEX, DECOY_OBJECT);
        bb.putInt(decoyAt + OBJ_TOTAL_BYTE_LENGTH, decoyLen);

        bb.putInt(objAt + OBJ_NAME_TITLE_INDEX, WANTED_OBJECT);
        bb.putInt(objAt + OBJ_TOTAL_BYTE_LENGTH, bytes.length - objAt);
        bb.putInt(objAt + OBJ_HEADER_LENGTH, OBJECT_HEADER_SIZE);
        bb.putInt(objAt + OBJ_DEFINITION_LENGTH, definitionLength);
        bb.putInt(objAt + OBJ_NUM_COUNTERS, 2);
        bb.putInt(objAt + OBJ_NUM_INSTANCES, 2);

        // A 4-byte counter at block offset 8, and an 8-byte counter at block offset 16
        bb.putInt(defsAt + DEF_BYTE_LENGTH, COUNTER_DEF_SIZE);
        bb.putInt(defsAt + DEF_NAME_TITLE_INDEX, PROC_COUNTER);
        bb.putInt(defsAt + DEF_COUNTER_SIZE, 4);
        bb.putInt(defsAt + DEF_COUNTER_OFFSET, 8);
        int def2 = defsAt + COUNTER_DEF_SIZE;
        bb.putInt(def2 + DEF_BYTE_LENGTH, COUNTER_DEF_SIZE);
        bb.putInt(def2 + DEF_NAME_TITLE_INDEX, TIME_COUNTER);
        bb.putInt(def2 + DEF_COUNTER_SIZE, 8);
        bb.putInt(def2 + DEF_COUNTER_OFFSET, 16);

        putInstance(bb, instancesAt, "firefox", inst1Block, 1234, 5_000_000_000L);
        putInstance(bb, inst2At, "chrome", inst2Block, 5678, 9_000_000_000L);
        return bytes;
    }

    private static void putInstance(ByteBuffer bb, int at, String name, int blockAt, int pid, long elapsed) {
        bb.putInt(at + INST_BYTE_LENGTH, INSTANCE_DEF_SIZE);
        bb.putInt(at + INST_NAME_OFFSET, 24);
        bb.position(at + 24);
        bb.put(name.getBytes(StandardCharsets.UTF_16LE));
        bb.putShort((short) 0);
        bb.putInt(blockAt + BLOCK_BYTE_LENGTH, COUNTER_BLOCK_SIZE);
        bb.putInt(blockAt + 8, pid);
        bb.putLong(blockAt + 16, elapsed);
    }

    private static EnumMap<TestProperty, Integer> indexMap() {
        EnumMap<TestProperty, Integer> map = new EnumMap<>(TestProperty.class);
        map.put(TestProperty.PROC_ID, PROC_COUNTER);
        map.put(TestProperty.ELAPSED, TIME_COUNTER);
        return map;
    }

    @Test
    void testParsePerfData() {
        Triplet<List<Map<TestProperty, Object>>, Long, Long> result = HkeyPerformanceDataUtil
                .parsePerfData(new ArrayBuffer(buildBlock()), WANTED_OBJECT, indexMap(), TestProperty.class);
        assertNotNull(result);
        assertEquals(PERF_TIME, result.getB());

        List<Map<TestProperty, Object>> instances = result.getA();
        assertEquals(2, instances.size());

        assertEquals("firefox", instances.get(0).get(TestProperty.INSTANCE));
        assertEquals(1234, instances.get(0).get(TestProperty.PROC_ID));
        assertEquals(5_000_000_000L, instances.get(0).get(TestProperty.ELAPSED));

        // Reaching the second instance means stepping by the counter block length, not just the definition length
        assertEquals("chrome", instances.get(1).get(TestProperty.INSTANCE));
        assertEquals(5678, instances.get(1).get(TestProperty.PROC_ID));
        assertEquals(9_000_000_000L, instances.get(1).get(TestProperty.ELAPSED));
    }

    @Test
    void testObjectNotPresentReturnsNull() {
        assertNull(HkeyPerformanceDataUtil.parsePerfData(new ArrayBuffer(buildBlock()), 999, indexMap(),
                TestProperty.class));
    }

    @Test
    void testUnknownCounterReturnsNull() {
        EnumMap<TestProperty, Integer> map = indexMap();
        map.put(TestProperty.ELAPSED, 12345);
        assertNull(HkeyPerformanceDataUtil.parsePerfData(new ArrayBuffer(buildBlock()), WANTED_OBJECT, map,
                TestProperty.class));
    }
}
