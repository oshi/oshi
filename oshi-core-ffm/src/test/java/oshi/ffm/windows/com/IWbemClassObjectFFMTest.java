/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows.com;

import static java.lang.foreign.MemorySegment.NULL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;

/**
 * Tests for {@link IWbemClassObjectFFM} get methods using real WMI queries.
 */
@EnabledOnOs(OS.WINDOWS)
@EnabledForJreRange(min = JRE.JAVA_25)
class IWbemClassObjectFFMTest {

    // -------------------------------------------------------------------------
    // Null pointer — all get methods return defaults
    // -------------------------------------------------------------------------

    @Test
    void testGetWithNullPointerReturnsFailed() {
        try (var arena = Arena.ofConfined()) {
            var result = IWbemClassObjectFFM.get(NULL, "Name", arena);
            assertThat(result.succeeded(), is(false));
            assertThat(result.cimType(), is(WbemcliFFM.CIM_ILLEGAL));
        }
    }

    @Test
    void testGetStringWithNullPointerReturnsEmpty() {
        try (var arena = Arena.ofConfined()) {
            assertThat(IWbemClassObjectFFM.getString(NULL, "Name", arena), is(""));
        }
    }

    @Test
    void testGetIntWithNullPointerReturnsZero() {
        try (var arena = Arena.ofConfined()) {
            assertThat(IWbemClassObjectFFM.getInt(NULL, "Size", arena), is(0));
        }
    }

    @Test
    void testGetLongWithNullPointerReturnsZero() {
        try (var arena = Arena.ofConfined()) {
            assertThat(IWbemClassObjectFFM.getLong(NULL, "Size", arena), is(0L));
        }
    }

    @Test
    void testGetBooleanWithNullPointerReturnsFalse() {
        try (var arena = Arena.ofConfined()) {
            assertThat(IWbemClassObjectFFM.getBoolean(NULL, "Primary", arena), is(false));
        }
    }

    // -------------------------------------------------------------------------
    // Nonexistent property — get methods return defaults
    // -------------------------------------------------------------------------

    @Test
    void testGetStringNonexistentProperty() {
        withFirstObject("SELECT Caption FROM Win32_OperatingSystem", (pObject, arena) -> {
            assertThat(IWbemClassObjectFFM.getString(pObject, "NoSuchProperty", arena), is(""));
        });
    }

    @Test
    void testGetIntNonexistentProperty() {
        withFirstObject("SELECT ProcessId FROM Win32_Process WHERE ProcessId = " + ProcessHandle.current().pid(),
                (pObject, arena) -> {
                    assertThat(IWbemClassObjectFFM.getInt(pObject, "NoSuchProperty", arena), is(0));
                });
    }

    // -------------------------------------------------------------------------
    // getString — Win32_OperatingSystem.Caption (CIM_STRING → VT_BSTR)
    // -------------------------------------------------------------------------

    @Test
    void testGetStringFromWmi() {
        withFirstObject("SELECT Caption FROM Win32_OperatingSystem", (pObject, arena) -> {
            var caption = IWbemClassObjectFFM.getString(pObject, "Caption", arena);
            assertThat(caption, is(not(emptyString())));
        });
    }

    // -------------------------------------------------------------------------
    // getInt — Win32_Process.ProcessId (CIM_UINT32 → VT_I4)
    // -------------------------------------------------------------------------

    @Test
    void testGetIntFromWmi() {
        long myPid = ProcessHandle.current().pid();
        withFirstObject("SELECT ProcessId FROM Win32_Process WHERE ProcessId = " + myPid, (pObject, arena) -> {
            int pid = IWbemClassObjectFFM.getInt(pObject, "ProcessId", arena);
            assertThat(pid, is((int) myPid));
        });
    }

    // -------------------------------------------------------------------------
    // getLong — Win32_Process.WorkingSetSize (CIM_UINT64 → VT_BSTR)
    // -------------------------------------------------------------------------

    @Test
    void testGetLongFromWmi() {
        long myPid = ProcessHandle.current().pid();
        withFirstObject("SELECT WorkingSetSize FROM Win32_Process WHERE ProcessId = " + myPid, (pObject, arena) -> {
            long wss = IWbemClassObjectFFM.getLong(pObject, "WorkingSetSize", arena);
            assertThat(wss, is(greaterThan(0L)));
        });
    }

    // -------------------------------------------------------------------------
    // getLong — Win32_Process.HandleCount (CIM_UINT32 → VT_I4, widened to long)
    // -------------------------------------------------------------------------

    @Test
    void testGetLongFromUint32() {
        long myPid = ProcessHandle.current().pid();
        withFirstObject("SELECT HandleCount FROM Win32_Process WHERE ProcessId = " + myPid, (pObject, arena) -> {
            long handles = IWbemClassObjectFFM.getLong(pObject, "HandleCount", arena);
            assertThat(handles, is(greaterThan(0L)));
        });
    }

    // -------------------------------------------------------------------------
    // getBoolean — Win32_OperatingSystem.Primary (CIM_BOOLEAN → VT_BOOL)
    // -------------------------------------------------------------------------

