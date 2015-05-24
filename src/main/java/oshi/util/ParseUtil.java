/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String parsing utility.
 * 
 * @author alessio.fachechi[at]gmail[dot]com
 */
public abstract class ParseUtil {

	/**
	 * Hertz related variables
	 */
	final private static String Hertz = "Hz";
	final private static String kiloHertz = "k" + Hertz;
	final private static String megaHertz = "M" + Hertz;
	final private static String gigaHertz = "G" + Hertz;
	final private static String teraHertz = "T" + Hertz;
	final private static String petaHertz = "P" + Hertz;
	final private static Map<String, Long> multipliers;

	static {
		multipliers = new HashMap<String, Long>();
		multipliers.put(Hertz, 1L);
		multipliers.put(kiloHertz, 1000L);
		multipliers.put(megaHertz, 1000000L);
		multipliers.put(gigaHertz, 1000000000L);
		multipliers.put(teraHertz, 1000000000000L);
		multipliers.put(petaHertz, 1000000000000000L);
	}

	/**
	 * Parse hertz from a string, eg. "2.00MHz" in 2000000L.
	 * 
	 * @param hertz
	 *            Hertz size.
	 * @return {@link Long} Hertz value or -1 if not parsable.
	 * 
	 * @author alessio.fachechi[at]gmail[dot]com
	 */
	public static long parseHertz(String hertz) {
		Pattern pattern = Pattern.compile("(\\d+(.\\d+)?) ?([kMGT]?Hz)");
		Matcher matcher = pattern.matcher(hertz.trim());

		if (matcher.find() && (matcher.groupCount() == 3)) {
			try {
				Double value = Double.valueOf(matcher.group(1));
				String unit = matcher.group(3);

				if (multipliers.containsKey(unit)) {
					value = value * multipliers.get(unit);
					return value.longValue();
				}
			} catch (NumberFormatException e) {
			}
		}

		return -1L;
	}

}
