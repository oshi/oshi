/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.unix.freebsd;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.unix.LibC;
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
        LibC.Addrinfo hint = new LibC.Addrinfo();
        hint.ai_flags = LibC.AI_CANONNAME;
        String hostname = "";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.error("Unknown host exception when getting address of local host: " + e);
            return "";
        }
        PointerByReference ptr = new PointerByReference();
        int res = LibC.INSTANCE.getaddrinfo(hostname, null, hint, ptr);
        if (res > 0) {
            LOG.error("Failed getaddrinfo(): " + LibC.INSTANCE.gai_strerror(res));
            return "";
        }
        LibC.Addrinfo info = new LibC.Addrinfo(ptr.getValue());
        String canonname = new String(info.ai_canonname).trim();
        LibC.INSTANCE.freeaddrinfo(ptr.getValue());
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
