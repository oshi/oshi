/*
 * Copyright 2019-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;

/**
 * Test GlobalMemory
 */
class VirtualMemoryTest {

    protected VirtualMemory createVirtualMemory() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        GlobalMemory memory = hal.getMemory();
        return memory.getVirtualMemory();
    }

    /**
     * Test VirtualMemory.
     */
    @Test
    void testVirtualMemory() {
        VirtualMemory vm = createVirtualMemory();
        assertThat("VM object shouldn't be null", vm, is(notNullValue()));

        // Swap tests
        assertThat("VM's swap pages in shouldn't be negative", vm.getSwapPagesIn(), is(greaterThanOrEqualTo(0L)));
        assertThat("VM's swap pages out shouldn't be negative", vm.getSwapPagesOut(), is(greaterThanOrEqualTo(0L)));
        assertThat("VM's swap total shouldn't be negative", vm.getSwapTotal(), is(greaterThanOrEqualTo(0L)));
        assertThat("VM's swap used shouldn't be negative", vm.getSwapUsed(), is(greaterThanOrEqualTo(0L)));
        assertThat("VM's swap used should be less than or equal to VM swap total", vm.getSwapUsed(),
                is(lessThanOrEqualTo(vm.getSwapTotal())));
        assertThat("VM's max should be greater than or qual to VM swap total", vm.getVirtualMax() >= vm.getSwapTotal(),
                is(true));
        assertThat("VM's virtual in use shouldn't be negative", vm.getVirtualInUse(), is(greaterThanOrEqualTo(0L)));
        assertThat("VM's toString contains 'Used'", vm.toString(), containsString("Used"));
    }
}
