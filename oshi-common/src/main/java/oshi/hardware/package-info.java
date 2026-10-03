/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
/**
 * [oshi-common API] Provides cross-platform implementation to retrieve hardware information such as CPU, Memory,
 * Display, Disks, Network Interfaces, Power Sources, Sensors, and USB Devices
 * <p>
 * This package is {@code @NullMarked}: every type usage in a signature is non-null unless explicitly annotated
 * {@code @Nullable}. Rather than returning {@code null}, a getter reports a value it could not read as a sentinel:
 * <ul>
 * <li>a string is {@link Constants#UNKNOWN} or, for some free-text values, the empty string;</li>
 * <li>a collection, map or array is empty;</li>
 * <li>a number is a value outside its legitimate range: {@code 0} where zero cannot be a real value, such as a size or
 * a frequency, {@code -1} where it can, such as a count, and {@link Double#NaN} for some floating-point
 * measurements.</li>
 * </ul>
 * Where a getter uses a more specific sentinel, its own Javadoc names it. See the
 * <a href="https://github.com/oshi/oshi/blob/master/FAQ.md#what-do-oshis-annotations-mean">FAQ</a>.
 */
@NullMarked
package oshi.hardware;

import org.jspecify.annotations.NullMarked;

import oshi.util.Constants;
