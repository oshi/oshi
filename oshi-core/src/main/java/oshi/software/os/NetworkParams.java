/*
 * MIT License
 *
 * Copyright (c) 2019-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.software.os;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * NetworkParams presents network parameters of running OS, such as DNS, host
 * name etc.
 */
@ThreadSafe
public interface NetworkParams {

    /**
     * Gets the HostName of the machine executing OSHI.
     *
     * @return the hostname
     */
    String getHostName();

    /**
     * Gets the Domain Name of the machine executing OSHI.
     *
     * @return the domain name
     */
    String getDomainName();

    /**
     * Gets the DNS Servers configured for this machine.
     *
     * @return the DNS servers
     */
    String[] getDnsServers();

    /**
     * Gets the default gateway(routing destination for 0.0.0.0/0) for IPv4
     * connections.
     *
     * @return default gateway for IPv4, or empty string if not defined.
     */
    String getIpv4DefaultGateway();

    /**
     * Gets default gateway(routing destination for ::/0) for IPv6 connections.
     *
     * @return default gateway for IPv6, or empty string if not defined.
     */
    String getIpv6DefaultGateway();
}
