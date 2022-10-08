/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
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
