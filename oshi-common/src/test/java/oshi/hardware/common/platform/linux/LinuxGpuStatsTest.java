/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static oshi.hardware.common.platform.linux.TestFileUtil.writeFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests {@link LinuxGpuStats} sysfs parsing logic using temp directory fixtures. A concrete subclass stubs all NVML
 * methods to unavailable, isolating the sysfs code paths.
 */
class LinuxGpuStatsTest {

    private static final double EPS = 1e-6;

    /** Concrete subclass with NVML always unavailable. Tracks findDevice call count. */
    private static final class StubLinuxGpuStats extends LinuxGpuStats {
        private int findDeviceCallCount;

        StubLinuxGpuStats(String drmDevicePath, String driverName, String pciBusId, String cardName) {
            super(drmDevicePath, driverName, pciBusId, cardName);
        }

        /**
         * Returns the number of times nvmlFindDevice was called.
         *
         * @return call count
         */
        int getFindDeviceCallCount() {
            return findDeviceCallCount;
        }

        @Override
        protected boolean nvmlIsAvailable() {
            return false;
        }

        @Override
        protected String nvmlFindDevice(String busId) {
            findDeviceCallCount++;
            return null;
        }

        @Override
        protected String nvmlFindDeviceByName(String name) {
            return null;
        }

        @Override
        protected long nvmlGetVramUsed(String deviceId) {
            return -1L;
        }

        @Override
        protected double nvmlGetTemperature(String deviceId) {
            return -1d;
        }

        @Override
        protected double nvmlGetPowerDraw(String deviceId) {
            return -1d;
        }

        @Override
        protected long nvmlGetCoreClockMhz(String deviceId) {
            return -1L;
        }

        @Override
        protected long nvmlGetMemoryClockMhz(String deviceId) {
            return -1L;
        }

        @Override
        protected double nvmlGetFanSpeedPercent(String deviceId) {
            return -1d;
        }
    }

    /** Concrete subclass simulating a working NVML device found by bus ID. Tracks findDevice call count. */
    private static final class NvmlLinuxGpuStats extends LinuxGpuStats {
        private int findDeviceCallCount;

        NvmlLinuxGpuStats(String drmDevicePath, String driverName, String pciBusId, String cardName) {
            super(drmDevicePath, driverName, pciBusId, cardName);
        }

        /**
         * Returns the number of times nvmlFindDevice was called.
         *
         * @return call count
         */
        int getFindDeviceCallCount() {
            return findDeviceCallCount;
        }

        @Override
        protected boolean nvmlIsAvailable() {
            return true;
        }

        @Override
        protected String nvmlFindDevice(String busId) {
            findDeviceCallCount++;
            return "nvml-device-0";
        }

        @Override
        protected String nvmlFindDeviceByName(String name) {
            return "nvml-device-0";
        }

        @Override
        protected long nvmlGetVramUsed(String deviceId) {
            return 2147483648L;
        }

        @Override
        protected double nvmlGetTemperature(String deviceId) {
            return 65.0;
        }

        @Override
        protected double nvmlGetPowerDraw(String deviceId) {
            return 120.0;
        }

        @Override
        protected long nvmlGetCoreClockMhz(String deviceId) {
            return 1800L;
        }

        @Override
        protected long nvmlGetMemoryClockMhz(String deviceId) {
            return 7000L;
        }

        @Override
        protected double nvmlGetFanSpeedPercent(String deviceId) {
            return 45.0;
        }
    }

    // -------------------------------------------------------------------------
    // Empty / missing path — all metrics return sentinel
    // -------------------------------------------------------------------------

    @Test
    void testEmptyPathReturnsSentinels() {
        try (LinuxGpuStats stats = new StubLinuxGpuStats("", "", "", "TestGPU")) {
            assertThat(stats.getGpuUtilization(), is(-1d));
            assertThat(stats.getVramUsed(), is(-1L));
            assertThat(stats.getTemperature(), is(-1d));
            assertThat(stats.getPowerDraw(), is(-1d));
            assertThat(stats.getCoreClockMhz(), is(-1L));
            assertThat(stats.getMemoryClockMhz(), is(-1L));
            assertThat(stats.getFanSpeedPercent(), is(-1d));
            assertThat(stats.getSharedMemoryUsed(), is(-1L));
            assertThat(stats.getGpuTicks().getActiveTicks(), is(0L));
            assertThat(stats.getGpuTicks().getIdleTicks(), is(0L));
        }
    }

