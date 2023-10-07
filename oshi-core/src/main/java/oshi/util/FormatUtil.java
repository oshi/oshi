/*
 * Copyright 2016-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import java.math.BigInteger;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Formatting utility for appending units or converting between number types.
 */
@ThreadSafe
public final class FormatUtil {
    /**
     * Binary prefixes, used in IEC Standard for naming bytes.
     * (https://en.wikipedia.org/wiki/International_Electrotechnical_Commission)
     *
     * Should be used for most representations of bytes
     */
    private static final long KIBI = 1L << 10;
    private static final long MEBI = 1L << 20;
    private static final long GIBI = 1L << 30;
    private static final long TEBI = 1L << 40;
    private static final long PEBI = 1L << 50;
    private static final long EXBI = 1L << 60;

    /**
     * Decimal prefixes, used for Hz and other metric units and for bytes by hard drive manufacturers
     */
    private static final long KILO = 1_000L;
    private static final long MEGA = 1_000_000L;
    private static final long GIGA = 1_000_000_000L;
    private static final long TERA = 1_000_000_000_000L;
    private static final long PETA = 1_000_000_000_000_000L;
    private static final long EXA = 1_000_000_000_000_000_000L;

    /*
     * Two's complement reference: 2^64.
     */
    private static final BigInteger TWOS_COMPLEMENT_REF = BigInteger.ONE.shiftLeft(64);

    /** Constant <code>HEX_ERROR="0x%08X"</code> */
    public static final String HEX_ERROR = "0x%08X";

    private FormatUtil() {
    }

    /**
     * Format bytes into a rounded string representation using IEC standard (matches Mac/Linux). For hard drive
     * capacities, use @link {@link #formatBytesDecimal(long)}. For Windows displays for KB, MB and GB, in JEDEC units,
     * edit the returned string to remove the 'i' to display the (incorrect) JEDEC units.
     *
     * @param bytes Bytes.
     * @return Rounded string representation of the byte size.
     */
    public static String formatBytes(long bytes) {
        if (bytes == 1L) { // bytes
            return String.format(Locale.ROOT, "%d byte", bytes);
        } else if (bytes < KIBI) { // bytes
            return String.format(Locale.ROOT, "%d bytes", bytes);
        } else if (bytes < MEBI) { // KiB
            return formatUnits(bytes, KIBI, "KiB");
        } else if (bytes < GIBI) { // MiB
            return formatUnits(bytes, MEBI, "MiB");
        } else if (bytes < TEBI) { // GiB
            return formatUnits(bytes, GIBI, "GiB");
        } else if (bytes < PEBI) { // TiB
            return formatUnits(bytes, TEBI, "TiB");
        } else if (bytes < EXBI) { // PiB
            return formatUnits(bytes, PEBI, "PiB");
        } else { // EiB
            return formatUnits(bytes, EXBI, "EiB");
        }
    }

    /**
     * Format units as exact integer or fractional decimal based on the prefix, appending the appropriate units
     *
     * @param value  The value to format
     * @param prefix The divisor of the unit multiplier
     * @param unit   A string representing the units
     * @return A string with the value
     */
    private static String formatUnits(long value, long prefix, String unit) {
        if (value % prefix == 0) {
            return String.format(Locale.ROOT, "%d %s", value / prefix, unit);
        }
        return String.format(Locale.ROOT, "%.1f %s", (double) value / prefix, unit);
    }

    /**
     * Format bytes into a rounded string representation using decimal SI units. These are used by hard drive
     * manufacturers for capacity. Most other storage should use {@link #formatBytes(long)}.
     *
     * @param bytes Bytes.
     * @return Rounded string representation of the byte size.
     */
    public static String formatBytesDecimal(long bytes) {
        if (bytes == 1L) { // bytes
            return String.format(Locale.ROOT, "%d byte", bytes);
        } else if (bytes < KILO) { // bytes
            return String.format(Locale.ROOT, "%d bytes", bytes);
        } else {
            return formatValue(bytes, "B");
        }
    }

    /**
     * Format hertz into a string to a rounded string representation.
     *
     * @param hertz Hertz.
     * @return Rounded string representation of the hertz size.
     */
    public static String formatHertz(long hertz) {
        return formatValue(hertz, "Hz");
    }

    /**
     * Format arbitrary units into a string to a rounded string representation.
     *
     * @param value The value
     * @param unit  Units to append metric prefix to
     * @return Rounded string representation of the value with metric prefix to extension
     */
    public static String formatValue(long value, String unit) {
        if (value < KILO) {
            return String.format(Locale.ROOT, "%d %s", value, unit).trim();
        } else if (value < MEGA) { // K
            return formatUnits(value, KILO, "K" + unit);
        } else if (value < GIGA) { // M
            return formatUnits(value, MEGA, "M" + unit);
        } else if (value < TERA) { // G
            return formatUnits(value, GIGA, "G" + unit);
        } else if (value < PETA) { // T
            return formatUnits(value, TERA, "T" + unit);
        } else if (value < EXA) { // P
            return formatUnits(value, PETA, "P" + unit);
        } else { // E
            return formatUnits(value, EXA, "E" + unit);
        }
    }

    /**
     * Formats an elapsed time in seconds as days, hh:mm:ss.
     *
     * @param secs Elapsed seconds
     * @return A string representation of elapsed time
     */
    public static String formatElapsedSecs(long secs) {
        long eTime = secs;
        final long days = TimeUnit.SECONDS.toDays(eTime);
        eTime -= TimeUnit.DAYS.toSeconds(days);
        final long hr = TimeUnit.SECONDS.toHours(eTime);
        eTime -= TimeUnit.HOURS.toSeconds(hr);
        final long min = TimeUnit.SECONDS.toMinutes(eTime);
        eTime -= TimeUnit.MINUTES.toSeconds(min);
        final long sec = eTime;
        return String.format(Locale.ROOT, "%d days, %02d:%02d:%02d", days, hr, min, sec);
    }

    /**
     * Convert unsigned int to signed long.
     *
     * @param x Signed int representing an unsigned integer
     * @return long value of x unsigned
     */
    public static long getUnsignedInt(int x) {
        return x & 0x0000_0000_ffff_ffffL;
    }

    /**
     * Represent a 32 bit value as if it were an unsigned integer.
     *
     * This is a Java 7 implementation of Java 8's Integer.toUnsignedString.
     *
     * @param i a 32 bit value
     * @return the string representation of the unsigned integer
     */
    public static String toUnsignedString(int i) {
        if (i >= 0) {
            return Integer.toString(i);
        }
        return Long.toString(getUnsignedInt(i));
    }

    /**
     * Represent a 64 bit value as if it were an unsigned long.
     *
     * This is a Java 7 implementation of Java 8's Long.toUnsignedString.
     *
     * @param l a 64 bit value
     * @return the string representation of the unsigned long
     */
    public static String toUnsignedString(long l) {
        if (l >= 0) {
            return Long.toString(l);
        }
        return BigInteger.valueOf(l).add(TWOS_COMPLEMENT_REF).toString();
    }

    /**
     * Translate an integer error code to its hex notation
     *
     * @param errorCode The error code
     * @return A string representing the error as 0x....
     */
    public static String formatError(int errorCode) {
        return String.format(Locale.ROOT, HEX_ERROR, errorCode);
    }

    /**
     * Rounds a floating point number to the nearest integer
     *
     * @param x the floating point number
     * @return the integer
     */
    public static int roundToInt(double x) {
        return (int) Math.round(x);
    }
}
