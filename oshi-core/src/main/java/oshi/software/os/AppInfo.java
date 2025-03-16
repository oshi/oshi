/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import java.util.Map;

public interface AppInfo {
    String getName();

    String getVersion();

    String getVendor();

    long getLastModifiedEpoch();

    Map<String, String> getAdditionalInfo(); // For optional fields
}
