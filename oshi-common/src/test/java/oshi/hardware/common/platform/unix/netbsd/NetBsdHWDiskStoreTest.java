/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class NetBsdHWDiskStoreTest {

    /** Captured from {@code iostat -x -I} on NetBSD 10.1. */
    private static final List<String> FIRST = """
            device  read KB/t    xfr   time     MB   write KB/t    xfr   time     MB
            ld0         26.36   2087   8.23    53.72      25.42  74725   8.23  1855.30
            fd0          3.34    179  92.38     0.58       0.00      0  92.38     0.00
            dk0         27.45   1995  14.32    53.47      25.42  74725  14.32  1855.30
            cd0          0.00      0   0.00     0.00       0.00      0   0.00     0.00
            """.lines().toList();

    /** The same drives two seconds later. The write KB/t figure has fallen while the transfer count has risen. */
    private static final List<String> SECOND = """
            device  read KB/t    xfr   time     MB   write KB/t    xfr   time     MB
            ld0         26.36   2087   8.23    53.72      25.40  74833   8.23  1856.52
            fd0          3.34    179  92.38     0.58       0.00      0  92.38     0.00
            dk0         27.45   1995  14.32    53.47      25.40  74833  14.32  1856.52
            cd0          0.00      0   0.00     0.00       0.00      0   0.00     0.00
            """.lines().toList();

    private static NetBsdHWDiskStore store(String name, List<String> iostat) {
        return new NetBsdHWDiskStore(name, "model", "serial", 1024L, () -> iostat);
    }

    @Test
    void testUpdateAttributes() {
        NetBsdHWDiskStore ld0 = store("ld0", FIRST);
        assertTrue(ld0.updateAttributes());
        assertThat(ld0.getReads(), is(2087L));
        assertThat(ld0.getWrites(), is(74725L));
        // The MB columns, not KB-per-transfer times transfers
        assertThat(ld0.getReadBytes(), is((long) (53.72 * 1024 * 1024)));
        assertThat(ld0.getWriteBytes(), is((long) (1855.30 * 1024 * 1024)));
        // One drive-busy total, not the two identical columns added together
        assertThat(ld0.getTransferTime(), is(8230L));
        assertThat(ld0.getTimeStamp(), greaterThanOrEqualTo(1L));
    }

    @Test
    void testBytesNeverGoBackwards() {
        // iostat's write KB/t for ld0 rounds down from 25.42 to 25.40 between these two samples while the transfer
        // count rises, so a KB/t times transfers reading would report fewer bytes written than a moment earlier
        NetBsdHWDiskStore before = store("ld0", FIRST);
        NetBsdHWDiskStore after = store("ld0", SECOND);
        assertTrue(before.updateAttributes());
        assertTrue(after.updateAttributes());
        assertThat(after.getWrites(), greaterThanOrEqualTo(before.getWrites()));
        assertThat(after.getWriteBytes(), greaterThanOrEqualTo(before.getWriteBytes()));
        assertThat(after.getReadBytes(), greaterThanOrEqualTo(before.getReadBytes()));
    }

    @Test
    void testIdleAndMissingDrives() {
        NetBsdHWDiskStore cd0 = store("cd0", FIRST);
        assertTrue(cd0.updateAttributes());
        assertThat(cd0.getReadBytes(), is(0L));
        assertThat(cd0.getWriteBytes(), is(0L));
        assertThat(cd0.getTransferTime(), is(0L));

        NetBsdHWDiskStore absent = store("sd9", FIRST);
        assertFalse(absent.updateAttributes());
    }
}
