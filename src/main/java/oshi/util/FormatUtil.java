/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.util;

import java.math.BigDecimal;

/**
 * String formatting utility.
 * 
 * @author dblock[at]dblock[dot]org
 */
public abstract class FormatUtil {

	/**
	 * Added these variable for easier reading Using the IEC Standard for naming
	 * the units
	 * (http://en.wikipedia.org/wiki/International_Electrotechnical_Commission)
	 */
	final private static long kibiByte = 1024L;
	final private static long mebiByte = kibiByte * kibiByte;
	final private static long gibiByte = mebiByte * kibiByte;
	final private static long tebiByte = gibiByte * kibiByte;
	final private static long pebiByte = tebiByte * kibiByte;

	/**
	 * Hertz related variables
	 */
	final private static long kiloHertz = 1000L;
	final private static long megaHertz = kiloHertz * kiloHertz;
	final private static long gigaHertz = megaHertz * kiloHertz;
	final private static long teraHertz = gigaHertz * kiloHertz;
	final private static long petaHertz = teraHertz * kiloHertz;

	/**
	 * Format bytes into a string to a rounded string representation. Using the
	 * JEDEC representation for KB, MB and GB Using the IEC representation for
	 * TiB
	 * 
	 * @param bytes
	 *            Bytes.
	 * @return Rounded string representation of the byte size.
	 */
	public static String formatBytes(long bytes) {
		if (bytes == 1) { // bytes
			return String.format("%d byte", bytes);
		} else if (bytes < kibiByte) { // bytes
			return String.format("%d bytes", bytes);
		} else if (bytes < mebiByte && bytes % kibiByte == 0) { // KB
			return String.format("%.0f KB", (double) bytes / kibiByte);
		} else if (bytes < mebiByte) { // KB
			return String.format("%.1f KB", (double) bytes / kibiByte);
		} else if (bytes < gibiByte && bytes % mebiByte == 0) { // MB
			return String.format("%.0f MB", (double) bytes / mebiByte);
		} else if (bytes < gibiByte) { // MB
			return String.format("%.1f MB", (double) bytes / mebiByte);
		} else if (bytes % gibiByte == 0 && bytes < tebiByte) { // GB
			return String.format("%.0f GB", (double) bytes / gibiByte);
		} else if (bytes < tebiByte) {
			return String.format("%.1f GB", (double) bytes / gibiByte);
		} else if (bytes % tebiByte == 0 && bytes < pebiByte) { // TiB
			return String.format("%.0f TiB", (double) bytes / tebiByte);
		} else if (bytes < pebiByte) {
			return String.format("%.1f TiB", (double) bytes / tebiByte);
		} else {
			return String.format("%d bytes", bytes);
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
		if (hertz < kiloHertz ) { // Hz
			return String.format("%d Hz", hertz);
		} else if (hertz < megaHertz && hertz % kiloHertz == 0) { // KHz
			return String.format("%.0f kHz", (double) hertz / kiloHertz);
		} else if (hertz < megaHertz) {
			return String.format("%.1f kHz", (double) hertz / kiloHertz);
		} else if (hertz < gigaHertz && hertz % megaHertz == 0) { // MHz
			return String.format("%.0f MHz", (double) hertz / megaHertz);
		} else if (hertz < gigaHertz) {
			return String.format("%.1f MHz", (double) hertz / megaHertz);
		} else if (hertz < teraHertz && hertz % gigaHertz == 0) { // GHz
			return String.format("%.0f GHz", (double) hertz / gigaHertz);
		} else if (hertz < teraHertz) {
			return String.format("%.1f GHz", (double) hertz / gigaHertz);
		} else if (hertz < petaHertz && hertz % teraHertz == 0) { // THz
			return String.format("%.0f THz", (double) hertz / teraHertz);
		} else if (hertz < petaHertz) {
			return String.format("%.1f THz", (double) hertz / teraHertz);
		} else {
			return String.format("%d Hz", hertz);
		}
	}

	/**
	 * Round to certain number of decimals
	 *
	 * @param d
	 * @param decimalPlace
	 * @return
	 */
	public static float round(float d, int decimalPlace) {
		BigDecimal bd = new BigDecimal(Float.toString(d));
		bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
		return bd.floatValue();
	}

	/**
	 * Convert unsigned int to signed long
	 * 
	 * @param x
	 * @return long value of x unsigned
	 */
	public static long getUnsignedInt(int x) {
		return x & 0x00000000ffffffffL;
	}
}
