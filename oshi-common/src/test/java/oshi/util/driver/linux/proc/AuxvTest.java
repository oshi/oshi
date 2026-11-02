/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux.proc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

class AuxvTest {

    // The leading entries of a real s390x /proc/self/auxv: HWCAP, PAGESZ, CLKTCK, then the terminating AT_NULL
    private static final long[] VECTOR = { Auxv.AT_HWCAP, 0x5fffffL, Auxv.AT_PAGESZ, 4096L, Auxv.AT_CLKTCK, 100L,
            Auxv.AT_NULL, 0L };

    @Test
    @EnabledOnOs(OS.LINUX)
    void testQueryAuxv() {
        Map<Integer, Long> auxv = Auxv.queryAuxv();
        assertThat("Clock Ticks should be positive", auxv.getOrDefault(Auxv.AT_CLKTCK, 0L), greaterThan(0L));
        assertThat("Page Size should be positive", auxv.getOrDefault(Auxv.AT_PAGESZ, 0L), greaterThan(0L));
        assertThat(Auxv.userHz(auxv), is(auxv.get(Auxv.AT_CLKTCK)));
        assertThat(Auxv.pageSize(auxv), is(auxv.get(Auxv.AT_PAGESZ)));
    }

    @Test
    void testParseAuxv64BitBigEndian() {
        assertParsesVector(true, ByteOrder.BIG_ENDIAN);
    }

    @Test
    void testParseAuxv64BitLittleEndian() {
        assertParsesVector(true, ByteOrder.LITTLE_ENDIAN);
    }

    @Test
    void testParseAuxv32BitBigEndian() {
        assertParsesVector(false, ByteOrder.BIG_ENDIAN);
    }

    @Test
    void testParseAuxv32BitLittleEndian() {
        assertParsesVector(false, ByteOrder.LITTLE_ENDIAN);
    }

    private static void assertParsesVector(boolean is64Bit, ByteOrder order) {
        ByteBuffer buff = ByteBuffer.allocate(VECTOR.length * (is64Bit ? 8 : 4)).order(order);
        for (long v : VECTOR) {
            if (is64Bit) {
                buff.putLong(v);
            } else {
                buff.putInt((int) v);
            }
        }
        buff.flip();
        Map<Integer, Long> auxv = Auxv.parseAuxv(buff, is64Bit);
        assertThat(auxv, is(Map.of(Auxv.AT_HWCAP, 0x5fffffL, Auxv.AT_PAGESZ, 4096L, Auxv.AT_CLKTCK, 100L)));
        assertThat(Auxv.userHz(auxv), is(100L));
        assertThat(Auxv.pageSize(auxv), is(4096L));
    }

    @Test
    void testParseAuxvEmpty() {
        // An unreadable /proc/self/auxv reads as no bytes, and the parse stops at once
        assertThat(Auxv.parseAuxv(ByteBuffer.allocate(0), true), is(anEmptyMap()));
    }

    @Test
    void testFallbacksArePositive() {
        // A missing or nonpositive entry falls back to getconf, and to a default if that gives nothing usable
        Map<Integer, Long> auxv = Map.of(Auxv.AT_CLKTCK, 0L, Auxv.AT_PAGESZ, -1L);
        assertThat(Auxv.userHz(auxv), greaterThan(0L));
        assertThat(Auxv.pageSize(auxv), greaterThan(0L));
        assertThat(Auxv.userHz(Map.of()), greaterThan(0L));
        assertThat(Auxv.pageSize(Map.of()), greaterThan(0L));
    }
}
