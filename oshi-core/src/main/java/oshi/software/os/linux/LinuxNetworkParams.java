/*
 * Copyright 2017-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import static com.sun.jna.platform.unix.LibCAPI.HOST_NAME_MAX;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.linux.LibC;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseablePointerByReference;
import oshi.jna.platform.linux.LinuxLibc;
import oshi.jna.platform.unix.CLibrary;
import oshi.jna.platform.unix.CLibrary.Addrinfo;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * LinuxNetworkParams class.
 */
@ThreadSafe
final class LinuxNetworkParams extends AbstractNetworkParams {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxNetworkParams.class);

    private static final LinuxLibc LIBC = LinuxLibc.INSTANCE;

    private static final String IPV4_DEFAULT_DEST = "0.0.0.0"; // NOSONAR
    private static final String IPV6_DEFAULT_DEST = "::/0";

    @Override
    public String getDomainName() {
        try (Addrinfo hint = new Addrinfo()) {
            hint.ai_flags = CLibrary.AI_CANONNAME;
            String hostname = "";
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                LOG.warn("Unknown host exception when getting address of local host: {}", e.getMessage());
                return "";
            }
            try (CloseablePointerByReference ptr = new CloseablePointerByReference()) {
                int res = LIBC.getaddrinfo(hostname, null, hint, ptr);
                if (res > 0) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Failed getaddrinfo(): {}", LIBC.gai_strerror(res));
                    }
                    return "";
                }
                try (Addrinfo info = new Addrinfo(ptr.getValue())) {
                    return info.ai_canonname == null ? hostname : info.ai_canonname.trim();
                }
            }
        }
    }

    @Override
    public String getHostName() {
        byte[] hostnameBuffer = new byte[HOST_NAME_MAX + 1];
        if (0 != LibC.INSTANCE.gethostname(hostnameBuffer, hostnameBuffer.length)) {
            return super.getHostName();
        }
        return Native.toString(hostnameBuffer);
    }

    @Override
    public String getIpv4DefaultGateway() {
        List<String> routes = ExecutingCommand.runNative("route -A inet -n");
        if (routes.size() <= 2) {
            return "";
        }

        String gateway = "";
        int minMetric = Integer.MAX_VALUE;

        for (int i = 2; i < routes.size(); i++) {
            String[] fields = ParseUtil.whitespaces.split(routes.get(i));
            if (fields.length > 4 && fields[0].equals(IPV4_DEFAULT_DEST)) {
                boolean isGateway = fields[3].indexOf('G') != -1;
                int metric = ParseUtil.parseIntOrDefault(fields[4], Integer.MAX_VALUE);
                if (isGateway && metric < minMetric) {
                    minMetric = metric;
                    gateway = fields[1];
                }
            }
        }
        return gateway;
    }

    @Override
    public String getIpv6DefaultGateway() {
        List<String> routes = ExecutingCommand.runNative("route -A inet6 -n");
        if (routes.size() <= 2) {
            return "";
        }

        String gateway = "";
        int minMetric = Integer.MAX_VALUE;

        for (int i = 2; i < routes.size(); i++) {
            String[] fields = ParseUtil.whitespaces.split(routes.get(i));
            if (fields.length > 3 && fields[0].equals(IPV6_DEFAULT_DEST)) {
                boolean isGateway = fields[2].indexOf('G') != -1;
                int metric = ParseUtil.parseIntOrDefault(fields[3], Integer.MAX_VALUE);
                if (isGateway && metric < minMetric) {
                    minMetric = metric;
                    gateway = fields[1];
                }
            }
        }
        return gateway;
    }
}
