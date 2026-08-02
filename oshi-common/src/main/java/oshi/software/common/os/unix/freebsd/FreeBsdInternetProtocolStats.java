/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

import oshi.software.common.AbstractInternetProtocolStats;

/**
 * Abstract base for the FreeBSD InternetProtocolStats. Parses the TCP and UDP counters from {@code netstat} output; the
 * JNA and FFM subclasses supply the per-connection reads.
 */
public abstract class FreeBsdInternetProtocolStats extends AbstractInternetProtocolStats {
}
