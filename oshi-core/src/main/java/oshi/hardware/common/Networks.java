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
package oshi.hardware.common;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.NetworkIF;

/**
 * Network interfaces implementation.
 */
@ThreadSafe
public class Networks {

    private static final Logger LOG = LoggerFactory.getLogger(Networks.class);

    /**
     * Gets the network interfaces on this machine
     * 
     * @return An array of {@link NetworkIF} objects representing the interfaces
     */
    public NetworkIF[] getNetworks() {
        List<NetworkIF> result = new ArrayList<>();

        for (NetworkInterface netint : getNetworkInterfaces()) {
            NetworkIF netIF = new NetworkIF();
            netIF.setNetworkInterface(netint);
            netIF.updateAttributes();
            result.add(netIF);
        }

        return result.toArray(new NetworkIF[0]);
    }

    /**
     * Returns network interfaces that are not Loopback, and have a hardware
     * address.
     *
     * @return An array of network interfaces
     */
    protected NetworkInterface[] getNetworkInterfaces() {
        List<NetworkInterface> result = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces = null;

        try {
            interfaces = NetworkInterface.getNetworkInterfaces(); // can return null
        } catch (SocketException ex) {
            LOG.error("Socket exception when retrieving interfaces: {}", ex.getMessage());
        }
        if (interfaces != null) {
            while (interfaces.hasMoreElements()) {
                NetworkInterface netint = interfaces.nextElement();
                try {
                    if (!netint.isLoopback() && netint.getHardwareAddress() != null) {
                        result.add(netint);
                    }
                } catch (SocketException ex) {
                    LOG.error("Socket exception when retrieving interface \"{}\": {}", netint.getName(),
                            ex.getMessage());
                }
            }
        }
        return result.toArray(new NetworkInterface[0]);
    }
}
