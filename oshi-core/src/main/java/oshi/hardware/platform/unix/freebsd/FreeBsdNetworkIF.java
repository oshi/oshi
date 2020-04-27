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
package oshi.hardware.platform.unix.freebsd;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * FreeBsdNetworks class.
 */
@ThreadSafe
public final class FreeBsdNetworkIF extends AbstractNetworkIF {

    private long bytesRecv;
    private long bytesSent;
    private long packetsRecv;
    private long packetsSent;
    private long inErrors;
    private long outErrors;
    private long inDrops;
    private long collisions;
    private long timeStamp;

    public FreeBsdNetworkIF(NetworkInterface netint) {
        super(netint);
        updateAttributes();
    }

    /**
     * Gets the network interfaces on this machine
     *
     * @return An {@code UnmodifiableList} of {@link NetworkIF} objects representing
     *         the interfaces
     */
    public static List<NetworkIF> getNetworks() {
        return Collections.unmodifiableList(
                getNetworkInterfaces().stream().map(FreeBsdNetworkIF::new).collect(Collectors.toList()));
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
        return 0;
    }

    @Override
    public long getTimeStamp() {
        return this.timeStamp;
    }

    @Override
    public boolean updateAttributes() {
        String stats = ExecutingCommand.getAnswerAt("netstat -bI " + getName(), 1);
        this.timeStamp = System.currentTimeMillis();
        String[] split = ParseUtil.whitespaces.split(stats);
        if (split.length < 12) {
            // No update
            return false;
        }
        this.bytesSent = ParseUtil.parseUnsignedLongOrDefault(split[10], 0L);
        this.bytesRecv = ParseUtil.parseUnsignedLongOrDefault(split[7], 0L);
        this.packetsSent = ParseUtil.parseUnsignedLongOrDefault(split[8], 0L);
        this.packetsRecv = ParseUtil.parseUnsignedLongOrDefault(split[4], 0L);
        this.outErrors = ParseUtil.parseUnsignedLongOrDefault(split[9], 0L);
        this.inErrors = ParseUtil.parseUnsignedLongOrDefault(split[5], 0L);
        this.collisions = ParseUtil.parseUnsignedLongOrDefault(split[11], 0L);
        this.inDrops = ParseUtil.parseUnsignedLongOrDefault(split[6], 0L);
        return true;
    }
}
