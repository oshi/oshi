/*
 * Copyright 2017-2026 The OSHI Project Contributors
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
import oshi.software.common.os.unix.freebsd.FreeBsdNetworkParams;

/**
 * JNA-backed FreeBSD network params. Command-line gateway lookup and the {@code getHostName} fallback live on
 * {@link FreeBsdNetworkParams}; only the libc bindings ({@code getaddrinfo}, {@code gethostname}) are JNA-specific.
 */
@ThreadSafe
public class FreeBsdNetworkParamsJNA extends FreeBsdNetworkParams {

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdNetworkParamsJNA.class);

    private static final FreeBsdLibc LIBC = FreeBsdLibc.INSTANCE;

    @Override
    protected String queryDomainName() {
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
                try (Addrinfo info = new Addrinfo(ptr.getValue())) { // NOSONAR
                    String canonname = info.ai_canonname == null ? "" : info.ai_canonname.trim();
                    LIBC.freeaddrinfo(ptr.getValue());
                    return canonname;
                }
            }
        }
    }

    @Override
    protected String queryHostName() {
        byte[] hostnameBuffer = new byte[HOST_NAME_MAX + 1];
        if (0 != LIBC.gethostname(hostnameBuffer, hostnameBuffer.length)) {
            return null;
        }
        return Native.toString(hostnameBuffer);
    }
}
