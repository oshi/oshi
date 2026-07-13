/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Exercises the pre-Vista {@code MIB_IFROW} fallback path against the real system by forcing the
 * {@code isVistaOrGreater()} version gate. CI runners are all Vista+, so that branch is otherwise dead; running the
 * genuine {@code GetIfEntry} query with only the version flag overridden catches a rotted legacy path (wrong struct
 * offsets, failed query) as a divergence rather than a coverage number.
 * <p>
 * Only {@code ifType} is compared: the two APIs report the same IANA interface type, whereas the byte/packet counters
 * legitimately differ (pre-Vista {@code MIB_IFROW} uses unsigned 32-bit counters that can wrap) and physical-medium
 * type / alias / oper-status are simply unavailable in the legacy struct.
 */
@EnabledOnOs(OS.WINDOWS)
class WindowsNetworkIfLegacyTest {

    /** Forces the pre-Vista MIB_IFROW path; the constant override applies during super-construction. */
    private static final class LegacyNetworkIf extends WindowsNetworkIfJNA {
        LegacyNetworkIf(NetworkInterface netint) throws InstantiationException {
            super(netint);
        }

        @Override
        protected boolean isVistaOrGreater() {
            return false;
        }
    }

    @Test
    void legacyMibIfRowPathMatchesModernType() throws Exception {
        List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        int exercised = 0;
        for (NetworkInterface ni : interfaces) {
            WindowsNetworkIfJNA modern;
            LegacyNetworkIf legacy;
            try {
                modern = new WindowsNetworkIfJNA(ni);
                legacy = new LegacyNetworkIf(ni);
            } catch (InstantiationException e) {
                continue;
            }
            // updateAttributes() reports whether the pre-Vista GetIfEntry query succeeded on this real interface
            if (legacy.updateAttributes()) {
                exercised++;
                assertThat("ifType for " + ni.getName(), legacy.getIfType(), is(modern.getIfType()));
            }
        }
        assertThat("pre-Vista MIB_IFROW query succeeded for at least one interface", exercised, greaterThan(0));
    }
}
