/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.json.hardware;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import oshi.json.SystemInfo;

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
        assertTrue(memory.getPageSize() > 0);

        // Swap tests
        assertTrue(memory.getSwapPagesIn() >= 0);
        assertTrue(memory.getSwapPagesOut() >= 0);
        assertTrue(memory.getSwapTotal() >= 0);
        assertTrue(memory.getSwapUsed() >= 0);
        assertTrue(memory.getSwapUsed() <= memory.getSwapTotal());
    }
}
