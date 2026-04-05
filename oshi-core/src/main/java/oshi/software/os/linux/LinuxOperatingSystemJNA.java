/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * JNA-based Linux operating system implementation. Extends {@link LinuxOperatingSystem}, overriding methods as FFM
 * equivalents are migrated to {@link LinuxOperatingSystemFFM}.
 */
@ThreadSafe
public final class LinuxOperatingSystemJNA extends LinuxOperatingSystem {
}
