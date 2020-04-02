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

import javax.annotation.concurrent.ThreadSafe;

import oshi.hardware.NetworkIF;
import oshi.hardware.common.Networks;
import oshi.util.FileUtil;

/**
 * LinuxNetworks class.
 */
@ThreadSafe
public final class LinuxNetworks extends Networks {
    /**
     * Updates interface network statistics on the given interface. Statistics
     * include packets and bytes sent and received, and interface speed.
     *
     * @param netIF
     *            The interface on which to update statistics
     * @return {@code true} if the update was successful, {@code false} otherwise.
     */
    public static boolean updateNetworkStats(NetworkIF netIF) {
        try {
            File ifDir = new File(String.format("/sys/class/net/%s/statistics", netIF.getName()));
            if (!ifDir.isDirectory()) {
                return false;
            }
        } catch (SecurityException e) {
            return false;
        }
        String ifTypePath = String.format("/sys/class/net/%s/type", netIF.getName());
        String carrierPath = String.format("/sys/class/net/%s/carrier", netIF.getName());
        String txBytesPath = String.format("/sys/class/net/%s/statistics/tx_bytes", netIF.getName());
        String rxBytesPath = String.format("/sys/class/net/%s/statistics/rx_bytes", netIF.getName());
        String txPacketsPath = String.format("/sys/class/net/%s/statistics/tx_packets", netIF.getName());
        String rxPacketsPath = String.format("/sys/class/net/%s/statistics/rx_packets", netIF.getName());
        String txErrorsPath = String.format("/sys/class/net/%s/statistics/tx_errors", netIF.getName());
        String rxErrorsPath = String.format("/sys/class/net/%s/statistics/rx_errors", netIF.getName());
        String collisionsPath = String.format("/sys/class/net/%s/statistics/collisions", netIF.getName());
        String rxDropsPath = String.format("/sys/class/net/%s/statistics/rx_dropped", netIF.getName());
        String speed = String.format("/sys/class/net/%s/speed", netIF.getName());

        netIF.setTimeStamp(System.currentTimeMillis());
        netIF.setIfType(FileUtil.getIntFromFile(ifTypePath));
        netIF.setConnectorPresent(FileUtil.getIntFromFile(carrierPath) > 0);
        netIF.setBytesSent(FileUtil.getUnsignedLongFromFile(txBytesPath));
        netIF.setBytesRecv(FileUtil.getUnsignedLongFromFile(rxBytesPath));
        netIF.setPacketsSent(FileUtil.getUnsignedLongFromFile(txPacketsPath));
        netIF.setPacketsRecv(FileUtil.getUnsignedLongFromFile(rxPacketsPath));
        netIF.setOutErrors(FileUtil.getUnsignedLongFromFile(txErrorsPath));
        netIF.setInErrors(FileUtil.getUnsignedLongFromFile(rxErrorsPath));
        netIF.setCollisions(FileUtil.getUnsignedLongFromFile(collisionsPath));
        netIF.setInDrops(FileUtil.getUnsignedLongFromFile(rxDropsPath));
        long netSpeed = FileUtil.getUnsignedLongFromFile(speed) * 1024 * 1024;
        netIF.setSpeed(netSpeed < 0 ? 0 : netSpeed);
        return true;
    }
}
