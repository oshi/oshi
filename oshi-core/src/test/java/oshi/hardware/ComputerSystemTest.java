/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
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
