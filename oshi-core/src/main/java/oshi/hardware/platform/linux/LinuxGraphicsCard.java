/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GraphicsCard;
import oshi.hardware.GpuStats;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
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

    @Override
    public GpuStats createStatsSession() {
        return new LinuxGpuStats(drmDevicePath, driverName, pciBusId, getName());
    }

    /**
     * public method used by {@code AbstractHardwareAbstractionLayer} to access the graphics cards.
     *
     * @return List of {@link LinuxGraphicsCard} objects.
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
                lookupDevice = null;
                name = Constants.UNKNOWN;
                deviceId = Constants.UNKNOWN;
                vendor = Constants.UNKNOWN;
                versionInfoList.clear();
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
                            lookupDevice != null ? queryLspciMemorySize(lookupDevice) : 0L, drmInfo.getA(),
                            drmInfo.getB(), drmInfo.getC()));
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
                    } else if (prefix.equals("Rev")) {
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
                    lookupDevice != null ? queryLspciMemorySize(lookupDevice) : 0L, drmInfo.getA(), drmInfo.getB(),
                    drmInfo.getC()));
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
        List<String> lshw = ExecutingCommand.runPrivilegedNative("lshw -C display");
        String name = Constants.UNKNOWN;
        String deviceId = Constants.UNKNOWN;
        String vendor = Constants.UNKNOWN;
        List<String> versionInfoList = new ArrayList<>();
        long vram = 0;
        int cardNum = 0;
        String busInfo = null;
        for (String line : lshw) {
            String[] split = line.trim().split(":", 2);
            if (split[0].startsWith("*-display")) {
                // Save previous card
                if (cardNum++ > 0) {
                    Triplet<String, String, String> drmInfo = findDrmInfo(busInfo);
                    cardList.add(new LinuxGraphicsCard(name, deviceId, vendor,
                            versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList), vram,
                            drmInfo.getA(), drmInfo.getB(), drmInfo.getC()));
                }
                name = Constants.UNKNOWN;
                deviceId = Constants.UNKNOWN;
                vendor = Constants.UNKNOWN;
                vram = 0;
                versionInfoList.clear();
                busInfo = null;
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
                } else if (prefix.equals("bus info")) {
                    // lshw reports PCI slot as "pci@0000:01:00.0"; the value contains multiple
                    // colons so we locate the first colon in the original line to get the full value.
                    int colonIdx = line.indexOf(':');
                    String raw = colonIdx >= 0 ? line.substring(colonIdx + 1).trim() : "";
                    busInfo = raw.startsWith("pci@") ? raw.substring(4) : raw;
                }
            }
        }
        if (cardNum > 0) {
            Triplet<String, String, String> drmInfo = findDrmInfo(busInfo);
            cardList.add(new LinuxGraphicsCard(name, deviceId, vendor,
                    versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList), vram,
                    drmInfo.getA(), drmInfo.getB(), drmInfo.getC()));
        }
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
        String name = FileUtil.getFileName(target);
        return name.isEmpty() ? target : name;
    }
}
