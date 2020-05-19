/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import oshi.SystemInfo;

/**
 * Test GraphicsCard
 */
public class GraphicsCardTest {

    /**
     * Testing sound cards , each attribute.
     */
    @Test
    public void testGraphicsCards() {
        SystemInfo info = new SystemInfo();
        for (GraphicsCard graphicsCard : info.getHardware().getGraphicsCards()) {
            assertNotNull("Graphics card's name should not be null", graphicsCard.getName());
            assertNotNull("Graphics card's vendor should not be null", graphicsCard.getVendor());
            assertTrue("Graphics card's VRAM should be at least zero", graphicsCard.getVRam() >= 0);
            assertEquals("Graphics card's VRAM should be divisible by 1024", 0, graphicsCard.getVRam() % 1024);
            assertTrue("Graphics card's id number length should be at least 6",
                    graphicsCard.getDeviceId().length() >= 6);
            assertTrue("Graphics card's version information length should be at least 2",
                    graphicsCard.getVersionInfo().length() >= 2);
        }
    }
}
