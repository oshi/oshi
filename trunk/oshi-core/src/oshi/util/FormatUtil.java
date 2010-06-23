/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.util;

/**
 * String formatting utility.
 * @author dblock[at]dblock[dot]org
 */
public abstract class FormatUtil {

	/**
	 * Format bytes into a string to a rounded string representation.
	 * @param bytes
	 *  Bytes.
	 * @return
	 *  Rounded string representation of the byte size.
	 */
    public static String formatBytes(long bytes) {
		if (bytes == 1) { // bytes
			return String.format("%d byte", bytes);
		} else if (bytes < 1024) { // bytes
			return String.format("%d bytes", bytes);
		} else if (bytes < 1048576 && bytes % 1024 == 0) { // Kb
			return String.format("%.0f KB", (double) bytes / 1024);
		} else if (bytes < 1048576) { // Kb
			return String.format("%.1f KB", (double) bytes / 1024);
		} else if (bytes % 1048576 == 0 && bytes < 1073741824) { // Mb
			return String.format("%.0f MB", (double) bytes / 1048576);
		} else if (bytes < 1073741824) { // Mb
			return String.format("%.1f MB", (double) bytes / 1048576);
		} else if (bytes % 1073741824 == 0 && bytes < 1099511627776L) { // GB
			return String.format("%.0f GB", (double) bytes / 1073741824);
		} else if (bytes < 1099511627776L ) {
			return String.format("%.1f GB", (double) bytes / 1073741824);
		} else if (bytes % 1099511627776L == 0 && bytes < 1125899906842624L) { // TB
			return String.format("%.0f TB", (double) bytes / 1099511627776L);
		} else if (bytes < 1125899906842624L ) {
			return String.format("%.1f TB", (double) bytes / 1099511627776L);
		} else {
			return String.format("%d bytes", bytes);
		}
    }
}
