/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux.proc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.util.FileUtil;
import oshi.util.Util;

@EnabledOnOs(OS.LINUX)
class AuxvTest {

    // A native long is the JVM's word size, which the os.arch name alone does not reliably tell (s390x is 64-bit)
    private static final Auxv.NativeLongReader READER = Util.is64Bit() ? FileUtil::readLongFromBuffer
            : FileUtil::readIntFromBuffer;

    @Test
    void testQueryAuxv() {
        Map<Integer, Long> auxv = Auxv.queryAuxv(READER);
        assertNotNull(auxv, "Aux vector should not be null");
        assertThat("Clock Ticks should be positive", auxv.getOrDefault(Auxv.AT_CLKTCK, 0L), greaterThan(0L));
        assertThat("Page Size should be positive", auxv.getOrDefault(Auxv.AT_PAGESZ, 0L), greaterThan(0L));
    }
}
