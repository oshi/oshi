/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.sun.jna.Pointer;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GraphicsCard;
import oshi.hardware.GpuTicks;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.hardware.common.DefaultGpuTicks;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.gpu.NvmlUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Graphics card info obtained by lshw, with dynamic metrics from sysfs DRM driver files.
 */
@ThreadSafe
final class LinuxGraphicsCard extends AbstractGraphicsCard {

    private static final String DRM_PATH = "/sys/class/drm/";

    // sysfs path for this card's device directory, e.g. /sys/class/drm/card0/device
    // Empty string if this card has no associated DRM sysfs entry.
    private final String drmDevicePath;

    // Driver name detected from the sysfs driver symlink, e.g. "amdgpu", "i915", "xe", "nvidia"
    private final String driverName;

    // PCI bus ID string for NVML correlation, e.g. "0000:01:00.0". Empty if unknown.
    private final String pciBusId;

    /**
     * Constructor for LinuxGraphicsCard
     *
     * @param name          The name
     * @param deviceId      The device ID
     * @param vendor        The vendor
     * @param versionInfo   The version info
     * @param vram          The VRAM
     * @param drmDevicePath sysfs device path for this card, or empty string if unavailable
     * @param driverName    driver name (e.g. "amdgpu"), or empty string if unknown
     * @param pciBusId      PCI bus ID for NVML correlation, or empty string if unknown
     */
    LinuxGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram, String drmDevicePath,
            String driverName, String pciBusId) {
        super(name, deviceId, vendor, versionInfo, vram);
        this.drmDevicePath = drmDevicePath;
        this.driverName = driverName;
        this.pciBusId = pciBusId;
    }

    /**
     * public method used by {@link oshi.hardware.common.AbstractHardwareAbstractionLayer} to access the graphics cards.
     *
     * @return List of {@link oshi.hardware.platform.linux.LinuxGraphicsCard} objects.
     */
    public static List<GraphicsCard> getGraphicsCards() {
        List<GraphicsCard> cardList = getGraphicsCardsFromLspci();
        if (cardList.isEmpty()) {
            cardList = getGraphicsCardsFromLshw();
        }
        return cardList;
    }

    // Faster, use as primary
    private static List<GraphicsCard> getGraphicsCardsFromLspci() {
        List<GraphicsCard> cardList = new ArrayList<>();
        // Machine readable version
        List<String> lspci = ExecutingCommand.runNative("lspci -vnnm");
        String name = Constants.UNKNOWN;
        String deviceId = Constants.UNKNOWN;
        String vendor = Constants.UNKNOWN;
        List<String> versionInfoList = new ArrayList<>();
        boolean found = false;
        String lookupDevice = null;
        for (String line : lspci) {
            String[] split = line.trim().split(":", 2);
            String prefix = split[0];
            // Skip until line contains "VGA" or "3D controller"
            if (prefix.equals("Class") && (line.contains("VGA") || line.contains("3D controller"))) {
                found = true;
            } else if (prefix.equals("Slot") && split.length > 1) {
                // Capture PCI slot address (e.g. "01:00.0") for use with lspci -s
                lookupDevice = split[1].trim();
            }
            if (found) {
                if (split.length < 2) {
                    // Save previous card
                    Triplet<String, String, String> drmInfo = findDrmInfo(lookupDevice);
                    cardList.add(new LinuxGraphicsCard(name, deviceId, vendor,
                            versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList),
                            queryLspciMemorySize(lookupDevice), drmInfo.getA(), drmInfo.getB(), drmInfo.getC()));
                    versionInfoList.clear();
                    found = false;
                } else {
                    if (prefix.equals("Device")) {
                        Pair<String, String> pair = ParseUtil.parseLspciMachineReadable(split[1].trim());
                        if (pair != null) {
                            name = pair.getA();
                            deviceId = "0x" + pair.getB();
                        }
                    } else if (prefix.equals("Vendor")) {
                        Pair<String, String> pair = ParseUtil.parseLspciMachineReadable(split[1].trim());
                        if (pair != null) {
                            vendor = pair.getA() + " (0x" + pair.getB() + ")";
                        } else {
                            vendor = split[1].trim();
                        }
                    } else if (prefix.equals("Rev:")) {
                        versionInfoList.add(line.trim());
                    }
                }
            }
        }
        // If we haven't yet written the last card do so now
        if (found) {
            Triplet<String, String, String> drmInfo = findDrmInfo(lookupDevice);
            cardList.add(new LinuxGraphicsCard(name, deviceId, vendor,
                    versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList),
                    queryLspciMemorySize(lookupDevice), drmInfo.getA(), drmInfo.getB(), drmInfo.getC()));
        }
        return cardList;
    }

    private static long queryLspciMemorySize(String lookupDevice) {
        long vram = 0L;
        // Lookup memory
        // Human readable version, includes memory
        List<String> lspciMem = ExecutingCommand.runNative("lspci -v -s " + lookupDevice);
        for (String mem : lspciMem) {
            if (mem.contains(" prefetchable")) {
                vram += ParseUtil.parseLspciMemorySize(mem);
            }
        }
        return vram;
    }

    // Slower, use as backup
    private static List<GraphicsCard> getGraphicsCardsFromLshw() {
        List<GraphicsCard> cardList = new ArrayList<>();
        List<String> lshw = ExecutingCommand.runNative("lshw -C display");
        String name = Constants.UNKNOWN;
        String deviceId = Constants.UNKNOWN;
        String vendor = Constants.UNKNOWN;
        List<String> versionInfoList = new ArrayList<>();
        long vram = 0;
        int cardNum = 0;
        for (String line : lshw) {
            String[] split = line.trim().split(":");
            if (split[0].startsWith("*-display")) {
                // Save previous card
                if (cardNum++ > 0) {
                    Triplet<String, String, String> drmInfo = findDrmInfo(null);
                    cardList.add(new LinuxGraphicsCard(name, deviceId, vendor,
                            versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList), vram,
                            drmInfo.getA(), drmInfo.getB(), drmInfo.getC()));
                    versionInfoList.clear();
                }
            } else if (split.length == 2) {
                String prefix = split[0];
                if (prefix.equals("product")) {
                    name = split[1].trim();
                } else if (prefix.equals("vendor")) {
                    vendor = split[1].trim();
                } else if (prefix.equals("version")) {
                    versionInfoList.add(line.trim());
                } else if (prefix.startsWith("resources")) {
                    vram = ParseUtil.parseLshwResourceString(split[1].trim());
                }
            }
        }
        Triplet<String, String, String> drmInfo = findDrmInfo(null);
        cardList.add(new LinuxGraphicsCard(name, deviceId, vendor,
                versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList), vram,
                drmInfo.getA(), drmInfo.getB(), drmInfo.getC()));
        return cardList;
    }

    /**
     * Finds the sysfs DRM device path, driver name, and PCI bus ID for a GPU by matching against the PCI slot address
     * from the uevent file under each DRM card's device directory.
     *
     * <p>
     * When {@code pciSlot} is non-null, each card's {@code device/uevent} file is read and the {@code PCI_SLOT_NAME}
     * key is compared against the supplied slot (e.g. {@code "0000:01:00.0"} or {@code "01:00.0"}). The first card
     * whose slot matches is returned. If no match is found, or if {@code pciSlot} is null (lshw path), the first card
     * with a non-empty driver symlink is returned as a best-effort fallback.
     *
     * @param pciSlot the PCI slot address from lspci (e.g. {@code "01:00.0"}), or {@code null} to use first-match
     * @return triplet of (drmDevicePath, driverName, pciBusId), all empty strings if not found
     */
    private static Triplet<String, String, String> findDrmInfo(String pciSlot) {
        File drmDir = new File(DRM_PATH);
        File[] cards = drmDir.listFiles(f -> f.getName().matches("card\\d+"));
        if (cards == null) {
            return new Triplet<>("", "", "");
        }
        Triplet<String, String, String> firstWithDriver = null;
        for (File card : cards) {
            String devicePath = card.getAbsolutePath() + "/device";
            String driver = readDriverName(devicePath + "/driver");
            if (driver.isEmpty()) {
                continue;
            }
            String slotName = readUeventValue(devicePath + "/uevent", "PCI_SLOT_NAME");
            if (firstWithDriver == null) {
                firstWithDriver = new Triplet<>(devicePath, driver, slotName);
            }
            // Attempt PCI slot match via uevent
            if (pciSlot != null && slotName.endsWith(pciSlot)) {
                return new Triplet<>(devicePath, driver, slotName);
            }
        }
        // Fall back to first card with a driver symlink
        return firstWithDriver != null ? firstWithDriver : new Triplet<>("", "", "");
    }

    /**
     * Reads a key=value entry from a sysfs uevent file.
     *
     * @param ueventPath absolute path to the uevent file
     * @param key        the key to look up (e.g. {@code "PCI_SLOT_NAME"})
     * @return the value string, or empty string if not found
     */
    private static String readUeventValue(String ueventPath, String key) {
        List<String> lines = FileUtil.readFile(ueventPath);
        String prefix = key + "=";
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private static String readDriverName(String driverSymlink) {
        String target = FileUtil.readSymlinkTarget(new File(driverSymlink));
        if (target == null || target.isEmpty()) {
            return "";
        }
        // The symlink target resolves to a driver directory,
        // e.g. "../../../bus/pci/drivers/amdgpu"; the last path segment is the driver name.
        int lastSlash = target.lastIndexOf('/');
        return lastSlash >= 0 ? target.substring(lastSlash + 1) : target;
    }

    // -------------------------------------------------------------------------
    // Dynamic metric implementations
    // -------------------------------------------------------------------------

    @Override
    public GpuTicks getGpuTicks() {
        // Tick-level counters are not available through sysfs on Linux.
        return new DefaultGpuTicks(System.nanoTime() / 100L, 0L);
    }

    @Override
    public double getGpuUtilization() {
        if (drmDevicePath.isEmpty()) {
            return -1d;
        }
        String driver = driverName.toLowerCase(Locale.ROOT);
        if ("amdgpu".equals(driver)) {
            int pct = FileUtil.getIntFromFile(drmDevicePath + "/gpu_busy_percent");
            return pct >= 0 ? pct : -1d;
        }
        if ("i915".equals(driver) || "xe".equals(driver)) {
            return intelFreqUtilization();
        }
        // nvidia / nouveau and others: not available via sysfs
        return -1d;
    }

    private double intelFreqUtilization() {
        // Frequency ratio is a rough proxy for GPU utilization; not measured core utilization.
        String gtPath = drmDevicePath + "/../gt/gt0";
        long actual = FileUtil.getLongFromFile(gtPath + "/rps_act_freq_mhz");
        long max = FileUtil.getLongFromFile(gtPath + "/rps_max_freq_mhz");
        if (actual > 0 && max > 0) {
            return Math.min(100.0, actual * 100.0 / max);
        }
        return -1d;
    }

    @Override
    public long getVramUsed() {
        if (drmDevicePath.isEmpty()) {
            return -1L;
        }
        String driver = driverName.toLowerCase(Locale.ROOT);
        if ("amdgpu".equals(driver)) {
            long used = FileUtil.getLongFromFile(drmDevicePath + "/mem_info_vram_used");
            return used >= 0 ? used : -1L;
        }
        // Intel integrated GPUs use system memory; dedicated VRAM usage not exposed in sysfs.
        // NVIDIA requires NVML (Phase 2).
        return -1L;
    }

    @Override
    public long getSharedMemoryUsed() {
        return -1L;
    }

    @Override
    public double getTemperature() {
        // Priority 1: NVML
        Pointer nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            double val = NvmlUtil.getTemperature(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        // Priority 2: hwmon temp1_input (millidegrees C -> degrees C)
        String hwmon = findHwmonPath();
        if (!hwmon.isEmpty()) {
            long milliC = FileUtil.getLongFromFile(hwmon + "/temp1_input");
            if (milliC > 0) {
                return milliC / 1000.0;
            }
        }
        return -1d;
    }

    @Override
    public double getPowerDraw() {
        // Priority 1: NVML
        Pointer nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            double val = NvmlUtil.getPowerDraw(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        // Priority 2: hwmon power1_average (microwatts -> watts); AMD only
        String hwmon = findHwmonPath();
        if (!hwmon.isEmpty()) {
            long microW = FileUtil.getLongFromFile(hwmon + "/power1_average");
            if (microW > 0) {
                return microW / 1_000_000.0;
            }
        }
        return -1d;
    }

    @Override
    public long getCoreClockMhz() {
        // Priority 1: NVML
        Pointer nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            long val = NvmlUtil.getCoreClockMhz(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        if (drmDevicePath.isEmpty()) {
            return -1L;
        }
        String driver = driverName.toLowerCase(Locale.ROOT);
        // AMD: try hwmon freq1_input (Hz -> MHz), then pp_dpm_sclk active state
        if ("amdgpu".equals(driver)) {
            String hwmon = findHwmonPath();
            if (!hwmon.isEmpty()) {
                long hz = FileUtil.getLongFromFile(hwmon + "/freq1_input");
                if (hz > 0) {
                    return hz / 1_000_000L;
                }
            }
            return parseDpmActiveMhz(drmDevicePath + "/pp_dpm_sclk");
        }
        // Intel: rps_cur_freq_mhz
        if ("i915".equals(driver) || "xe".equals(driver)) {
            long mhz = FileUtil.getLongFromFile(drmDevicePath + "/../gt/gt0/rps_cur_freq_mhz");
            return mhz > 0 ? mhz : -1L;
        }
        return -1L;
    }

    @Override
    public long getMemoryClockMhz() {
        // Priority 1: NVML
        Pointer nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            long val = NvmlUtil.getMemoryClockMhz(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        if (drmDevicePath.isEmpty()) {
            return -1L;
        }
        String driver = driverName.toLowerCase(Locale.ROOT);
        // AMD: try hwmon freq2_input (Hz -> MHz), then pp_dpm_mclk active state
        if ("amdgpu".equals(driver)) {
            String hwmon = findHwmonPath();
            if (!hwmon.isEmpty()) {
                long hz = FileUtil.getLongFromFile(hwmon + "/freq2_input");
                if (hz > 0) {
                    return hz / 1_000_000L;
                }
            }
            return parseDpmActiveMhz(drmDevicePath + "/pp_dpm_mclk");
        }
        return -1L;
    }

    @Override
    public double getFanSpeedPercent() {
        // Priority 1: NVML
        Pointer nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            double val = NvmlUtil.getFanSpeedPercent(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        // Priority 2: hwmon fan1_input / fan1_max (AMD)
        String hwmon = findHwmonPath();
        if (!hwmon.isEmpty()) {
            long fanRpm = FileUtil.getLongFromFile(hwmon + "/fan1_input");
            long fanMax = FileUtil.getLongFromFile(hwmon + "/fan1_max");
            if (fanRpm > 0 && fanMax > 0) {
                return Math.min(100.0, fanRpm * 100.0 / fanMax);
            }
            // Fallback: PWM percentage
            long pwm = FileUtil.getLongFromFile(hwmon + "/pwm1");
            if (pwm > 0) {
                return pwm / 255.0 * 100.0;
            }
        }
        return -1d;
    }

    /**
     * Finds the hwmon directory for this card via the direct sysfs path {@code drmDevicePath/hwmon/hwmonN}.
     *
     * @return absolute path to the hwmon directory, or empty string if not found
     */
    private String findHwmonPath() {
        if (drmDevicePath.isEmpty()) {
            return "";
        }
        File hwmonDir = new File(drmDevicePath + "/hwmon");
        File[] entries = hwmonDir.listFiles(f -> f.getName().startsWith("hwmon"));
        if (entries != null && entries.length > 0) {
            return entries[0].getAbsolutePath();
        }
        return "";
    }

    /**
     * Parses the active clock frequency from a {@code pp_dpm_sclk} or {@code pp_dpm_mclk} sysfs file. The active state
     * is marked with {@code *}, e.g. {@code "1: 800Mhz *"}.
     *
     * @param path absolute path to the pp_dpm file
     * @return active clock in MHz, or -1 if not parseable
     */
    private static long parseDpmActiveMhz(String path) {
        for (String line : FileUtil.readFile(path, false)) {
            if (line.endsWith("*")) {
                // Format: "N: <value>Mhz *" or "N: <value>MHz *"
                int mhzIdx = line.toLowerCase(Locale.ROOT).indexOf("mhz");
                if (mhzIdx > 0) {
                    int start = line.lastIndexOf(' ', mhzIdx - 1);
                    if (start >= 0) {
                        return ParseUtil.parseLongOrDefault(line.substring(start + 1, mhzIdx), -1L);
                    }
                }
            }
        }
        return -1L;
    }

    /**
     * Finds the NVML device handle for this card. Tries PCI bus ID first, then falls back to name matching.
     *
     * @return NVML device handle, or null if NVML unavailable or no match
     */
    private Pointer findNvmlDevice() {
        if (!NvmlUtil.isAvailable()) {
            return null;
        }
        if (!pciBusId.isEmpty()) {
            Pointer handle = NvmlUtil.findDevice(pciBusId);
            if (handle != null) {
                return handle;
            }
        }
        return NvmlUtil.findDeviceByName(getName());
    }
}
