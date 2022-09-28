/*
 * MIT License
 *
 * Copyright (c) 2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.hardware.CentralProcessor.LogicalProcessor;
import oshi.hardware.CentralProcessor.PhysicalProcessor;
import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.util.tuples.Triplet;

@EnabledOnOs(OS.WINDOWS)
class LogicalProcessorInformationTest {
    @Test
    void testGetLogicalProcessorInformation() {
        Triplet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>> info = LogicalProcessorInformation
                .getLogicalProcessorInformation();
        assertThat("Logical Processor list must not be empty", info.getA(), is(not(empty())));
        assertThat("Physical Processor list is null", info.getB(), is(nullValue()));
        assertThat("Cache list is null", info.getC(), is(nullValue()));
    }

    @Test
    void testGetLogicalProcessorInformationEx() {
        Triplet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>> info = LogicalProcessorInformation
                .getLogicalProcessorInformationEx();
        assertThat("Must be more Logical Processors than Physical Ones", info.getA().size(),
                greaterThanOrEqualTo(info.getB().size()));
        assertThat("Must be more Physical Processors than Caches", info.getB().size(),
                greaterThanOrEqualTo(info.getC().size()));
    }
}
