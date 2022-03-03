/*
 * MIT License
 *
 * Copyright (c) 2016-2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;

/**
 * Tests Computer System
 */
class ComputerSystemTest {
    private static final Pattern UUID_PATTERN = Pattern
            .compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}|unknown");

    /**
     * Test Computer System
     */
    @Test
    void testComputerSystem() {
        SystemInfo si = new SystemInfo();
        ComputerSystem cs = si.getHardware().getComputerSystem();
        assertThat("Computer System's manufacturer shouldn't be null", cs.getManufacturer(), is(notNullValue()));
        assertThat("Computer System's model shouldn't be null", cs.getModel(), is(notNullValue()));
        assertThat("Computer System's serial number shouldn't be null", cs.getSerialNumber(), is(notNullValue()));
        assertThat("Computer System's UUID should be in UUID format or unknown", cs.getHardwareUUID(),
                matchesPattern(UUID_PATTERN));

        Firmware fw = cs.getFirmware();
        assertThat("Firmware shouldn't be null", fw, is(notNullValue()));
        assertThat("Firmware's manufacturer shouldn't be null", fw.getManufacturer(), is(notNullValue()));
        assertThat("Firmware's name shouldn't be null", fw.getName(), is(notNullValue()));
        assertThat("Firmware's description shouldn't be null", fw.getDescription(), is(notNullValue()));
        assertThat("Firmware's version shouldn't be null", fw.getVersion(), is(notNullValue()));
        assertThat("Firmware's release date shouldn't be null", fw.getReleaseDate(), is(notNullValue()));
        assertThat("Firmware's tostring value should contain manufacturer's name", fw.toString(),
                containsString(fw.getManufacturer()));

        Baseboard bb = cs.getBaseboard();
        assertThat("Baseboard shouldn't be null", bb, is(notNullValue()));
        assertThat("Baseboard's manufacturer shouldn't be null", bb.getManufacturer(), is(notNullValue()));
        assertThat("Baseboard's model shouldn't be null", bb.getModel(), is(notNullValue()));
        assertThat("Baseboard's version shouldn't be null", bb.getVersion(), is(notNullValue()));
        assertThat("Baseboard's serial number shouldn't be null", bb.getSerialNumber(), is(notNullValue()));
    }
}
