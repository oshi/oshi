/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

import java.util.Locale;

/**
 * An enumeration of supported operating systems. The order of declaration matches the osType constants in the JNA
 * Platform class.
 */
public enum PlatformEnum {

    /**
     * macOS
     */
    MACOS("macOS"),
    /**
     * A flavor of Linux
     */
    LINUX("Linux"),
    /**
     * Microsoft Windows
     */
    WINDOWS("Windows"),
    /**
     * Solaris (SunOS)
     */
    SOLARIS("Solaris"),
    /**
     * FreeBSD
     */
    FREEBSD("FreeBSD"),
    /**
     * OpenBSD
     */
    OPENBSD("OpenBSD"),
    /**
     * Windows Embedded Compact
     */
    WINDOWSCE("Windows CE"),
    /**
     * IBM AIX
     */
    AIX("AIX"),
    /**
     * Android
     */
    ANDROID("Android"),
    /**
     * GNU operating system
     */
    GNU("GNU"),
    /**
     * Debian GNU/kFreeBSD
     */
    KFREEBSD("kFreeBSD"),
    /**
     * NetBSD
     */
    NETBSD("NetBSD"),
    /**
     * DragonFly BSD
     */
    DRAGONFLYBSD("DragonFly BSD"),
    /**
     * An unspecified system
     */
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
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the friendly name of the specified JNA Platform type
     *
     * @param osType The constant returned from JNA's {@link com.sun.jna.Platform#getOSType()} method.
     * @return the friendly name of the specified JNA Platform type
     */
    public static String getName(int osType) {
        return getValue(osType).getName();
    }

    /**
     * Gets the value corresponding to the specified JNA Platform type
     *
     * @param osType The constant returned from JNA's {@link com.sun.jna.Platform#getOSType()} method.
     * @return the value corresponding to the specified JNA Platform type
     */
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
     */
    public static PlatformEnum getCurrentPlatform() {
        return CURRENT_PLATFORM;
    }
}
