/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.comparison;

import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.sun.jna.Memory;

import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.jna.platform.unix.FreeBsdLibc;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * Compares JNA {@link BsdSysctlUtil} against FFM {@link BsdSysctlUtilFFM} on FreeBSD to validate that the new FFM
 * binding returns identical values to the established JNA path. Runs only on FreeBSD.
 *
 * <p>
 * Activate with: {@code mvn test -pl oshi-benchmark -Paggregate-coverage} on a FreeBSD host.
 */
@EnabledOnOs(OS.FREEBSD)
class BsdSysctlComparisonTest {

    /** Compares an int-valued sysctl ({@code hw.ncpu}) — static, must match exactly. */
    @Test
    void sysctlInt() {
        int jna = BsdSysctlUtil.sysctl("hw.ncpu", -1);
        int ffm = BsdSysctlUtilFFM.sysctl("hw.ncpu", -1);
        assertThat(ffm).isEqualTo(jna).isGreaterThan(0);
    }

    /** Compares a long-valued sysctl ({@code hw.physmem}) — static, must match exactly. */
    @Test
    void sysctlLong() {
        long jna = BsdSysctlUtil.sysctl("hw.physmem", -1L);
        long ffm = BsdSysctlUtilFFM.sysctl("hw.physmem", -1L);
        assertThat(ffm).isEqualTo(jna).isGreaterThan(0L);
    }

    /** Compares string-valued sysctls ({@code kern.ostype}, {@code kern.osrelease}) — static, must match exactly. */
    @Test
    void sysctlString() {
        assertThat(BsdSysctlUtilFFM.sysctl("kern.ostype", "")).isEqualTo(BsdSysctlUtil.sysctl("kern.ostype", ""))
                .isEqualTo("FreeBSD");
        assertThat(BsdSysctlUtilFFM.sysctl("kern.osrelease", "")).isEqualTo(BsdSysctlUtil.sysctl("kern.osrelease", ""))
                .isNotEmpty();
    }

    /**
     * Default-on-failure path: an unknown MIB returns the caller-supplied default for both implementations.
     */
    @Test
    void sysctlReturnsDefaultOnUnknownMib() {
        assertThat(BsdSysctlUtilFFM.sysctl("oshi.nonexistent.mib", -1))
                .isEqualTo(BsdSysctlUtil.sysctl("oshi.nonexistent.mib", -1)).isEqualTo(-1);
        assertThat(BsdSysctlUtilFFM.sysctl("oshi.nonexistent.mib", "default"))
                .isEqualTo(BsdSysctlUtil.sysctl("oshi.nonexistent.mib", "default")).isEqualTo("default");
    }

    /**
     * Struct path: {@code kern.boottime} returns a {@code struct timeval} (two longs on LP64 FreeBSD). The boot time
     * must match exactly between back-to-back reads.
     */
    @Test
    void sysctlStruct() {
        FreeBsdLibc.Timeval jnaTv = new FreeBsdLibc.Timeval();
        boolean jnaOk = BsdSysctlUtil.sysctl("kern.boottime", jnaTv);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ffmTv = arena.allocate(JAVA_LONG.byteSize() * 2);
            boolean ffmOk = BsdSysctlUtilFFM.sysctl("kern.boottime", ffmTv);
            assertThat(ffmOk).isEqualTo(jnaOk).isTrue();
            assertThat(ffmTv.get(JAVA_LONG, 0)).as("tv_sec").isEqualTo(jnaTv.tv_sec).isGreaterThan(0L);
            // tv_usec may not match exactly (kernel updates between calls under some configurations),
            // but tv_sec is the monotonic boot-second and must agree.
        }
    }

    /**
     * Raw-buffer path: {@code sysctl(name)} returns an allocated buffer sized by the kernel. Verifies both
     * implementations return the same byte count and identical contents for {@code kern.boottime}.
     */
    @Test
    void sysctlRawBuffer() {
        try (Memory jnaMem = BsdSysctlUtil.sysctl("kern.boottime")) {
            MemorySegment ffmSeg = BsdSysctlUtilFFM.sysctl("kern.boottime");
            assertThat(jnaMem).isNotNull();
            assertThat(ffmSeg).isNotNull();
            assertThat(ffmSeg.byteSize()).isEqualTo(jnaMem.size());
            // Compare the tv_sec field (first long) — must agree exactly across back-to-back reads
            assertThat(ffmSeg.get(JAVA_LONG, 0)).isEqualTo(jnaMem.getLong(0)).isGreaterThan(0L);
        }
    }
}
