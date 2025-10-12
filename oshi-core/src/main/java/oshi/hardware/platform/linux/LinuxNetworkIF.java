/*
 * Copyright 2020-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.software.os.linux.LinuxOperatingSystem.HAS_UDEV;

import java.io.File;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.linux.Udev;
import com.sun.jna.platform.linux.Udev.UdevContext;
import com.sun.jna.platform.linux.Udev.UdevDevice;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;
import oshi.util.FileUtil;
import oshi.util.Util;
import oshi.util.platform.linux.SysPath;

/**
 * LinuxNetworks class.
 */
@ThreadSafe
public final class LinuxNetworkIF extends AbstractNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxNetworkIF.class);

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

    public LinuxNetworkIF(NetworkInterface netint) throws InstantiationException {
        super(netint, queryIfModel(netint));
        updateAttributes();
    }

    private static String queryIfModel(NetworkInterface netint) {
        String name = netint.getName();
        if (!HAS_UDEV) {
            return queryIfModelFromSysfs(name);
        }
        UdevContext udev = Udev.INSTANCE.udev_new();
        if (udev != null) {
            try {
                UdevDevice device = udev.deviceNewFromSyspath(SysPath.NET + name);
                if (device != null) {
                    try {
                        String devVendor = device.getPropertyValue("ID_VENDOR_FROM_DATABASE");
                        String devModel = device.getPropertyValue("ID_MODEL_FROM_DATABASE");
                        if (!Util.isBlank(devModel)) {
                            if (!Util.isBlank(devVendor)) {
                                return devVendor + " " + devModel;
                            }
                            return devModel;
                        }
                    } finally {
                        device.unref();
                    }
                }
            } finally {
                udev.unref();
            }
        }
        return name;
    }

    private static String queryIfModelFromSysfs(String name) {
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

    /**
     * Gets network interfaces on this machine
     *
     * @param includeLocalInterfaces include local interfaces in the result
     * @return A list of {@link NetworkIF} objects representing the interfaces
     */
    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        List<NetworkIF> ifList = new ArrayList<>();
        for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
            try {
                ifList.add(new LinuxNetworkIF(ni));
            } catch (InstantiationException e) {
                LOG.debug("Network Interface Instantiation failed: {}", e.getMessage());
            }
        }
        return ifList;
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

    private static IfOperStatus parseIfOperStatus(String operState) {
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
