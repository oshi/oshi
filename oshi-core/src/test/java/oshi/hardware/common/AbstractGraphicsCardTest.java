/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import oshi.hardware.GpuStats;
import oshi.hardware.GpuTicks;

class AbstractGraphicsCardTest {

    /** Returns fixed metric values from its GpuStats session. */
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
        public GpuStats createStatsSession() {
            return new GpuStats() {
                private boolean closed;

                @Override
                public void close() {
                    closed = true;
                }

                @Override
                public boolean isClosed() {
                    return closed;
                }

                @Override
                public GpuTicks getGpuTicks() {
                    checkOpen();
                    return new DefaultGpuTicks(System.nanoTime() / 100L, 0L);
                }

                @Override
                public double getGpuUtilization() {
                    checkOpen();
                    return -1d;
                }

                @Override
                public long getVramUsed() {
                    checkOpen();
                    return -1L;
                }

                @Override
                public long getSharedMemoryUsed() {
                    checkOpen();
                    return -1L;
                }

                @Override
                public double getTemperature() {
                    checkOpen();
                    return temperature;
                }

                @Override
                public double getPowerDraw() {
                    checkOpen();
                    return power;
                }

                @Override
                public long getCoreClockMhz() {
                    checkOpen();
                    return coreClock;
                }

                @Override
                public long getMemoryClockMhz() {
                    checkOpen();
                    return memClock;
                }

                @Override
                public double getFanSpeedPercent() {
                    checkOpen();
                    return fan;
                }

                private void checkOpen() {
                    if (closed) {
                        throw new IllegalStateException("GpuStats session is closed");
                    }
                }
            };
        }
    }

    @Test
    void testCreateStatsSessionNeverNull() {
        assertThat(new StubGraphicsCard(-1d, -1d, -1L, -1L, -1d).createStatsSession(), is(notNullValue()));
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
        String s = new StubGraphicsCard(0d, 0d, 0L, 0L, 0d).toString();
        assertThat(s, containsString("temp=0.0°C"));
        assertThat(s, containsString("power=0.0W"));
        assertThat(s, containsString("coreClock=0MHz"));
        assertThat(s, containsString("memClock=0MHz"));
        assertThat(s, containsString("fan=0.0%"));
    }

    @Test
    void testDefaultNoOpSessionReturnsAllSentinels() {
        AbstractGraphicsCard card = new AbstractGraphicsCard("GPU", "0x0", "Vendor", "v0", 0L) {
        };
        try (GpuStats stats = card.createStatsSession()) {
            assertThat(stats.getGpuUtilization(), is(-1d));
            assertThat(stats.getVramUsed(), is(-1L));
            assertThat(stats.getSharedMemoryUsed(), is(-1L));
            assertThat(stats.getTemperature(), is(-1d));
            assertThat(stats.getPowerDraw(), is(-1d));
            assertThat(stats.getCoreClockMhz(), is(-1L));
            assertThat(stats.getMemoryClockMhz(), is(-1L));
            assertThat(stats.getFanSpeedPercent(), is(-1d));
            GpuTicks ticks = stats.getGpuTicks();
            assertThat(ticks, is(notNullValue()));
            assertThat(ticks.getActiveTicks(), is(-1L));
        }
    }
}
