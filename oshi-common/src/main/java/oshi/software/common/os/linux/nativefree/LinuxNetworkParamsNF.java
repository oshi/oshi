/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux.nativefree;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.linux.LinuxNetworkParams;

/**
 * Native-free Linux network parameters implementation. Extends {@link LinuxNetworkParams}, inheriting its procfs host
 * name read and using the Java-based domain name resolution from {@link oshi.software.common.AbstractNetworkParams}.
 */
@ThreadSafe
public class LinuxNetworkParamsNF extends LinuxNetworkParams {
}
