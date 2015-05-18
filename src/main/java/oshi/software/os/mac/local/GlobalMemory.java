/**
 * Copyright (c) Alessandro Perucchi, 2014
 * alessandro[at]perucchi[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.mac.local;

import oshi.hardware.Memory;
import oshi.util.ExecutingCommand;

/**
 * Memory obtained by GlobalMemoryStatusEx.
 * 
 * @author alessandro[at]perucchi[dot]org
 */
public class GlobalMemory implements Memory {

	private long totalMemory = 0;

	public long getAvailable() {
		long returnCurrentUsageMemory = 0;
		long pageSize = 4096;
		for (String line : ExecutingCommand.runNative("vm_stat")) {
			if (line.matches("\\D+(page size of \\d+ bytes)\\D+")) {
				pageSize = new Long(line.replaceAll("\\D", ""));
			} else if (line.startsWith("Pages free:")
					|| line.startsWith("Pages speculative:")
					|| line.startsWith("Pages inactive:")) {
				String[] memorySplit = line.split(":\\s+");
				if (memorySplit.length > 1) {
					returnCurrentUsageMemory += new Long(
							memorySplit[1].replace(".", ""));
				}
			}
		}
		returnCurrentUsageMemory *= pageSize;
		return returnCurrentUsageMemory;
	}

	public long getTotal() {
		if (totalMemory == 0) {
			totalMemory = new Long(
					ExecutingCommand.getFirstAnswer("sysctl -n hw.memsize"));
		}
		return totalMemory;
	}
}
