/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GraphicsCard;
import oshi.hardware.GpuTicks;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.hardware.common.DefaultGpuTicks;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Graphics card info obtained by lshw, with dynamic metrics from sysfs DRM driver files.
 */
@ThreadSafe
final class LinuxGraphicsCard extends AbstractGraphicsCard {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxGraphicsCard.class);

    private static final String DRM_PATH = "/sys/class/drm/";

    // sysfs path for this card's device directory, e.g. /sys/class/drm/card0/device
    // Empty string if this card has no associated DRM sysfs entry.
    private final String drmDevicePath;

    // Driver name detected from the sysfs driver symlink, e.g. "amdgpu", "i915", "xe", "nvidia"
    private final String driverName;

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
     */
    LinuxGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram, String drmDevicePath,
            String driverName) {
        super(name, deviceId, vendor, versionInfo, vram);
        this.drmDevicePath = drmDevicePath;
        this.driverName = driverName;
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
                    Pair<String, String> drmInfo = findDrmInfo(name);
                    cardList.add(new LinuxGraphicsCard(name, deviceId, vendor,
                            versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList),
                            queryLspciMemorySize(lookupDevice), drmInfo.getA(), drmInfo.getB()));
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
            Pair<String, String> drmInfo = findDrmInfo(name);
            cardList.add(new LinuxGraphicsCard(name, deviceId, vendor,
                    versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList),
                    queryLspciMemorySize(lookupDevice), drmInfo.getA(), drmInfo.getB()));
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
                    Pair<String, String> drmInfo = findDrmInfo(name);
                    cardList.add(new LinuxGraphicsCard(name, deviceId, vendor,
                            versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList), vram,
                            drmInfo.getA(), drmInfo.getB()));
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
        Pair<String, String> drmInfo = findDrmInfo(name);
        cardList.add(new LinuxGraphicsCard(name, deviceId, vendor,
                versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList), vram,
                drmInfo.getA(), drmInfo.getB()));
        return cardList;
    }

    /**
     * Finds the sysfs DRM device path and driver name for a GPU by matching card names under /sys/class/drm/cardN.
     *
     * @param gpuName the GPU name from lspci/lshw
     * @return pair of (drmDevicePath, driverName), both empty strings if not found
     */
    private static Pair<String, String> findDrmInfo(String gpuName) {
        File drmDir = new File(DRM_PATH);
        File[] cards = drmDir.listFiles(f -> f.getName().matches("card\\d+"));
        if (cards == null) {
            return new Pair<>("", "");
        }
        for (File card : cards) {
            String devicePath = card.getAbsolutePath() + "/device";
            // Try to match by reading the uevent name or just use the first card if only one
            String driverPath = devicePath + "/driver";
            String driver = readDriverName(driverPath);
            // sysfs does not expose the GPU marketing name, so reliable name matching is not
            // possible. Return the first card that has a driver symlink as a best-effort result.
            // On multi-GPU systems this may select the wrong card; accurate matching would
            // require correlating PCI slot names from uevent files with lspci output (future work).
            if (!driver.isEmpty()) {
                return new Pair<>(devicePath, driver);
            }
        }
        return new Pair<>("", "");
    }

    private static String readDriverName(String driverSymlink) {
        String target = FileUtil.readSymlinkTarget(new File(driverSymlink));
        if (target == null || target.isEmpty()) {
            return "";
        }
        // The symlink target ends with the driver module name, e.g. ".../kernel/drivers/gpu/drm/amdgpu/amdgpu.ko"
        // or the symlink itself points to a directory named after the driver.
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
            return pct >= 0 ? pct / 100.0 : -1d;
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
            return Math.min(1.0, actual / (double) max);
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
}
