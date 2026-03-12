/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import oshi.hardware.GpuTicks;

class AbstractGraphicsCardTest {

    /** Minimal subclass that returns fixed Phase 2 metric values. */
    private static final class StubGraphicsCard extends AbstractGraphicsCard {
        private final double temperature;
        private final double power;
        private final long coreClock;
        private final long memClock;
        private final double fan;

        StubGraphicsCard(double temperature, double power, long coreClock, long memClock, double fan) {
            super("Test GPU", "0xDEAD", "TestVendor", "v1.0", 8L * 1024 * 1024 * 1024);
            this.temperature = temperature;
            this.power = power;
            this.coreClock = coreClock;
            this.memClock = memClock;
            this.fan = fan;
        }

        @Override
        public GpuTicks getGpuTicks() {
            return new DefaultGpuTicks(0L, 0L);
        }

        @Override
        public double getTemperature() {
            return temperature;
        }

        @Override
        public double getPowerDraw() {
            return power;
        }

        @Override
        public long getCoreClockMhz() {
            return coreClock;
        }

        @Override
        public long getMemoryClockMhz() {
            return memClock;
        }

        @Override
        public double getFanSpeedPercent() {
            return fan;
        }
    }

    /**
     * Minimal subclass that only implements getGpuTicks(), leaving all Phase 2 methods to AbstractGraphicsCard's
     * defaults. Used to verify the default sentinel values without overriding them.
     */
    private static final class MinimalStubGraphicsCard extends AbstractGraphicsCard {
        MinimalStubGraphicsCard() {
            super("Minimal GPU", "0x0000", "MinimalVendor", "v0.0", 0L);
        }

        @Override
        public GpuTicks getGpuTicks() {
            return new DefaultGpuTicks(0L, 0L);
        }
    }

    @Test
    void testDefaultSentinelValues() {
        // Uses MinimalStubGraphicsCard so all Phase 2 getters come from AbstractGraphicsCard itself
        AbstractGraphicsCard card = new MinimalStubGraphicsCard();
        assertThat(card.getTemperature(), is(-1d));
        assertThat(card.getPowerDraw(), is(-1d));
        assertThat(card.getCoreClockMhz(), is(-1L));
        assertThat(card.getMemoryClockMhz(), is(-1L));
        assertThat(card.getFanSpeedPercent(), is(-1d));
    }

    @Test
    void testToStringOmitsPhase2WhenAllSentinel() {
        String s = new StubGraphicsCard(-1d, -1d, -1L, -1L, -1d).toString();
        assertThat(s, not(containsString("temp=")));
        assertThat(s, not(containsString("power=")));
        assertThat(s, not(containsString("coreClock=")));
        assertThat(s, not(containsString("memClock=")));
        assertThat(s, not(containsString("fan=")));
    }

    @Test
    void testToStringIncludesTemperatureWhenAvailable() {
        String s = new StubGraphicsCard(72.5d, -1d, -1L, -1L, -1d).toString();
        assertThat(s, containsString("temp=72.5°C"));
    }

    @Test
    void testToStringIncludesPowerWhenAvailable() {
        String s = new StubGraphicsCard(-1d, 150.0d, -1L, -1L, -1d).toString();
        assertThat(s, containsString("power=150.0W"));
    }

    @Test
    void testToStringIncludesClocksWhenAvailable() {
        String s = new StubGraphicsCard(-1d, -1d, 2100L, 1000L, -1d).toString();
        assertThat(s, containsString("coreClock=2100MHz"));
        assertThat(s, containsString("memClock=1000MHz"));
    }

    @Test
    void testToStringIncludesFanWhenAvailable() {
        String s = new StubGraphicsCard(-1d, -1d, -1L, -1L, 65.0d).toString();
        assertThat(s, containsString("fan=65.0%"));
    }

    @Test
    void testToStringIncludesAllPhase2WhenAllAvailable() {
        String s = new StubGraphicsCard(80.0d, 200.0d, 2500L, 1200L, 75.0d).toString();
        assertThat(s, containsString("temp=80.0°C"));
        assertThat(s, containsString("power=200.0W"));
        assertThat(s, containsString("coreClock=2500MHz"));
        assertThat(s, containsString("memClock=1200MHz"));
        assertThat(s, containsString("fan=75.0%"));
    }

    @Test
    void testToStringAlwaysContainsBaseFields() {
        String s = new StubGraphicsCard(-1d, -1d, -1L, -1L, -1d).toString();
        assertThat(s, containsString("name=Test GPU"));
        assertThat(s, containsString("deviceId=0xDEAD"));
        assertThat(s, containsString("vendor=TestVendor"));
        assertThat(s, containsString("versionInfo=[v1.0]"));
    }

    @Test
    void testToStringIncludesZeroValues() {
        // Zero is a valid reading (>= 0); toString must include it, not treat it as sentinel
        String s = new StubGraphicsCard(0d, 0d, 0L, 0L, 0d).toString();
        assertThat(s, containsString("temp=0.0°C"));
        assertThat(s, containsString("power=0.0W"));
        assertThat(s, containsString("coreClock=0MHz"));
        assertThat(s, containsString("memClock=0MHz"));
        assertThat(s, containsString("fan=0.0%"));
    }
}
