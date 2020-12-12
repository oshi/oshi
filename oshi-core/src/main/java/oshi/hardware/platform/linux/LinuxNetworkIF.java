/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;
import oshi.util.FileUtil;

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

    public LinuxNetworkIF(NetworkInterface netint) throws InstantiationException {
        super(netint);
        updateAttributes();
    }

    /**
     * Gets network interfaces on this machine
     *
     * @param includeLocalInterfaces
     *            include local interfaces in the result
     * @return An {@code UnmodifiableList} of {@link NetworkIF} objects representing
     *         the interfaces
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
        return Collections.unmodifiableList(ifList);
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

        return true;
    }
}
