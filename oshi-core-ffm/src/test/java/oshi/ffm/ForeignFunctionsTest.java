/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static oshi.util.LogLevel.DEBUG;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link ForeignFunctions} helper methods.
 */
@EnabledForJreRange(min = JRE.JAVA_25)
@EnabledOnOs({ OS.LINUX, OS.MAC, OS.WINDOWS, OS.FREEBSD, OS.OPENBSD, OS.SOLARIS, OS.AIX })
class ForeignFunctionsTest {

    private static final Logger LOG = LoggerFactory.getLogger(ForeignFunctionsTest.class);

    private static final StructLayout LAYOUT = MemoryLayout.structLayout(JAVA_INT.withName("x"));

    @Test
    void testCallInArenaOrDefaultReturnsValue() {
        String result = ForeignFunctions.callInArenaOrDefault(arena -> {
            MemorySegment value = arena.allocate(JAVA_INT);
            value.set(JAVA_INT, 0, 7);
            return "value-" + value.get(JAVA_INT, 0);
        }, LOG, DEBUG, "object success", "default");

        assertThat(result, is("value-7"));
    }

    @Test
    void testCallInArenaOrDefaultReturnsDefaultOnThrowable() {
        String result = ForeignFunctions.callInArenaOrDefault(arena -> {
            throw new Throwable("object failure");
        }, LOG, DEBUG, "object failure", "default");

        assertThat(result, is("default"));
    }

    @Test
    void testCallInArenaIntOrDefaultReturnsValue() {
        int result = ForeignFunctions.callInArenaIntOrDefault(arena -> {
            MemorySegment value = arena.allocate(JAVA_INT);
            value.set(JAVA_INT, 0, 42);
            return value.get(JAVA_INT, 0);
        }, LOG, DEBUG, "int success", -1);

        assertThat(result, is(42));
    }

    @Test
    void testCallInArenaIntOrDefaultReturnsDefaultOnThrowable() {
        int result = ForeignFunctions.callInArenaIntOrDefault(arena -> {
            throw new Throwable("int failure");
        }, LOG, DEBUG, "int failure", -1);

        assertThat(result, is(-1));
    }

    @Test
    void testCallInArenaLongOrDefaultReturnsValue() {
        long result = ForeignFunctions.callInArenaLongOrDefault(arena -> 42L, LOG, DEBUG, "long success", -1L);

        assertThat(result, is(42L));
    }

    @Test
    void testCallInArenaLongOrDefaultReturnsDefaultOnThrowable() {
        long result = ForeignFunctions.callInArenaLongOrDefault(arena -> {
            throw new Throwable("long failure");
        }, LOG, DEBUG, "long failure", -1L);

        assertThat(result, is(-1L));
    }

    @Test
    void testCallInArenaDoubleOrDefaultReturnsValue() {
        double result = ForeignFunctions.callInArenaDoubleOrDefault(arena -> 42.5, LOG, DEBUG, "double success", -1d);

        assertThat(result, is(42.5));
    }

    @Test
    void testCallInArenaDoubleOrDefaultReturnsDefaultOnThrowable() {
        double result = ForeignFunctions.callInArenaDoubleOrDefault(arena -> {
            throw new Throwable("double failure");
        }, LOG, DEBUG, "double failure", -1d);

        assertThat(result, is(-1d));
    }

    @Test
    void testCallInArenaBooleanOrDefaultReturnsValue() {
        boolean result = ForeignFunctions.callInArenaBooleanOrDefault(arena -> true, LOG, DEBUG, "boolean success",
                false);

        assertThat(result, is(true));
    }

    @Test
    void testCallInArenaBooleanOrDefaultReturnsDefaultOnThrowable() {
        boolean result = ForeignFunctions.callInArenaBooleanOrDefault(arena -> {
            throw new Throwable("boolean failure");
        }, LOG, DEBUG, "boolean failure", false);

        assertThat(result, is(false));
    }

    // -- parameterized-message overloads (default value second) --

