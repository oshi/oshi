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
package oshi.hardware.platform.unix.solaris;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat; // NOSONAR

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;

/**
 * SolarisNetworks class.
 */
@ThreadSafe
public final class SolarisNetworkIF extends AbstractNetworkIF {

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

    public SolarisNetworkIF(NetworkInterface netint) {
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
                getNetworkInterfaces().stream().map(SolarisNetworkIF::new).collect(Collectors.toList()));
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
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = kc.lookup("link", -1, getName());
            if (ksp == null) { // Solaris 10 compatibility
                ksp = kc.lookup(null, -1, getName());
            }
            if (ksp != null && kc.read(ksp)) {
                this.bytesSent = KstatUtil.dataLookupLong(ksp, "obytes64");
                this.bytesRecv = KstatUtil.dataLookupLong(ksp, "rbytes64");
                this.packetsSent = KstatUtil.dataLookupLong(ksp, "opackets64");
                this.packetsRecv = KstatUtil.dataLookupLong(ksp, "ipackets64");
                this.outErrors = KstatUtil.dataLookupLong(ksp, "oerrors");
                this.inErrors = KstatUtil.dataLookupLong(ksp, "ierrors");
                this.collisions = KstatUtil.dataLookupLong(ksp, "collisions");
                this.inDrops = KstatUtil.dataLookupLong(ksp, "dl_idrops");
                this.speed = KstatUtil.dataLookupLong(ksp, "ifspeed");
                // Snap time in ns; convert to ms
                this.timeStamp = ksp.ks_snaptime / 1_000_000L;
                return true;
            }
        }
        return false;
    }
}
