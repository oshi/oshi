/*
 * MIT License
 *
 * Copyright (c) 2019-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
    /**
     * Test VirtualMemory.
     */
    @Test
    void testGlobalMemory() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        GlobalMemory memory = hal.getMemory();
        VirtualMemory vm = memory.getVirtualMemory();
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
