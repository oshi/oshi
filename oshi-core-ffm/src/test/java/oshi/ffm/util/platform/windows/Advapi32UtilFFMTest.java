/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.lang.foreign.MemorySegment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.ffm.platform.windows.WinRegFFM;

/**
 * Tests {@link Advapi32UtilFFM} registry reads against well-known keys under
 * {@code HKLM\SOFTWARE\Microsoft\Windows NT\CurrentVersion}, which holds values of each supported type. Assertions are
 * guarded on {@link Advapi32UtilFFM#registryValueExists} so the test tolerates Windows editions that lack a given
 * value.
 */
@EnabledOnOs(OS.WINDOWS)
class Advapi32UtilFFMTest {

    private static final MemorySegment HKLM = MemorySegment.ofAddress(WinRegFFM.HKEY_LOCAL_MACHINE);

    private static final String CURVER = "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion";

    @Test
    void testRegistryGetString() {
        // ProductName is a REG_SZ present on every Windows edition.
        assertThat(Advapi32UtilFFM.registryGetValue(HKLM, CURVER, "ProductName"), is(instanceOf(String.class)));
    }

    @Test
    void testRegistryGetDword() {
        // CurrentMajorVersionNumber is a REG_DWORD on Windows 10/11 and Server 2016+.
        if (Advapi32UtilFFM.registryValueExists(HKLM, CURVER, "CurrentMajorVersionNumber")) {
            assertThat(Advapi32UtilFFM.registryGetValue(HKLM, CURVER, "CurrentMajorVersionNumber"),
                    is(instanceOf(Integer.class)));
        }
    }

    @Test
    void testRegistryGetQword() {
        // InstallTime is a REG_QWORD (FILETIME) on Windows 10/11 and Server 2016+.
        if (Advapi32UtilFFM.registryValueExists(HKLM, CURVER, "InstallTime")) {
            assertThat(Advapi32UtilFFM.registryGetValue(HKLM, CURVER, "InstallTime"), is(instanceOf(Long.class)));
        }
    }

    @Test
    void testRegistryGetBinary() {
        // DigitalProductId is a REG_BINARY present on every Windows edition.
        if (Advapi32UtilFFM.registryValueExists(HKLM, CURVER, "DigitalProductId")) {
            Object value = Advapi32UtilFFM.registryGetValue(HKLM, CURVER, "DigitalProductId");
            assertThat(value, is(notNullValue()));
            assertThat(value, is(instanceOf(byte[].class)));
        }
    }
}
