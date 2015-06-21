/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2015 The Oshi Project Team
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.util;

/**
 * General utility methods
 * 
 * @author widdis[at]gmail[dot]com
 */
public class Util {

	/**
	 * Sleeps for the specified number of milliseconds
	 * 
	 * @param ms
	 *            How long to sleep
	 */
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			// Awake, O sleeper
		}
	}

	/**
	 * Sleeps for the specified number of milliseconds after the given system
	 * time in milliseconds. If that number of milliseconds has already elapsed,
	 * does nothing.
	 * 
	 * @param ms
	 *            How long after startTime to sleep
	 */
	public static void sleepAfter(long startTime, long ms) {
		long now = System.currentTimeMillis();
		if (now < startTime + ms)
			sleep(startTime + ms - now);
	}
}