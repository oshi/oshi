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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import oshi.SystemInfo;

/**
 * Tests Computer System
 */
public class ComputerSystemTest {

    /**
     * Test Computer System
     */
    @Test
    public void testComputerSystem() {
        SystemInfo si = new SystemInfo();
        ComputerSystem cs = si.getHardware().getComputerSystem();
        assertNotNull("Computer System's manufacturer shouldn't be null", cs.getManufacturer());
        assertNotNull("Computer System's model shouldn't be null", cs.getModel());
        assertNotNull("Computer System's serial number shouldn't be null", cs.getSerialNumber());

        Firmware fw = cs.getFirmware();
        assertNotNull("Firmware shouldn't be null", fw);
        assertNotNull("Firmware's manufacturer shouldn't be null", fw.getManufacturer());
        assertNotNull("Firmware's name shouldn't be null", fw.getName());
        assertNotNull("Firmware's description shouldn't be null", fw.getDescription());
        assertNotNull("Firmware's version shouldn't be null", fw.getVersion());
        assertNotNull("Firmware's release date shouldn't be null", fw.getReleaseDate());
        assertTrue("Firmware's tostring value should contain manufacturer's name",
                fw.toString().contains(fw.getManufacturer()));

        Baseboard bb = cs.getBaseboard();
        assertNotNull("Baseboard shouldn't be null", bb);
        assertNotNull("Baseboard's manufacturer shouldn't be null", bb.getManufacturer());
        assertNotNull("Baseboard's model shouldn't be null", bb.getModel());
        assertNotNull("Baseboard's version shouldn't be null", bb.getVersion());
        assertNotNull("Baseboard's serial number shouldn't be null", bb.getSerialNumber());
    }
}
