/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
/**
 * [oshi-common API] Provides cross-platform implementation to retrieve hardware information such as CPU, Memory,
 * Display, Disks, Network Interfaces, Power Sources, Sensors, and USB Devices
 * <p>
 * This package is {@code @NullMarked}: every type usage in a signature is non-null unless explicitly annotated
 * {@code @Nullable}. Rather than returning {@code null}, values that could not be read are reported as
 * {@code Constants.UNKNOWN} or an empty string, an empty collection, or {@code 0}/{@code -1}/{@code NaN}. See the
 * <a href="https://github.com/oshi/oshi/blob/master/FAQ.md#what-do-oshis-annotations-mean">FAQ</a>.
 */
@NullMarked
package oshi.hardware;

import org.jspecify.annotations.NullMarked;
