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
package oshi.software.os.unix.freebsd;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.ptr.PointerByReference; // NOSONAR

import oshi.jna.platform.unix.freebsd.Libc;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;

public class FreeBsdNetworkParams extends AbstractNetworkParams {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdNetworkParams.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDomainName() {
        Libc.Addrinfo hint = new Libc.Addrinfo();
        hint.ai_flags = Libc.AI_CANONNAME;
        String hostname = "";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.error("Unknown host exception when getting address of local host: {}", e);
            return "";
        }
        PointerByReference ptr = new PointerByReference();
        int res = Libc.INSTANCE.getaddrinfo(hostname, null, hint, ptr);
        if (res > 0) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed getaddrinfo(): {}", Libc.INSTANCE.gai_strerror(res));
            }
            return "";
        }
        Libc.Addrinfo info = new Libc.Addrinfo(ptr.getValue());
        String canonname = info.ai_canonname.trim();
        Libc.INSTANCE.freeaddrinfo(ptr.getValue());
        return canonname;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv4DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route -4 get default"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv6DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route -6 get default"));
    }
}