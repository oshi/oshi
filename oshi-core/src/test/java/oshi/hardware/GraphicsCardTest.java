/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;

/**
 * Test GraphicsCard
 */
class GraphicsCardTest {

    /**
     * Testing sound cards , each attribute.
     */
    @Test
    void testGraphicsCards() {
        SystemInfo info = new SystemInfo();
        for (GraphicsCard graphicsCard : info.getHardware().getGraphicsCards()) {
            assertThat("Graphics card's name should not be null", graphicsCard.getName(), is(notNullValue()));
            assertThat("Graphics card's vendor should not be null", graphicsCard.getVendor(), is(notNullValue()));
            assertThat("Graphics card's VRAM should be at least zero", graphicsCard.getVRam(),
                    is(greaterThanOrEqualTo(0L)));
            assertThat("Graphics card's VRAM should be divisible by 1024", graphicsCard.getVRam() % 1024, is(0L));
            assertThat("Graphics card's id number length should be at least 6", graphicsCard.getDeviceId().length(),
                    is(greaterThanOrEqualTo(6)));
            assertThat("Graphics card's version information length should be at least 2",
                    graphicsCard.getVersionInfo().length(), is(greaterThanOrEqualTo(2)));
        }
    }
}
