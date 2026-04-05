/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import java.io.File;
import java.net.NetworkInterface;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractNetworkIF;
import oshi.util.FileUtil;
import oshi.util.Util;
import oshi.util.platform.linux.SysPath;

/**
 * LinuxNetworks class.
 */
@ThreadSafe
public abstract class LinuxNetworkIF extends AbstractNetworkIF {

    private int ifType;
    private boolean connectorPresent;
    private long bytesRecv;
    private long bytesSent;
    private long packetsRecv;
    private long packetsSent;
    private long inErrors;
    private long outErrors;
    private long inDrops;
    private long collisions;
    private long speed;
    private long timeStamp;
    private String ifAlias = "";
    private IfOperStatus ifOperStatus = IfOperStatus.UNKNOWN;

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
        Map<String, String> uevent = FileUtil.getKeyValueMapFromFile(SysPath.NET + name + "/uevent", "=");
        String devVendor = uevent.get("ID_VENDOR_FROM_DATABASE");
        String devModel = uevent.get("ID_MODEL_FROM_DATABASE");
        if (!Util.isBlank(devModel)) {
            if (!Util.isBlank(devVendor)) {
                return devVendor + " " + devModel;
            }
            return devModel;
        }
        return name;
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
    public long getBytesRecv() {
        return this.bytesRecv;
    }

    @Override
    public long getBytesSent() {
        return this.bytesSent;
    }

    @Override
    public long getPacketsRecv() {
        return this.packetsRecv;
    }

    @Override
    public long getPacketsSent() {
        return this.packetsSent;
    }

    @Override
    public long getInErrors() {
        return this.inErrors;
    }

    @Override
    public long getOutErrors() {
        return this.outErrors;
    }

    @Override
    public long getInDrops() {
        return this.inDrops;
    }

    @Override
    public long getCollisions() {
        return this.collisions;
    }

    @Override
    public long getSpeed() {
        return this.speed;
    }

    @Override
    public long getTimeStamp() {
        return this.timeStamp;
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
