/*
 * Copyright 2017-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.freebsd;

import static com.sun.jna.platform.unix.LibCAPI.HOST_NAME_MAX;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseablePointerByReference;
import oshi.jna.platform.unix.CLibrary;
import oshi.jna.platform.unix.CLibrary.Addrinfo;
import oshi.jna.platform.unix.FreeBsdLibc;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;

/**
 * FreeBsdNetworkParams class.
 */
@ThreadSafe
final class FreeBsdNetworkParams extends AbstractNetworkParams {

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdNetworkParams.class);

    private static final FreeBsdLibc LIBC = FreeBsdLibc.INSTANCE;

    @Override
    public String getDomainName() {
        try (Addrinfo hint = new Addrinfo()) {
            hint.ai_flags = CLibrary.AI_CANONNAME;
            String hostname = getHostName();

            try (CloseablePointerByReference ptr = new CloseablePointerByReference()) {
                int res = LIBC.getaddrinfo(hostname, null, hint, ptr);
                if (res > 0) {
                    if (LOG.isErrorEnabled()) {
                        LOG.warn("Failed getaddrinfo(): {}", LIBC.gai_strerror(res));
                    }
                    return "";
                }
                Addrinfo info = new Addrinfo(ptr.getValue()); // NOSONAR
                String canonname = info.ai_canonname.trim();
                LIBC.freeaddrinfo(ptr.getValue());
                return canonname;
            }
        }
    }

    @Override
    public String getHostName() {
        byte[] hostnameBuffer = new byte[HOST_NAME_MAX + 1];
        if (0 != LIBC.gethostname(hostnameBuffer, hostnameBuffer.length)) {
            return super.getHostName();
        }
        return Native.toString(hostnameBuffer);
    }

    @Override
    public String getIpv4DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route -4 get default"));
    }

    @Override
    public String getIpv6DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route -6 get default"));
    }
}
