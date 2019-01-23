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
package oshi.hardware.common;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.NetworkIF;
import oshi.hardware.Networks;

/**
 * Network interfaces implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public abstract class AbstractNetworks implements Networks {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNetworks.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkIF[] getNetworks() {
        List<NetworkIF> result = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces;

        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            LOG.error("Socket exception when retrieving interfaces: {}", ex);
            return new NetworkIF[0];
        }

        for (NetworkInterface netint : Collections.list(interfaces)) {
            try {
                if (!netint.isLoopback() && netint.getHardwareAddress() != null) {
                    NetworkIF netIF = new NetworkIF();
                    netIF.setNetworkInterface(netint);
                    netIF.updateNetworkStats();
                    result.add(netIF);
                }
            } catch (SocketException ex) {
                LOG.error("Socket exception when retrieving interface \"{}\": {}", netint.getName(), ex);
            }
        }

        return result.toArray(new NetworkIF[0]);
    }
}
