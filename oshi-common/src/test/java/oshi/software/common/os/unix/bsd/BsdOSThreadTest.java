/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.bsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests the monotonic CPU-time clamping in the shared {@code ps} thread-row parser of {@link BsdOSThread}.
 */
class BsdOSThreadTest {

    private static BsdOSThread stubThread() {
        return new BsdOSThread(42) {
            @Override
            protected List<BsdPsThreadKeyword> psThreadKeywords() {
                return Collections.emptyList();
            }

            @Override
            protected String psThreadCommand() {
                return "";
            }
        };
    }

    // Minimal ps thread row for the single-TIME-column platforms (DragonFly, OpenBSD, NetBSD).
    private static Map<BsdPsThreadKeyword, String> threadRow(String time) {
        Map<BsdPsThreadKeyword, String> threadMap = new EnumMap<>(BsdPsThreadKeyword.class);
        threadMap.put(BsdPsThreadKeyword.TID, "7");
        threadMap.put(BsdPsThreadKeyword.STATE, "S");
        threadMap.put(BsdPsThreadKeyword.TIME, time);
        return threadMap;
    }

    @Test
    void testUserTimeIsClampedMonotonic() {
        BsdOSThread thread = stubThread();
        thread.updateAttributes(threadRow("0:02.99"));
        assertThat(thread.getUserTime(), is(2990L));
        // A ps row reporting less CPU time than the last one holds at the previous value
        thread.updateAttributes(threadRow("0:02.50"));
        assertThat(thread.getUserTime(), is(2990L));
        // A genuine increase still comes through
        thread.updateAttributes(threadRow("0:03.25"));
        assertThat(thread.getUserTime(), is(3250L));
    }

    @Test
    void testKernelAndUserTimeClampedWithSystimeColumn() {
        BsdOSThread thread = stubThread();
        // FreeBSD reports systime separately and TIME is user+sys, so user time subtracts two ps columns
        Map<BsdPsThreadKeyword, String> first = threadRow("0:05.00");
        first.put(BsdPsThreadKeyword.SYSTIME, "0:02.00");
        thread.updateAttributes(first);
        assertThat(thread.getKernelTime(), is(2000L));
        assertThat(thread.getUserTime(), is(3000L));
        // Both components regress; both are clamped
        Map<BsdPsThreadKeyword, String> second = threadRow("0:04.00");
        second.put(BsdPsThreadKeyword.SYSTIME, "0:01.50");
        thread.updateAttributes(second);
        assertThat(thread.getKernelTime(), is(2000L));
        assertThat(thread.getUserTime(), is(3000L));
    }
}
