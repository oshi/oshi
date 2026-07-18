/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import com.sun.jna.Memory;

/**
 * Tests the pointer decoding shared by the Solaris and AIX JNA {@code PsInfo} address-space readers.
 * <p>
 * Allocating a JNA {@link Memory} requires the JNA native dispatch library, so this test is disabled on the NetBSD CI
 * runner that intentionally runs without it.
 */
@DisabledIfSystemProperty(named = "os.name", matches = "(?i)netbsd")
class ProcAddressSpaceReaderTest {

    @Test
    void testDecodePointer64Bit() {
        try (Memory buffer = new Memory(8)) {
            buffer.setLong(0, 0x0000_1234_5678_9abcL);
            assertThat(ProcAddressSpaceReader.decodePointer(buffer, 0, 8), is(0x0000_1234_5678_9abcL));
        }
    }

    @Test
    void testDecodePointer32BitLowAddress() {
        try (Memory buffer = new Memory(4)) {
            buffer.setInt(0, 0x0040_1000);
            assertThat(ProcAddressSpaceReader.decodePointer(buffer, 0, 4), is(0x0040_1000L));
        }
    }

    @Test
    void testDecodePointer32BitHighAddressIsUnsigned() {
        // A 32-bit pointer with the high bit set must be read unsigned, not sign-extended to a negative long.
        try (Memory buffer = new Memory(4)) {
            buffer.setInt(0, 0x8000_0000);
            assertThat(ProcAddressSpaceReader.decodePointer(buffer, 0, 4), is(0x0000_0000_8000_0000L));
        }
    }

    @Test
    void testDecodePointer32BitMaxAddressIsUnsigned() {
        try (Memory buffer = new Memory(4)) {
            buffer.setInt(0, 0xFFFF_FFFF);
            assertThat(ProcAddressSpaceReader.decodePointer(buffer, 0, 4), is(0x0000_0000_FFFF_FFFFL));
        }
    }
}
