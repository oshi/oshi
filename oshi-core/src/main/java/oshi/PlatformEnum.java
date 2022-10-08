/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

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
     * An unspecified system
     */
    UNKNOWN("Unknown");

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
}
