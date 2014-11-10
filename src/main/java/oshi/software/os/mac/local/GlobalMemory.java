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
		long returnCurrentUsageMemory=0;
		for (String line : ExecutingCommand.runNative("vm_stat")) {
			if (line.startsWith("Pages free:")) {
				String[] memorySplit = line.split(":\\s+");
				returnCurrentUsageMemory+=new Long(memorySplit[1].replace(".",""));
			} else if(line.startsWith("Pages speculative:")) {
				String[] memorySplit = line.split(":\\s+");
				returnCurrentUsageMemory+=new Long(memorySplit[1].replace(".",""));
			}
		}
		returnCurrentUsageMemory=returnCurrentUsageMemory*4096;
		return returnCurrentUsageMemory;
	}

	public long getTotal() {
		if (totalMemory == 0) {
			totalMemory=new Long(ExecutingCommand.getFirstAnswer("sysctl -n hw.memsize"));
		}
		return totalMemory;
	}
}
