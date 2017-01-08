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
package oshi.software.common;

import com.sun.jna.ptr.PointerByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import oshi.jna.platform.unix.LibC;
import oshi.software.os.NetworkParams;
import oshi.util.FileUtil;

/**
 * Common NetworkParams implementation.
 */
public abstract class AbstractNetworkParams implements NetworkParams {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNetworkParams.class);
    private static final String NAMESERVER = "nameserver";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDomainName() {
        LibC.Addrinfo hint = new LibC.Addrinfo();
        hint.ai_flags = LibC.AI_CANONNAME;
        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.error("Unknown host exception when getting address of local host: " + e);
            return "";
        }
        PointerByReference ptr = new PointerByReference();
        doGetaddrinfo(hint, hostname, ptr);
        LibC.Addrinfo info = new LibC.Addrinfo(ptr.getValue());
        String canonname = info.ai_canonname;
        doFreeaddrinfo(ptr);
        int dot = canonname.indexOf('.');
        if (dot == -1) {
            return "";
        } else {
            return canonname.substring(dot + 1);
        }
    }

    protected void doFreeaddrinfo(PointerByReference ptr) {
        LibC.INSTANCE.freeaddrinfo(ptr.getValue());
    }

    protected void doGetaddrinfo(LibC.Addrinfo hint, String hostname, PointerByReference ptr) {
        LibC.INSTANCE.getaddrinfo(hostname, null, hint, ptr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHostName() {
        try {
            String hn = InetAddress.getLocalHost().getHostName();
            int dot = hn.indexOf('.');
            if (dot == -1) {
                return hn;
            } else {
                return hn.substring(0, dot);
            }
        } catch (UnknownHostException e) {
            LOG.error("Unknown host exception when getting address of local host: " + e);
            return "";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDnsServers() {
        List<String> resolv = FileUtil.readFile("/etc/resolv.conf");
        String key = NAMESERVER;
        int maxNameServer = 3;
        List<String> servers = new ArrayList<>();
        for (int i = 0; i < resolv.size() && servers.size() < maxNameServer; i++) {
            String line = resolv.get(i);
            if (line.startsWith(key)) {
                String value = line.substring(key.length()).replaceFirst("^[ \t]+", "");
                if (value.length() != 0 && value.charAt(0) != '#' && value.charAt(0) != ';') {
                    String val = value.split("[ \t#;]", 2)[0];
                    servers.add(val);
                }
            }
        }
        return servers.toArray(new String[servers.size()]);
    }

    static protected String searchGateway(List<String> lines) {
        for (String line : lines) {
            String leftTrimmed = line.replaceFirst("^[ \t]+", "");
            if (leftTrimmed.startsWith("gateway:")) {
                return leftTrimmed.split("[ \t]", 2)[1];
            }
        }
        return "";
    }
}
