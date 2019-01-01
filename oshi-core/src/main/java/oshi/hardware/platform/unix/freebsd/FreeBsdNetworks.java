/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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

import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworks;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * @author widdis[at]gmail[dot]com
 */
public class FreeBsdNetworks extends AbstractNetworks {

    private static final long serialVersionUID = 1L;

    /**
     * Updates interface network statistics on the given interface. Statistics
     * include packets and bytes sent and received, and interface speed.
     *
     * @param netIF
     *            The interface on which to update statistics
     */
    public static void updateNetworkStats(NetworkIF netIF) {
        String stats = ExecutingCommand.getAnswerAt("netstat -bI " + netIF.getName(), 1);
        netIF.setTimeStamp(System.currentTimeMillis());
        String[] split = ParseUtil.whitespaces.split(stats);
        if (split.length < 12) {
            // No update
            return;
        }
        netIF.setBytesSent(ParseUtil.parseUnsignedLongOrDefault(split[10], 0L));
        netIF.setBytesRecv(ParseUtil.parseUnsignedLongOrDefault(split[7], 0L));
        netIF.setPacketsSent(ParseUtil.parseUnsignedLongOrDefault(split[8], 0L));
        netIF.setPacketsRecv(ParseUtil.parseUnsignedLongOrDefault(split[4], 0L));
        netIF.setOutErrors(ParseUtil.parseUnsignedLongOrDefault(split[9], 0L));
        netIF.setInErrors(ParseUtil.parseUnsignedLongOrDefault(split[5], 0L));
    }
}
