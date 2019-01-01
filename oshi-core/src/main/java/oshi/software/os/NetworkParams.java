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
package oshi.software.os;

import java.io.Serializable;

/**
 * NetworkParams presents network parameters of running OS, such as DNS, host
 * name etc.
 */
public interface NetworkParams extends Serializable {

    /**
     * @return Gets host name
     */
    String getHostName();

    /**
     * @return Gets domain name
     */
    String getDomainName();

    /**
     * @return Gets DNS servers
     */
    String[] getDnsServers();

    /**
     * @return Gets default gateway(routing destination for 0.0.0.0/0) for IPv4,
     *         empty string if not defined.
     */
    String getIpv4DefaultGateway();

    /**
     * @return Gets default gateway(routing destination for ::/0) for IPv6,
     *         empty string if not defined.
     */
    String getIpv6DefaultGateway();
}
