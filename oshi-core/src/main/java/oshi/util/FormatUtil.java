/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.util;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Formatting utility for appending units or converting between number types.
 *
 * @author dblock[at]dblock[dot]org
 */
public class FormatUtil {

    /**
     * Binary prefixes, used in IEC Standard for naming bytes.
     * (http://en.wikipedia.org/wiki/International_Electrotechnical_Commission)
     *
     * Should be used for most representations of bytes
     */
    final private static long KIBI = 1L << 10;
    final private static long MEBI = 1L << 20;
    final private static long GIBI = 1L << 30;
    final private static long TEBI = 1L << 40;
    final private static long PEBI = 1L << 50;
    final private static long EXBI = 1L << 60;

    /**
     * Decimal prefixes, used for Hz and other metric units and for bytes by
     * hard drive manufacturers
     */
    final private static long KILO = 1000L;
    final private static long MEGA = 1000000L;
    final private static long GIGA = 1000000000L;
    final private static long TERA = 1000000000000L;
    final private static long PETA = 1000000000000000L;
    final private static long EXA = 1000000000000000000L;

    /**
     * Format bytes into a rounded string representation using IEC standard
     * (matches Mac/Linux). For hard drive capacities, use @link
     * {@link #formatBytesDecimal(long)}. For Windows displays for KB, MB and
     * GB, in JEDEC units, edit the returned string to remove the 'i' to display
     * the (incorrect) JEDEC units.
     *
     * @param bytes
     *            Bytes.
     * @return Rounded string representation of the byte size.
     */
    public static String formatBytes(long bytes) {
        if (bytes == 1L) { // bytes
            return String.format("%d byte", bytes);
        } else if (bytes < KIBI) { // bytes
            return String.format("%d bytes", bytes);
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
     * Format units as exact integer or fractional decimal based on the prefix,
     * appending the appropriate units
     *
     * @param value
     *            The value to format
     * @param prefix
     *            The divisor of the unit multiplier
     * @param unit
     *            A string representing the units
     * @return A string with the value
     */
    private static String formatUnits(long value, long prefix, String unit) {
        if (value % prefix == 0) {
            return String.format("%d %s", value / prefix, unit);
        }
        return String.format("%.1f %s", (double) value / prefix, unit);
    }

    /**
     * Format bytes into a rounded string representation using decimal SI units.
     * These are used by hard drive manufacturers for capacity. Most other
     * storage should use {@link #formatBytes(long)}.
     *
     * @param bytes
     *            Bytes.
     * @return Rounded string representation of the byte size.
     */
    public static String formatBytesDecimal(long bytes) {
        if (bytes == 1L) { // bytes
            return String.format("%d byte", bytes);
        } else if (bytes < KILO) { // bytes
            return String.format("%d bytes", bytes);
        } else {
            return formatValue(bytes, "B");
        }
    }

    /**
     * Format hertz into a string to a rounded string representation.
     *
     * @param hertz
     *            Hertz.
     * @return Rounded string representation of the hertz size.
     */
    public static String formatHertz(long hertz) {
        return formatValue(hertz, "Hz");
    }

    /**
     * Format arbitrary units into a string to a rounded string representation.
     *
     * @param value
     *            The value
     * @param unit
     *            Units to append metric prefix to
     * @return Rounded string representation of the value with metric prefix to
     *         extension
     */
    public static String formatValue(long value, String unit) {
        if (value < KILO) {
            return String.format("%d %s", value, unit);
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
     * @param secs
     *            Elapsed seconds
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
        return String.format("%d days, %02d:%02d:%02d", days, hr, min, sec);
    }

    /**
     * Round to certain number of decimals.
     *
     * @param d
     *            Number to be rounded
     * @param decimalPlace
     *            Number of decimal places to round to
     * @return rounded result
     */
    public static float round(float d, int decimalPlace) {
        final BigDecimal bd = new BigDecimal(Float.toString(d)).setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    /**
     * Convert unsigned int to signed long.
     *
     * @param x
     *            Signed int representing an unsigned integer
     * @return long value of x unsigned
     */
    public static long getUnsignedInt(int x) {
        return x & 0x00000000ffffffffL;
    }

}
