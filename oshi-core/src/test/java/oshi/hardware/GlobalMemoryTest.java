/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware;

import org.junit.Test;
import oshi.SystemInfo;
import oshi.hardware.platform.windows.WindowsGlobalMemory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test GlobalMemory
 */
public class GlobalMemoryTest {
    /**
     * Test GlobalMemory.
     */
    @Test
    public void testGlobalMemory() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        GlobalMemory memory = hal.getMemory();
        assertNotNull(memory);

        // RAM tests
        assertTrue(memory.getTotal() > 0);
        assertTrue(memory.getAvailable() >= 0);
        assertTrue(memory.getAvailable() <= memory.getTotal());

        // Swap tests
        assertTrue(memory.getSwapTotal() >= 0);
        assertTrue(memory.getSwapUsed() >= 0);
        assertTrue(memory.getSwapUsed() <= memory.getSwapTotal());
    }

    /**
     * Test OSProcess setters and getters
     */
    @Test
    public void testGlobalMemoryCopy() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        GlobalMemory oldMemory = hal.getMemory();
        assertNotNull(oldMemory);

        WindowsGlobalMemory newMemory = new WindowsGlobalMemory();

        long availableMemory = oldMemory.getAvailable();
        newMemory.setAvailable(availableMemory);
        newMemory.setTotal(oldMemory.getTotal());
        newMemory.setSwapTotal(oldMemory.getSwapTotal());
        newMemory.setSwapUsed(oldMemory.getSwapUsed());

        assertEquals(oldMemory.getAvailable(), availableMemory);
        assertEquals(oldMemory.getTotal(),newMemory.getTotal());
        assertEquals(oldMemory.getSwapTotal(),newMemory.getSwapTotal());
        assertEquals(oldMemory.getSwapUsed(),newMemory.getSwapUsed());
    }
}