    /**
     * The parameterized overloads take the default value second rather than merely swapping it with the message. With
     * the default fourth, this call - whose {@code T} is String, so message and default have the same type - would
     * match both overloads, and Java's fixed-arity-before-varargs resolution would silently bind it to the older one
     * with the two transposed. This test fails to compile, or returns the message, if that ordering ever regresses.
     */
    @Test
    void testExistingOverloadStillWinsWhenTypeParameterIsString() {
        String result = ForeignFunctions.callInArenaOrDefault(arena -> {
            throw new Throwable("failure");
        }, LOG, DEBUG, "this is the message", "this is the default");

        assertThat(result, is("this is the default"));
    }

    @Test
    void testCallInArenaOrDefaultWithArgumentsReturnsDefaultOnThrowable() {
        String result = ForeignFunctions.callInArenaOrDefault(arena -> {
            throw new Throwable("failure");
        }, "the default", LOG, DEBUG, "failed for {}", "someKey");

        assertThat(result, is("the default"));
    }

    @Test
    void testCallInArenaOrDefaultWithArgumentsReturnsValue() {
        String result = ForeignFunctions.callInArenaOrDefault(arena -> "computed", "the default", LOG, DEBUG,
                "failed for {}", "someKey");

        assertThat(result, is("computed"));
    }

    @Test
    void testCallInArenaPrimitiveVariantsWithArguments() {
        assertThat(ForeignFunctions.callInArenaIntOrDefault(arena -> {
            throw new Throwable("failure");
        }, -1, LOG, DEBUG, "int failed for {}", "k"), is(-1));

        assertThat(ForeignFunctions.callInArenaLongOrDefault(arena -> {
            throw new Throwable("failure");
        }, -1L, LOG, DEBUG, "long failed for {}", "k"), is(-1L));

        assertThat(ForeignFunctions.callInArenaDoubleOrDefault(arena -> {
            throw new Throwable("failure");
        }, -1d, LOG, DEBUG, "double failed for {}", "k"), is(-1d));

        assertThat(ForeignFunctions.callInArenaBooleanOrDefault(arena -> {
            throw new Throwable("failure");
        }, true, LOG, DEBUG, "boolean failed for {}", "k"), is(true));
    }

    // -- raw pointer readers --

    @Test
    void testPointerReadersReturnNullForANullPointer() {
        try (Arena arena = Arena.ofConfined()) {
            assertThat(ForeignFunctions.getByteArrayFromNativePointer(null, 4L, arena), is(nullValue()));
            assertThat(ForeignFunctions.getStringFromNativePointer(null, arena), is(nullValue()));
            assertThat(ForeignFunctions.getStructFromNativePointer(null, LAYOUT, arena), is(nullValue()));
        }
    }

    @Test
    void testPointerReadersReturnNullForTheNullSegment() {
        try (Arena arena = Arena.ofConfined()) {
            assertThat(ForeignFunctions.getByteArrayFromNativePointer(MemorySegment.NULL, 4L, arena), is(nullValue()));
            assertThat(ForeignFunctions.getStringFromNativePointer(MemorySegment.NULL, arena), is(nullValue()));
            assertThat(ForeignFunctions.getStructFromNativePointer(MemorySegment.NULL, LAYOUT, arena), is(nullValue()));
        }
    }

    /**
     * The null guards above must not have cost the reader its actual job: a real pointer still yields its bytes.
     */
    @Test
    void testGetByteArrayFromNativePointerReadsTheBytes() {
        try (Arena arena = Arena.ofConfined()) {
            byte[] expected = { 1, 2, 3, 4 };
            MemorySegment segment = arena.allocate(expected.length);
            MemorySegment.copy(expected, 0, segment, JAVA_BYTE, 0, expected.length);

            assertThat(ForeignFunctions.getByteArrayFromNativePointer(segment, expected.length, arena),
                    is(equalTo(expected)));
        }
    }

    /**
     * Both sizes come from the linker's canonical layouts, which every platform this runs on defines.
     */
    @Test
    void testNativeTypeSizesAreResolved() {
        assertThat(ForeignFunctions.NATIVE_LONG_SIZE, is(greaterThan(0L)));
        assertThat(ForeignFunctions.NATIVE_SIZE_T_SIZE, is(greaterThan(0L)));
    }
}
