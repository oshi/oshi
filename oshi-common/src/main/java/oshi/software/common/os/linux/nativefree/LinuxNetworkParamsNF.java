/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux.nativefree;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.linux.LinuxNetworkParams;

/**
 * Native-free Linux network parameters implementation. Extends {@link LinuxNetworkParams}, using the Java-based
 * hostname and domain name resolution from {@link oshi.software.common.AbstractNetworkParams}.
 */
@ThreadSafe
public class LinuxNetworkParamsNF extends LinuxNetworkParams {
}
