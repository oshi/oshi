/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import static com.sun.jna.platform.unix.LibCAPI.HOST_NAME_MAX;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseablePointerByReference;
import oshi.jna.platform.mac.SystemB;
import oshi.jna.platform.unix.CLibrary;
import oshi.jna.platform.unix.CLibrary.Addrinfo;
import oshi.software.common.os.mac.MacNetworkParams;

/**
 * MacNetworkParamsJNA class.
 */
@ThreadSafe
final class MacNetworkParamsJNA extends MacNetworkParams {

    private static final Logger LOG = LoggerFactory.getLogger(MacNetworkParamsJNA.class);

    private static final SystemB SYS = SystemB.INSTANCE;

    @Override
    public String getDomainName() {
        String hostname = "";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
            if (hostname == null || hostname.isEmpty()) {
                LOG.debug("Could not determine hostname");
                return "";
            }
        } catch (UnknownHostException e) {
            LOG.debug("Unknown host exception when getting address of local host: {}", e.getMessage());
            return "";
        }
        try (Addrinfo hint = new Addrinfo(); CloseablePointerByReference ptr = new CloseablePointerByReference()) {
            hint.ai_flags = CLibrary.AI_CANONNAME;
            int res = SYS.getaddrinfo(hostname, null, hint, ptr);
            if (res != 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Failed getaddrinfo(): {}", SYS.gai_strerror(res));
                }
                return "";
            }
            try (Addrinfo info = new Addrinfo(ptr.getValue())) { // NOSONAR
                String canonname = info.ai_canonname.trim();
                SYS.freeaddrinfo(ptr.getValue());
                return canonname;
            }
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
}
