/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

import java.util.Locale;

/**
 * An enumeration of supported operating systems. The order of declaration matches the osType constants in the JNA
 * Platform class.
 *
 * @deprecated Use {@link oshi.util.PlatformEnum} instead.
 */
@Deprecated
public enum PlatformEnum {

    /**
     * macOS
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#MACOS}
     */
    @Deprecated
    MACOS("macOS"),
    /**
     * A flavor of Linux
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#LINUX}
     */
    @Deprecated
    LINUX("Linux"),
    /**
     * Microsoft Windows
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#WINDOWS}
     */
    @Deprecated
    WINDOWS("Windows"),
    /**
     * Solaris (SunOS)
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#SOLARIS}
     */
    @Deprecated
    SOLARIS("Solaris"),
    /**
     * FreeBSD
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#FREEBSD}
     */
    @Deprecated
    FREEBSD("FreeBSD"),
    /**
     * OpenBSD
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#OPENBSD}
     */
    @Deprecated
    OPENBSD("OpenBSD"),
    /**
     * Windows Embedded Compact
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#WINDOWSCE}
     */
    @Deprecated
    WINDOWSCE("Windows CE"),
    /**
     * IBM AIX
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#AIX}
     */
    @Deprecated
    AIX("AIX"),
    /**
     * Android
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#ANDROID}
     */
    @Deprecated
    ANDROID("Android"),
    /**
     * GNU operating system
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#GNU}
     */
    @Deprecated
    GNU("GNU"),
    /**
     * Debian GNU/kFreeBSD
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#KFREEBSD}
     */
    @Deprecated
    KFREEBSD("kFreeBSD"),
    /**
     * NetBSD
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#NETBSD}
     */
    @Deprecated
    NETBSD("NetBSD"),
    /**
     * DragonFly BSD
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#DRAGONFLYBSD}
     */
    @Deprecated
    DRAGONFLYBSD("DragonFly BSD"),
    /**
     * An unspecified system
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#UNKNOWN}
     */
    @Deprecated
    UNKNOWN("Unknown");

    private static final PlatformEnum CURRENT_PLATFORM;

    static {
        String osName = System.getProperty("os.name", "");
        if (osName.startsWith("Linux")) {
            String vmName = System.getProperty("java.vm.name", "").toLowerCase(Locale.ROOT);
            CURRENT_PLATFORM = vmName.equals("dalvik") ? ANDROID : LINUX;
        } else if (osName.startsWith("AIX")) {
            CURRENT_PLATFORM = AIX;
        } else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            CURRENT_PLATFORM = MACOS;
        } else if (osName.startsWith("Windows CE")) {
            CURRENT_PLATFORM = WINDOWSCE;
        } else if (osName.startsWith("Windows")) {
            CURRENT_PLATFORM = WINDOWS;
        } else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
            CURRENT_PLATFORM = SOLARIS;
        } else if (osName.startsWith("FreeBSD")) {
            CURRENT_PLATFORM = FREEBSD;
        } else if (osName.startsWith("OpenBSD")) {
            CURRENT_PLATFORM = OPENBSD;
        } else if (osName.equalsIgnoreCase("gnu")) {
            CURRENT_PLATFORM = GNU;
        } else if (osName.equalsIgnoreCase("gnu/kfreebsd")) {
            CURRENT_PLATFORM = KFREEBSD;
        } else if (osName.equalsIgnoreCase("netbsd")) {
            CURRENT_PLATFORM = NETBSD;
        } else if (osName.equalsIgnoreCase("dragonflybsd")) {
            CURRENT_PLATFORM = DRAGONFLYBSD;
        } else {
            CURRENT_PLATFORM = UNKNOWN;
        }
    }

    private final String name;

    PlatformEnum(String name) {
        this.name = name;
    }

    /**
     * Gets the friendly name of the platform
     *
     * @return the friendly name of the platform
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#getName()}
     */
    @Deprecated
    public String getName() {
        return this.name;
    }

    /**
     * Gets the friendly name of the platform corresponding to the specified JNA osType ordinal.
     *
     * @param osType The ordinal value matching the order of constants in this enum, corresponding to JNA's
     *               {@code Platform.getOSType()} return values.
     * @return the friendly name of the specified platform type
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#getName(int)}
     */
    @Deprecated
    public static String getName(int osType) {
        return getValue(osType).getName();
    }

    /**
     * Gets the value corresponding to the specified JNA osType ordinal.
     *
     * @param osType The ordinal value matching the order of constants in this enum, corresponding to JNA's
     *               {@code Platform.getOSType()} return values.
     * @return the value corresponding to the specified platform type
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#getValue(int)}
     */
    @Deprecated
    public static PlatformEnum getValue(int osType) {
        if (osType < 0 || osType >= UNKNOWN.ordinal()) {
            return UNKNOWN;
        }
        return values()[osType];
    }

    /**
     * Gets the current platform, detected from system properties without loading JNA.
     *
     * @return the current platform
     *
     * @deprecated Use {@link oshi.util.PlatformEnum#getCurrentPlatform()}
     */
    @Deprecated
    public static PlatformEnum getCurrentPlatform() {
        return CURRENT_PLATFORM;
    }
}
