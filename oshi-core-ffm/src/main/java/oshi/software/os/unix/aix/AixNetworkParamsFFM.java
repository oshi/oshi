/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.aix;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.aix.AixLibcFunctions;
import oshi.software.common.os.unix.aix.AixNetworkParams;

/**
 * FFM-backed AIX NetworkParams.
 */
@ThreadSafe
final class AixNetworkParamsFFM extends AixNetworkParams {

    @Override
    public String getHostName() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(HOST_NAME_BUF_SIZE);
            if (AixLibcFunctions.gethostname(buf, HOST_NAME_BUF_SIZE) != 0) {
                return super.getHostName();
            }
            int len = 0;
            while (len < HOST_NAME_BUF_SIZE && buf.get(JAVA_BYTE, len) != 0) {
                len++;
            }
            byte[] bytes = new byte[len];
            MemorySegment.copy(buf, JAVA_BYTE, 0, bytes, 0, len);
            return new String(bytes, StandardCharsets.US_ASCII);
        } catch (Throwable t) {
            return super.getHostName();
        }
    }
}
