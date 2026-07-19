/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import java.io.File;
import java.net.NetworkInterface;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractNetworkIF;
import oshi.util.FileUtil;
import oshi.util.Util;
import oshi.util.linux.SysPath;

/**
 * LinuxNetworks class.
 */
@ThreadSafe
public abstract class LinuxNetworkIF extends AbstractNetworkIF {

    private volatile int ifType;
    private volatile boolean connectorPresent;
    private volatile String ifAlias = "";
    private volatile IfOperStatus ifOperStatus = IfOperStatus.UNKNOWN;

    /**
     * Creates a LinuxNetworkIF.
     *
     * @param netint the network interface
     * @param model  the model string
     * @throws InstantiationException if the interface cannot be instantiated
     */
    protected LinuxNetworkIF(NetworkInterface netint, String model) throws InstantiationException {
        super(netint, model);
        updateAttributes();
    }

    /**
     * Reads vendor/model from the sysfs uevent file.
     *
     * @param name the interface name
     * @return vendor + model string, or the interface name if not found
     */
    protected static String queryIfModelFromSysfs(String name) {
        return queryIfModelFromSysfs(name, SysPath.NET);
    }

    static String queryIfModelFromSysfs(String name, String netPath) {
        Map<String, String> uevent = FileUtil.getKeyValueMapFromFile(netPath + name + "/uevent", "=");
        return formatModel(uevent.get("ID_VENDOR_FROM_DATABASE"), uevent.get("ID_MODEL_FROM_DATABASE"), name);
    }

    /**
     * Formats a vendor/model pair into a display string, falling back to the interface name when no model is present.
     *
     * @param vendor   the vendor string, may be {@code null} or blank
     * @param model    the model string, may be {@code null} or blank
     * @param fallback the value to return when {@code model} is blank (typically the interface name)
     * @return {@code "vendor model"}, {@code "model"}, or {@code fallback}
     */
    protected static String formatModel(String vendor, String model, String fallback) {
        if (!Util.isBlank(model)) {
            if (!Util.isBlank(vendor)) {
                return vendor + " " + model;
            }
            return model;
        }
        return fallback;
    }

    @Override
    public int getIfType() {
        return this.ifType;
    }

    @Override
    public boolean isConnectorPresent() {
        return this.connectorPresent;
    }

    @Override
    public String getIfAlias() {
        return ifAlias;
    }

    @Override
    public IfOperStatus getIfOperStatus() {
        return ifOperStatus;
    }

    @Override
    public boolean updateAttributes() {
        String name = SysPath.NET + getName();
        try {
            File ifDir = new File(name + "/statistics");
            if (!ifDir.isDirectory()) {
                return false;
            }
        } catch (SecurityException e) {
            return false;
        }

        this.timeStamp = System.currentTimeMillis();
        this.ifType = FileUtil.getIntFromFile(name + "/type");
        this.connectorPresent = FileUtil.getIntFromFile(name + "/carrier") > 0;
        this.bytesSent = FileUtil.getUnsignedLongFromFile(name + "/statistics/tx_bytes");
        this.bytesRecv = FileUtil.getUnsignedLongFromFile(name + "/statistics/rx_bytes");
        this.packetsSent = FileUtil.getUnsignedLongFromFile(name + "/statistics/tx_packets");
        this.packetsRecv = FileUtil.getUnsignedLongFromFile(name + "/statistics/rx_packets");
        this.outErrors = FileUtil.getUnsignedLongFromFile(name + "/statistics/tx_errors");
        this.inErrors = FileUtil.getUnsignedLongFromFile(name + "/statistics/rx_errors");
        this.collisions = FileUtil.getUnsignedLongFromFile(name + "/statistics/collisions");
        this.inDrops = FileUtil.getUnsignedLongFromFile(name + "/statistics/rx_dropped");
        long speedMbps = FileUtil.getUnsignedLongFromFile(name + "/speed");
        // speed may be -1 from file.
        this.speed = speedMbps < 0 ? 0 : speedMbps * 1000000L;
        this.ifAlias = FileUtil.getStringFromFile(name + "/ifalias");
        this.ifOperStatus = parseIfOperStatus(FileUtil.getStringFromFile(name + "/operstate"));

        return true;
    }

    /**
     * Parses the operational status from a sysfs operstate string.
     *
     * @param operState the operstate string
     * @return the parsed IfOperStatus
     */
    protected static IfOperStatus parseIfOperStatus(String operState) {
        switch (operState) {
            case "up":
                return IfOperStatus.UP;
            case "down":
                return IfOperStatus.DOWN;
            case "testing":
                return IfOperStatus.TESTING;
            case "dormant":
                return IfOperStatus.DORMANT;
            case "notpresent":
                return IfOperStatus.NOT_PRESENT;
            case "lowerlayerdown":
                return IfOperStatus.LOWER_LAYER_DOWN;
            case "unknown":
            default:
                return IfOperStatus.UNKNOWN;
        }
    }
}
