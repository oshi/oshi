/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import static com.sun.jna.platform.unix.LibCAPI.HOST_NAME_MAX;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.linux.LibC;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseablePointerByReference;
import oshi.jna.platform.linux.LinuxLibc;
import oshi.jna.platform.unix.CLibrary;
import oshi.jna.platform.unix.CLibrary.Addrinfo;

/**
 * JNA-based Linux network parameters. Implements {@code getDomainName} via {@code getaddrinfo} and {@code getHostName}
 * via {@code gethostname}.
 */
@ThreadSafe
class LinuxNetworkParamsJNA extends LinuxNetworkParams {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxNetworkParamsJNA.class);

    private static final LinuxLibc LIBC = LinuxLibc.INSTANCE;

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
                if (res != 0) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Failed getaddrinfo(): {}", LIBC.gai_strerror(res));
                    }
                    return "";
                }
                Addrinfo info = new Addrinfo(ptr.getValue());
                try {
                    return info.ai_canonname == null ? hostname : info.ai_canonname.trim();
                } finally {
                    LIBC.freeaddrinfo(ptr.getValue());
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
}
