/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi;

/**
 * An enumeration of supported operating systems. The order of declaration
 * matches the osType constants in the JNA Platform class.
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
    UNKNOWN("Unknown"),
    /**
     * Legacy name for Mac OS version 10.x
     *
     * @deprecated use {@link MACOS}
     */
    @Deprecated
    MACOSX("macOS");

    private String name;

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
     * @param osType
     *            The constant returned from JNA's
     *            {@link com.sun.jna.Platform#getOSType()} method.
     * @return the friendly name of the specified JNA Platform type
     */
    public static String getName(int osType) {
        return getValue(osType).getName();
    }

    /**
     * Gets the value corresponding to the specified JNA Platform type
     *
     * @param osType
     *            The constant returned from JNA's
     *            {@link com.sun.jna.Platform#getOSType()} method.
     * @return the value corresponding to the specified JNA Platform type
     */
    public static PlatformEnum getValue(int osType) {
        if (osType < 0 || osType >= UNKNOWN.ordinal()) {
            return UNKNOWN;
        }
        return values()[osType];
    }
}
