/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import java.util.List;

public abstract class AbstractInstalledApps {
    public abstract List<AppInfo> getInstalledApps();
}
