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
package oshi.software.os.unix.openbsd;

import oshi.software.common.AbstractInternetProtocolStats;

public class OpenBsdInternetProtocolStats extends AbstractInternetProtocolStats {
    /**
     * Get the TCP stats for IPv4 connections.
     * <p>
     * On macOS connection information requires elevated permissions. Without
     * elevatd permissions, segment data is estimated.
     *
     * @return a {@link TcpStats} object encapsulating the stats.
     */
    @Override
    public TcpStats getTCPv4Stats() {
        return null;
    }

    /**
     * Get the TCP stats for IPv6 connections, if available. If not available
     * separately, these may be 0 and included in IPv4 connections.
     *
     * @return a {@link TcpStats} object encapsulating the stats.
     */
    @Override
    public TcpStats getTCPv6Stats() {
        return null;
    }

    /**
     * Get the UDP stats for IPv4 datagrams.
     *
     * @return a {@link UdpStats} object encapsulating the stats.
     */
    @Override
    public UdpStats getUDPv4Stats() {
        return null;
    }

    /**
     * Get the UDP stats for IPv6 datagrams, if available. If not available
     * separately, these may be 0 and included in IPv4 datagrams.
     *
     * @return a {@link UdpStats} object encapsulating the stats.
     */
    @Override
    public UdpStats getUDPv6Stats() {
        return null;
    }
}