    // -------------------------------------------------------------------------
    // Session lifecycle
    // -------------------------------------------------------------------------

    @Test
    void testCloseAndThrow() {
        LinuxGpuStats stats = new StubLinuxGpuStats("", "", "", "TestGPU");
        assertThat(stats.isClosed(), is(false));
        stats.close();
        assertThat(stats.isClosed(), is(true));
        assertThrows(IllegalStateException.class, stats::getGpuUtilization);
        assertThrows(IllegalStateException.class, stats::getVramUsed);
        assertThrows(IllegalStateException.class, stats::getTemperature);
        assertThrows(IllegalStateException.class, stats::getPowerDraw);
        assertThrows(IllegalStateException.class, stats::getCoreClockMhz);
        assertThrows(IllegalStateException.class, stats::getMemoryClockMhz);
        assertThrows(IllegalStateException.class, stats::getFanSpeedPercent);
        assertThrows(IllegalStateException.class, stats::getSharedMemoryUsed);
        assertThrows(IllegalStateException.class, stats::getGpuTicks);
    }

    // -------------------------------------------------------------------------
    // amdgpu driver
    // -------------------------------------------------------------------------

    @Test
    void testAmdgpuUtilization(@TempDir Path tmp) throws IOException {
        Path device = tmp.resolve("device");
        Files.createDirectories(device);
        writeFile(device.resolve("gpu_busy_percent"), "42\n");

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "amdgpu", "", "AMD GPU")) {
            assertThat(stats.getGpuUtilization(), closeTo(42.0, EPS));
        }
    }

    @Test
    void testAmdgpuVramUsed(@TempDir Path tmp) throws IOException {
        Path device = tmp.resolve("device");
        Files.createDirectories(device);
        writeFile(device.resolve("mem_info_vram_used"), "1073741824\n");

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "amdgpu", "", "AMD GPU")) {
            assertThat(stats.getVramUsed(), is(1073741824L));
        }
    }

    @Test
    void testAmdgpuTemperatureFromHwmon(@TempDir Path tmp) throws IOException {
        Path device = tmp.resolve("device");
        Path hwmon = device.resolve("hwmon/hwmon0");
        Files.createDirectories(hwmon);
        writeFile(hwmon.resolve("temp1_input"), "72000\n");

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "amdgpu", "", "AMD GPU")) {
            assertThat(stats.getTemperature(), closeTo(72.0, EPS));
        }
    }

    @Test
    void testAmdgpuPowerFromHwmon(@TempDir Path tmp) throws IOException {
        Path device = tmp.resolve("device");
        Path hwmon = device.resolve("hwmon/hwmon0");
        Files.createDirectories(hwmon);
        writeFile(hwmon.resolve("power1_average"), "150000000\n");

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "amdgpu", "", "AMD GPU")) {
            assertThat(stats.getPowerDraw(), closeTo(150.0, EPS));
        }
    }

    @Test
    void testAmdgpuCoreClockFromHwmonFreq(@TempDir Path tmp) throws IOException {
        Path device = tmp.resolve("device");
        Path hwmon = device.resolve("hwmon/hwmon0");
        Files.createDirectories(hwmon);
        writeFile(hwmon.resolve("freq1_input"), "1800000000\n");

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "amdgpu", "", "AMD GPU")) {
            assertThat(stats.getCoreClockMhz(), is(1800L));
        }
    }

    @Test
    void testAmdgpuCoreClockFallbackToDpm(@TempDir Path tmp) throws IOException {
        Path device = tmp.resolve("device");
        Files.createDirectories(device);
        // No hwmon freq1_input, fall back to pp_dpm_sclk
        writeFile(device.resolve("pp_dpm_sclk"), "0: 500Mhz\n1: 1200Mhz *\n2: 1800Mhz\n");

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "amdgpu", "", "AMD GPU")) {
            assertThat(stats.getCoreClockMhz(), is(1200L));
        }
    }

    @Test
    void testAmdgpuMemoryClockFromHwmonFreq(@TempDir Path tmp) throws IOException {
        Path device = tmp.resolve("device");
        Path hwmon = device.resolve("hwmon/hwmon0");
        Files.createDirectories(hwmon);
        writeFile(hwmon.resolve("freq2_input"), "1000000000\n");

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "amdgpu", "", "AMD GPU")) {
            assertThat(stats.getMemoryClockMhz(), is(1000L));
        }
    }

    @Test
    void testAmdgpuMemoryClockFallbackToDpm(@TempDir Path tmp) throws IOException {
        Path device = tmp.resolve("device");
        Files.createDirectories(device);
        writeFile(device.resolve("pp_dpm_mclk"), "0: 400Mhz\n1: 800Mhz\n2: 1000Mhz *\n");

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "amdgpu", "", "AMD GPU")) {
            assertThat(stats.getMemoryClockMhz(), is(1000L));
        }
    }

    @Test
    void testAmdgpuFanSpeedFromRpm(@TempDir Path tmp) throws IOException {
        Path device = tmp.resolve("device");
        Path hwmon = device.resolve("hwmon/hwmon0");
        Files.createDirectories(hwmon);
        writeFile(hwmon.resolve("fan1_input"), "1500\n");
        writeFile(hwmon.resolve("fan1_max"), "3000\n");

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "amdgpu", "", "AMD GPU")) {
            assertThat(stats.getFanSpeedPercent(), closeTo(50.0, EPS));
        }
    }

    @Test
    void testAmdgpuFanSpeedFallbackToPwm(@TempDir Path tmp) throws IOException {
        Path device = tmp.resolve("device");
        Path hwmon = device.resolve("hwmon/hwmon0");
        Files.createDirectories(hwmon);
        // No fan1_input/fan1_max, fall back to pwm1
        // pwm1 range is 0-255; 128 ≈ 50.2%
        writeFile(hwmon.resolve("pwm1"), "128\n");

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "amdgpu", "", "AMD GPU")) {
            assertThat(stats.getFanSpeedPercent(), closeTo(128.0 / 255.0 * 100.0, EPS));
        }
    }

    // -------------------------------------------------------------------------
    // i915 driver
    // -------------------------------------------------------------------------

    /**
     * Creates a temp directory layout matching real sysfs: {@code card0/device} and {@code card0/gt/gt0}. The
     * constructor computes gt0Path as {@code drmDevicePath + "/../gt/gt0"}, so the "device" dir must be a real
     * subdirectory so that ".." resolves to the card directory.
     *
     * @param tmp the JUnit {@code @TempDir} root
     * @return the {@code device} subdirectory path to pass as {@code drmDevicePath}
     * @throws IOException if directory creation fails
     */
    private static Path createCardWithGt(Path tmp) throws IOException {
        Path card = tmp.resolve("card0");
        Path device = card.resolve("device");
        Path gt0 = card.resolve("gt/gt0");
        Files.createDirectories(device);
        Files.createDirectories(gt0);
        return device;
    }

    @Test
    void testI915Utilization(@TempDir Path tmp) throws IOException {
        Path device = createCardWithGt(tmp);
        Path gt0 = device.getParent().resolve("gt/gt0");
        writeFile(gt0.resolve("rps_act_freq_mhz"), "1200\n");
        writeFile(gt0.resolve("rps_max_freq_mhz"), "1500\n");

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "i915", "", "Intel GPU")) {
            // 1200/1500 * 100 = 80%
            assertThat(stats.getGpuUtilization(), closeTo(80.0, EPS));
        }
    }

    @Test
    void testI915UtilizationZeroActual(@TempDir Path tmp) throws IOException {
        Path device = createCardWithGt(tmp);
        Path gt0 = device.getParent().resolve("gt/gt0");
        writeFile(gt0.resolve("rps_act_freq_mhz"), "0\n");
        writeFile(gt0.resolve("rps_max_freq_mhz"), "1500\n");

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "i915", "", "Intel GPU")) {
            assertThat(stats.getGpuUtilization(), closeTo(0.0, EPS));
        }
    }

    @Test
    void testI915CoreClock(@TempDir Path tmp) throws IOException {
        Path device = createCardWithGt(tmp);
        Path gt0 = device.getParent().resolve("gt/gt0");
        writeFile(gt0.resolve("rps_cur_freq_mhz"), "1350\n");

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "i915", "", "Intel GPU")) {
            assertThat(stats.getCoreClockMhz(), is(1350L));
        }
    }

    // -------------------------------------------------------------------------
    // xe driver (same paths as i915)
    // -------------------------------------------------------------------------

    @Test
    void testXeUtilization(@TempDir Path tmp) throws IOException {
        Path device = createCardWithGt(tmp);
        Path gt0 = device.getParent().resolve("gt/gt0");
        writeFile(gt0.resolve("rps_act_freq_mhz"), "900\n");
        writeFile(gt0.resolve("rps_max_freq_mhz"), "1800\n");

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "xe", "", "Intel GPU")) {
            assertThat(stats.getGpuUtilization(), closeTo(50.0, EPS));
        }
    }

    // -------------------------------------------------------------------------
    // Unknown driver — utilization/clock/memclock return sentinel
    // -------------------------------------------------------------------------

    @Test
    void testUnknownDriverReturnsSentinels(@TempDir Path tmp) throws IOException {
        Path device = tmp.resolve("device");
        Files.createDirectories(device);

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "nouveau", "", "GPU")) {
            assertThat(stats.getGpuUtilization(), is(-1d));
            assertThat(stats.getCoreClockMhz(), is(-1L));
            assertThat(stats.getMemoryClockMhz(), is(-1L));
        }
    }

    // -------------------------------------------------------------------------
    // NVML device resolution caching
    // -------------------------------------------------------------------------

    @Test
    void testNvmlUnavailableCachesResult() {
        StubLinuxGpuStats stats = new StubLinuxGpuStats("", "", "0000:01:00.0", "GPU");
        // First call resolves and caches
        assertThat(stats.findNvmlDevice() == null, is(true));
        assertThat(stats.getFindDeviceCallCount(), is(0)); // nvmlIsAvailable() is false, never calls findDevice
        // Second call uses cache — still no findDevice calls
        assertThat(stats.findNvmlDevice() == null, is(true));
        assertThat(stats.getFindDeviceCallCount(), is(0));
        stats.close();
    }

    // -------------------------------------------------------------------------
    // Hwmon not present — temperature/power/fan return sentinel
    // -------------------------------------------------------------------------

    @Test
    void testNoHwmonReturnsSentinels(@TempDir Path tmp) throws IOException {
        Path device = tmp.resolve("device");
        Files.createDirectories(device);
        // No hwmon subdirectory

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "amdgpu", "", "AMD GPU")) {
            assertThat(stats.getTemperature(), is(-1d));
            assertThat(stats.getPowerDraw(), is(-1d));
            assertThat(stats.getFanSpeedPercent(), is(-1d));
        }
    }

    // -------------------------------------------------------------------------
    // pp_dpm_sclk with no active entry (no asterisk)
    // -------------------------------------------------------------------------

    @Test
    void testDpmNoActiveEntry(@TempDir Path tmp) throws IOException {
        Path device = tmp.resolve("device");
        Files.createDirectories(device);
        writeFile(device.resolve("pp_dpm_sclk"), "0: 500Mhz\n1: 1200Mhz\n");

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "amdgpu", "", "AMD GPU")) {
            assertThat(stats.getCoreClockMhz(), is(-1L));
        }
    }

    // -------------------------------------------------------------------------
    // Non-amdgpu driver — VRAM returns sentinel
    // -------------------------------------------------------------------------

    @Test
    void testNonAmdgpuVramReturnsSentinel(@TempDir Path tmp) throws IOException {
        Path device = tmp.resolve("device");
        Files.createDirectories(device);

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "i915", "", "Intel GPU")) {
            assertThat(stats.getVramUsed(), is(-1L));
        }
    }

    // -------------------------------------------------------------------------
    // Non-amdgpu driver — memory clock returns sentinel
    // -------------------------------------------------------------------------

    @Test
    void testNonAmdgpuMemoryClockReturnsSentinel(@TempDir Path tmp) throws IOException {
        Path device = tmp.resolve("device");
        Files.createDirectories(device);

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "i915", "", "Intel GPU")) {
            assertThat(stats.getMemoryClockMhz(), is(-1L));
        }
    }

    // -------------------------------------------------------------------------
    // i915 utilization with capped value (actual > max)
    // -------------------------------------------------------------------------

    @Test
    void testI915UtilizationCappedAt100(@TempDir Path tmp) throws IOException {
        Path device = createCardWithGt(tmp);
        Path gt0 = device.getParent().resolve("gt/gt0");
        writeFile(gt0.resolve("rps_act_freq_mhz"), "2000\n");
        writeFile(gt0.resolve("rps_max_freq_mhz"), "1500\n");

        try (LinuxGpuStats stats = new StubLinuxGpuStats(device.toString(), "i915", "", "Intel GPU")) {
            assertThat(stats.getGpuUtilization(), closeTo(100.0, EPS));
        }
    }

    // -------------------------------------------------------------------------
    // NVML available — all metrics sourced from NVML
    // -------------------------------------------------------------------------

    @Test
    void testNvmlMetrics() {
        try (NvmlLinuxGpuStats stats = new NvmlLinuxGpuStats("", "nvidia", "0000:01:00.0", "NVIDIA GPU")) {
            assertThat(stats.getVramUsed(), is(2147483648L));
            assertThat(stats.getTemperature(), closeTo(65.0, EPS));
            assertThat(stats.getPowerDraw(), closeTo(120.0, EPS));
            assertThat(stats.getCoreClockMhz(), is(1800L));
            assertThat(stats.getMemoryClockMhz(), is(7000L));
            assertThat(stats.getFanSpeedPercent(), closeTo(45.0, EPS));
        }
    }

    @Test
    void testNvmlDeviceResolutionCaching() {
        NvmlLinuxGpuStats stats = new NvmlLinuxGpuStats("", "nvidia", "0000:01:00.0", "NVIDIA GPU");
        // First call resolves via bus ID
        assertThat(stats.findNvmlDevice(), is("nvml-device-0"));
        assertThat(stats.getFindDeviceCallCount(), is(1));
        // Second call uses cache — findDevice not called again
        assertThat(stats.findNvmlDevice(), is("nvml-device-0"));
        assertThat(stats.getFindDeviceCallCount(), is(1));
        stats.close();
    }

    @Test
    void testNvmlFallbackToNameLookup() {
        // Bus ID is non-empty so findDevice(busId) is called first, returns null;
        // then falls back to findDeviceByName
        LinuxGpuStats stats = new LinuxGpuStats("", "nvidia", "0000:01:00.0", "NVIDIA GPU") {
            @Override
            protected boolean nvmlIsAvailable() {
                return true;
            }

            @Override
            protected String nvmlFindDevice(String busId) {
                return null;
            }

            @Override
            protected String nvmlFindDeviceByName(String name) {
                return "nvml-by-name";
            }

            @Override
            protected long nvmlGetVramUsed(String deviceId) {
                return 1024L;
            }

            @Override
            protected double nvmlGetTemperature(String deviceId) {
                return -1d;
            }

            @Override
            protected double nvmlGetPowerDraw(String deviceId) {
                return -1d;
            }

            @Override
            protected long nvmlGetCoreClockMhz(String deviceId) {
                return -1L;
            }

            @Override
            protected long nvmlGetMemoryClockMhz(String deviceId) {
                return -1L;
            }

            @Override
            protected double nvmlGetFanSpeedPercent(String deviceId) {
                return -1d;
            }
        };
        assertThat(stats.findNvmlDevice(), is("nvml-by-name"));
        assertThat(stats.getVramUsed(), is(1024L));
        stats.close();
    }

    // -------------------------------------------------------------------------
    // Protected getters
    // -------------------------------------------------------------------------

    @Test
    void testProtectedGetters() {
        StubLinuxGpuStats stats = new StubLinuxGpuStats("/dev/path", "amdgpu", "0000:03:00.0", "RX 7900");
        assertThat(stats.getPciBusId(), is("0000:03:00.0"));
        assertThat(stats.getCardName(), is("RX 7900"));
        stats.close();
    }
}
