/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.linux;

import java.io.File;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

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
    private String ifAlias;
    private IfOperStatus ifOperStatus;

    public LinuxNetworkIF(NetworkInterface netint) throws InstantiationException {
        super(netint, queryIfModel(netint));
        updateAttributes();
    }

    private static String queryIfModel(NetworkInterface netint) {
        String name = netint.getName();
        UdevContext udev = Udev.INSTANCE.udev_new();
        if (udev != null) {
            try {
                UdevDevice device = udev.deviceNewFromSyspath("/sys/class/net/" + name);
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

    /**
     * Gets network interfaces on this machine
     *
     * @param includeLocalInterfaces
     *            include local interfaces in the result
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
        try {
            File ifDir = new File(String.format("/sys/class/net/%s/statistics", getName()));
            if (!ifDir.isDirectory()) {
                return false;
            }
        } catch (SecurityException e) {
            return false;
        }
        String ifTypePath = String.format("/sys/class/net/%s/type", getName());
        String carrierPath = String.format("/sys/class/net/%s/carrier", getName());
        String txBytesPath = String.format("/sys/class/net/%s/statistics/tx_bytes", getName());
        String rxBytesPath = String.format("/sys/class/net/%s/statistics/rx_bytes", getName());
        String txPacketsPath = String.format("/sys/class/net/%s/statistics/tx_packets", getName());
        String rxPacketsPath = String.format("/sys/class/net/%s/statistics/rx_packets", getName());
        String txErrorsPath = String.format("/sys/class/net/%s/statistics/tx_errors", getName());
        String rxErrorsPath = String.format("/sys/class/net/%s/statistics/rx_errors", getName());
        String collisionsPath = String.format("/sys/class/net/%s/statistics/collisions", getName());
        String rxDropsPath = String.format("/sys/class/net/%s/statistics/rx_dropped", getName());
        String ifSpeed = String.format("/sys/class/net/%s/speed", getName());
        String ifAliasPath = String.format("/sys/class/net/%s/ifalias", getName());
        String ifOperStatusPath = String.format("/sys/class/net/%s/operstate", getName());

        this.timeStamp = System.currentTimeMillis();
        this.ifType = FileUtil.getIntFromFile(ifTypePath);
        this.connectorPresent = FileUtil.getIntFromFile(carrierPath) > 0;
        this.bytesSent = FileUtil.getUnsignedLongFromFile(txBytesPath);
        this.bytesRecv = FileUtil.getUnsignedLongFromFile(rxBytesPath);
        this.packetsSent = FileUtil.getUnsignedLongFromFile(txPacketsPath);
        this.packetsRecv = FileUtil.getUnsignedLongFromFile(rxPacketsPath);
        this.outErrors = FileUtil.getUnsignedLongFromFile(txErrorsPath);
        this.inErrors = FileUtil.getUnsignedLongFromFile(rxErrorsPath);
        this.collisions = FileUtil.getUnsignedLongFromFile(collisionsPath);
        this.inDrops = FileUtil.getUnsignedLongFromFile(rxDropsPath);
        long speedMiB = FileUtil.getUnsignedLongFromFile(ifSpeed);
        // speed may be -1 from file.
        this.speed = speedMiB < 0 ? 0 : speedMiB << 20;
        this.ifAlias = FileUtil.getStringFromFile(ifAliasPath);
        this.ifOperStatus = parseIfOperStatus(FileUtil.getStringFromFile(ifOperStatusPath));

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
