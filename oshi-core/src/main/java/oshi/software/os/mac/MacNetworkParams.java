/*
 * Copyright 2017-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import static com.sun.jna.platform.unix.LibCAPI.HOST_NAME_MAX;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseablePointerByReference;
import oshi.jna.platform.mac.SystemB;
import oshi.jna.platform.unix.CLibrary;
import oshi.jna.platform.unix.CLibrary.Addrinfo;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * MacNetworkParams class.
 */
@ThreadSafe
final class MacNetworkParams extends AbstractNetworkParams {

    private static final Logger LOG = LoggerFactory.getLogger(MacNetworkParams.class);

    private static final SystemB SYS = SystemB.INSTANCE;

    private static final String IPV6_ROUTE_HEADER = "Internet6:";

    private static final String DEFAULT_GATEWAY = "default";

    @Override
    public String getDomainName() {
        String hostname = "";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.error("Unknown host exception when getting address of local host: {}", e.getMessage());
            return "";
        }
        try (Addrinfo hint = new Addrinfo(); CloseablePointerByReference ptr = new CloseablePointerByReference()) {
            hint.ai_flags = CLibrary.AI_CANONNAME;
            int res = SYS.getaddrinfo(hostname, null, hint, ptr);
            if (res > 0) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed getaddrinfo(): {}", SYS.gai_strerror(res));
                }
                return "";
            }
            Addrinfo info = new Addrinfo(ptr.getValue()); // NOSONAR
            String canonname = info.ai_canonname.trim();
            SYS.freeaddrinfo(ptr.getValue());
            return canonname;
        }
    }

    @Override
    public String getHostName() {
        byte[] hostnameBuffer = new byte[HOST_NAME_MAX + 1];
        if (0 != SYS.gethostname(hostnameBuffer, hostnameBuffer.length)) {
            return super.getHostName();
        }
        return Native.toString(hostnameBuffer);
    }

    @Override
    public String getIpv4DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route -n get default"));
    }

    @Override
    public String getIpv6DefaultGateway() {
        List<String> lines = ExecutingCommand.runNative("netstat -nr");
        boolean v6Table = false;
        for (String line : lines) {
            if (v6Table && line.startsWith(DEFAULT_GATEWAY)) {
                String[] fields = ParseUtil.whitespaces.split(line);
                if (fields.length > 2 && fields[2].contains("G")) {
                    return fields[1].split("%")[0];
                }
            } else if (line.startsWith(IPV6_ROUTE_HEADER)) {
                v6Table = true;
            }
        }
        return "";
    }
}
