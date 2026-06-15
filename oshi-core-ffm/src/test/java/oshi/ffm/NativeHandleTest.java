/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class NativeHandleTest {

    @Test
    void testCloseInvokesCloser() {
        AtomicBoolean closed = new AtomicBoolean(false);
        MemorySegment fakeHandle = MemorySegment.ofAddress(0x1234);
        try (NativeHandle h = NativeHandle.of(fakeHandle, seg -> closed.set(true))) {
            assertThat(h.get(), is(fakeHandle));
            assertThat(h.isNull(), is(false));
        }
        assertThat(closed.get(), is(true));
    }

    @Test
    void testNullHandleSkipsCloser() {
        AtomicBoolean closed = new AtomicBoolean(false);
        try (NativeHandle h = NativeHandle.of(null, seg -> closed.set(true))) {
            assertThat(h.isNull(), is(true));
        }
        assertThat(closed.get(), is(false));
    }

    @Test
    void testZeroAddressSkipsCloser() {
        AtomicBoolean closed = new AtomicBoolean(false);
        try (NativeHandle h = NativeHandle.of(MemorySegment.NULL, seg -> closed.set(true))) {
            assertThat(h.isNull(), is(true));
        }
        assertThat(closed.get(), is(false));
    }

    @Test
    void testCloserExceptionIsSuppressed() {
        AtomicReference<MemorySegment> closedWith = new AtomicReference<>();
        MemorySegment fakeHandle = MemorySegment.ofAddress(0x5678);
        try (NativeHandle h = NativeHandle.of(fakeHandle, seg -> {
            closedWith.set(seg);
            throw new RuntimeException("simulated close failure");
        })) {
            assertThat(h.isNull(), is(false));
        }
        // Should not throw, and closer was still called
        assertThat(closedWith.get(), is(fakeHandle));
    }

    @Test
    void testCloseIsIdempotent() {
        AtomicInteger closeCount = new AtomicInteger(0);
        MemorySegment fakeHandle = MemorySegment.ofAddress(0xABCD);
        NativeHandle h = NativeHandle.of(fakeHandle, seg -> closeCount.incrementAndGet());
        h.close();
        h.close();
        h.close();
        assertThat(closeCount.get(), is(1));
    }

    @Test
    void testNullCloserThrows() {
        MemorySegment segment = MemorySegment.ofAddress(0x1);
        assertThrows(NullPointerException.class, () -> NativeHandle.of(segment, null));
    }
}
