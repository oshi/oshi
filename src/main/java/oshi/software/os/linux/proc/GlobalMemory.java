/**
 * Copyright (c) Alessandro Perucchi, 2014
 * alessandro[at]perucchi[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.linux.proc;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

import oshi.hardware.Memory;

/**
 * Memory obtained by /proc/meminfo.
 * 
 * @author alessandro[at]perucchi[dot]org
 */
public class GlobalMemory implements Memory {

	private long totalMemory = 0;

	public long getAvailable() {
		long returnCurrentUsageMemory = 0;
		Scanner in = null;
		try {
			in = new Scanner(new FileReader("/proc/meminfo"));
		} catch (FileNotFoundException e) {
			return returnCurrentUsageMemory;
		}
		in.useDelimiter("\n");
		while (in.hasNext()) {
			String checkLine = in.next();
			if (checkLine.startsWith("MemAvailable:")) {
				String[] memorySplit = checkLine.split("\\s+");
				returnCurrentUsageMemory = parseMeminfo(memorySplit);
				break;
			} else if (checkLine.startsWith("MemFree:")) {
				String[] memorySplit = checkLine.split("\\s+");
				returnCurrentUsageMemory += parseMeminfo(memorySplit);
			} else if (checkLine.startsWith("Inactive:")) {
				String[] memorySplit = checkLine.split("\\s+");
				returnCurrentUsageMemory += parseMeminfo(memorySplit);
			}
		}
		in.close();
		return returnCurrentUsageMemory;
	}

	public long getTotal() {
		if (totalMemory == 0) {
			Scanner in = null;
			try {
				in = new Scanner(new FileReader("/proc/meminfo"));
			} catch (FileNotFoundException e) {
				totalMemory = 0;
				return totalMemory;
			}
			in.useDelimiter("\n");
			while (in.hasNext()) {
				String checkLine = in.next();
				if (checkLine.startsWith("MemTotal:")) {
					String[] memorySplit = checkLine.split("\\s+");
					totalMemory = parseMeminfo(memorySplit);
					break;
				}
			}
			in.close();
		}
		return totalMemory;
	}

	private long parseMeminfo(String[] memorySplit) {
		if (memorySplit.length < 2) {
			return 0l;
		}
		long memory = new Long(memorySplit[1]);
		if (memorySplit.length > 2 && memorySplit[2].equals("kB")) {
			memory *= 1024;
		}
		return memory;
	}
}
