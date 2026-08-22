/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.comparison;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.foreign.MemoryLayout;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.sun.jna.Structure;

import oshi.ffm.platform.mac.MacSystem;
import oshi.jna.platform.mac.SystemB;

/**
 * Asserts that the FFM struct layouts and the JNA {@link Structure} classes describing the same native struct agree on
 * total size and on where each shared field sits.
 *
 * <p>
 * The two bindings are written independently from the same headers, so a disagreement means one has drifted. This is a
 * different check from {@code .github/ffm-audit}, which compares one binding against the real SDK headers and needs a C
 * toolchain; this one catches drift between the pair and needs only a JVM.
 *
 * <p>
 * Neither subsumes the other. Both bindings once agreed that {@code utmpx.ut_pad} was 16 bytes where the header says
 * 64, which only the header audit could see; and the FreeBSD FFM binding orders {@code addrinfo} the BSD way while the
 * shared JNA one uses the glibc order, which only a parity check can see.
 */
class StructLayoutComparisonTest {

    /** JNA exposes field offsets only to subclasses, so each structure supplies its own accessor. */
    private static void assertSameLayout(String what, MemoryLayout ffm, Structure jna, ToIntFunction<String> jnaOffset,
            String... fields) {
        jna.size(); // JNA lays the structure out lazily
        assertThat(ffm.byteSize()).as("%s: total size, JNA vs FFM", what).isEqualTo((long) jna.size());
        for (String field : fields) {
            assertThat(ffm.byteOffset(groupElement(field))).as("%s.%s: offset, JNA vs FFM", what, field)
                    .isEqualTo((long) jnaOffset.applyAsInt(field));
        }
    }

    /** Exposes JNA's protected offset accessor for the one structure compared below. */
    private static final class ProbeUtmpx extends SystemB.MacUtmpx {
        int offsetOf(String field) {
            return fieldOffset(field);
        }
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void testUtmpxAgrees() {
        // ut_tv is a nested struct timeval in the JNA binding but two fields plus padding in the FFM one, so it is
        // pinned by the fields either side of it rather than by name
        ProbeUtmpx jna = new ProbeUtmpx();
        assertSameLayout("utmpx", MacSystem.UTMPX, jna, jna::offsetOf, "ut_user", "ut_id", "ut_line", "ut_pid",
                "ut_type", "ut_host", "ut_pad");
    }
}
