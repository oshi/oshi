/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

class PlatformEnumTest {

    @Test
    void testGetName() {
        assertThat(PlatformEnum.MACOS.getName(), is("macOS"));
        assertThat(PlatformEnum.LINUX.getName(), is("Linux"));
        assertThat(PlatformEnum.WINDOWS.getName(), is("Windows"));
        assertThat(PlatformEnum.SOLARIS.getName(), is("Solaris"));
        assertThat(PlatformEnum.FREEBSD.getName(), is("FreeBSD"));
        assertThat(PlatformEnum.OPENBSD.getName(), is("OpenBSD"));
        assertThat(PlatformEnum.WINDOWSCE.getName(), is("Windows CE"));
        assertThat(PlatformEnum.AIX.getName(), is("AIX"));
        assertThat(PlatformEnum.ANDROID.getName(), is("Android"));
        assertThat(PlatformEnum.GNU.getName(), is("GNU"));
        assertThat(PlatformEnum.KFREEBSD.getName(), is("kFreeBSD"));
        assertThat(PlatformEnum.NETBSD.getName(), is("NetBSD"));
        assertThat(PlatformEnum.DRAGONFLYBSD.getName(), is("DragonFly BSD"));
        assertThat(PlatformEnum.UNKNOWN.getName(), is("Unknown"));
    }

    @Test
    void testGetValueValidOrdinals() {
        assertThat(PlatformEnum.getValue(0), is(PlatformEnum.MACOS));
        assertThat(PlatformEnum.getValue(1), is(PlatformEnum.LINUX));
        assertThat(PlatformEnum.getValue(2), is(PlatformEnum.WINDOWS));
    }

    @Test
    void testGetValueOutOfRange() {
        assertThat(PlatformEnum.getValue(-1), is(PlatformEnum.UNKNOWN));
        assertThat(PlatformEnum.getValue(999), is(PlatformEnum.UNKNOWN));
        // UNKNOWN's own ordinal should also return UNKNOWN
        assertThat(PlatformEnum.getValue(PlatformEnum.UNKNOWN.ordinal()), is(PlatformEnum.UNKNOWN));
    }

    @Test
    void testGetNameByOrdinal() {
        assertThat(PlatformEnum.getName(0), is("macOS"));
        assertThat(PlatformEnum.getName(1), is("Linux"));
        assertThat(PlatformEnum.getName(-1), is("Unknown"));
    }

    @Test
    void testGetCurrentPlatform() {
        assertThat(PlatformEnum.getCurrentPlatform(), is(notNullValue()));
    }
}