    @Test
    void testGetBooleanFromWmi() {
        withFirstObject("SELECT Primary FROM Win32_OperatingSystem", (pObject, arena) -> {
            boolean primary = IWbemClassObjectFFM.getBoolean(pObject, "Primary", arena);
            assertThat(primary, is(true));
        });
    }

    // -------------------------------------------------------------------------
    // get() raw — verify GetResult fields
    // -------------------------------------------------------------------------

    @Test
    void testGetRawResultFields() {
        assertRawGet("SELECT Caption FROM Win32_OperatingSystem", "Caption", WbemcliFFM.CIM_STRING, VariantFFM.VT_BSTR);
    }

    @Test
    void testGetRawResultForUint32() {
        assertRawGet("SELECT ProcessId FROM Win32_Process WHERE ProcessId = " + ProcessHandle.current().pid(),
                "ProcessId", WbemcliFFM.CIM_UINT32, VariantFFM.VT_I4);
    }

    @Test
    void testGetRawResultForBoolean() {
        assertRawGet("SELECT Primary FROM Win32_OperatingSystem", "Primary", WbemcliFFM.CIM_BOOLEAN,
                VariantFFM.VT_BOOL);
    }

    /**
     * Asserts that a raw get() call returns the expected CIM and VARIANT types.
     *
     * @param wql         the WQL query
     * @param property    the property name
     * @param expectedCim the expected CIM type
     * @param expectedVt  the expected VARIANT type
     */
    private static void assertRawGet(String wql, String property, int expectedCim, int expectedVt) {
        withFirstObject(wql, (pObject, arena) -> {
            var result = IWbemClassObjectFFM.get(pObject, property, arena);
            try {
                assertThat(result.succeeded(), is(true));
                assertThat(result.cimType(), is(expectedCim));
                assertThat(VariantFFM.getVt(result.variant()), is(expectedVt));
            } finally {
                VariantFFM.clear(result.variant());
            }
        });
    }

    // -------------------------------------------------------------------------
    // getInt — Win32_Processor.NumberOfCores (CIM_UINT32)
    // -------------------------------------------------------------------------

    @Test
    void testGetIntNumberOfCores() {
        withFirstObject("SELECT NumberOfCores FROM Win32_Processor", (pObject, arena) -> {
            int cores = IWbemClassObjectFFM.getInt(pObject, "NumberOfCores", arena);
            assertThat(cores, is(greaterThanOrEqualTo(1)));
        });
    }

    // -------------------------------------------------------------------------
    // Helper: execute WQL, get first object, run assertion, clean up
    // -------------------------------------------------------------------------

    @FunctionalInterface
    interface WmiObjectConsumer {
        void accept(MemorySegment pObject, Arena arena) throws Throwable;
    }

    private static void withFirstObject(String wql, WmiObjectConsumer consumer) {
        var hr = Ole32FFM.CoInitializeEx(Ole32FFM.COINIT_MULTITHREADED);
        boolean comInitialized = hr.isPresent() && Ole32FFM.succeeded(hr.getAsInt());
        try (var arena = Arena.ofConfined()) {
            Optional<MemorySegment> locator = Optional.empty();
            Optional<MemorySegment> services = Optional.empty();
            Optional<MemorySegment> enumerator = Optional.empty();
            MemorySegment object = NULL;
            try {
                locator = IWbemLocatorFFM.create(arena);
                assertThat("Failed to create IWbemLocator", locator.isPresent(), is(true));

                services = IWbemLocatorFFM.connectServer(locator.get(), WbemcliFFM.DEFAULT_NAMESPACE, arena);
                assertThat("Failed to connect to WMI", services.isPresent(), is(true));

                Ole32FFM.CoSetProxyBlanket(services.get(), -1, -1, Ole32FFM.RPC_C_AUTHN_LEVEL_CALL,
                        Ole32FFM.RPC_C_IMP_LEVEL_IMPERSONATE, Ole32FFM.EOAC_NONE);

                enumerator = IWbemServicesFFM.execQuery(services.get(), wql, arena);
                assertThat("WQL query failed: " + wql, enumerator.isPresent(), is(true));

                var next = IEnumWbemClassObjectFFM.next(enumerator.get(), arena);
                assertThat("No WMI object returned for: " + wql, next.hasObject(), is(true));
                object = next.pObject();

                consumer.accept(object, arena);
            } catch (Throwable t) {
                throw new AssertionError("WMI consumer failed", t);
            } finally {
                if (!NULL.equals(object)) {
                    IUnknownFFM.safeRelease(object, arena);
                }
                enumerator.ifPresent(e -> IUnknownFFM.safeRelease(e, arena));
                services.ifPresent(s -> IUnknownFFM.safeRelease(s, arena));
                locator.ifPresent(l -> IUnknownFFM.safeRelease(l, arena));
            }
        } finally {
            if (comInitialized) {
                Ole32FFM.CoUninitialize();
            }
        }
    }
}
