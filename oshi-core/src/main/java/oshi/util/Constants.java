/*
 * Copyright 2019-2024 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * General constants used in multiple classes
 */
@ThreadSafe
public final class Constants {

    /**
     * String to report for unknown information
     */
    public static final String UNKNOWN = "unknown";

    /**
     * The Unix Epoch, a default value when WMI DateTime queries return no value.
     */
    public static final OffsetDateTime UNIX_EPOCH = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);

    public static final Pattern DIGITS = Pattern.compile("\\d+");

    /**
     * Everything in this class is static, never instantiate it
     */
    private Constants() {
        throw new AssertionError();
    }
}
