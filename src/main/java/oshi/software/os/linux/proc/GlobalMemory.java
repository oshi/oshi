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
 * Memory obtained by GlobalMemoryStatusEx.
 * 
 * @author alessandro[at]perucchi[dot]org
 */
public class GlobalMemory implements Memory {

	private long totalMemory = 0;

	public long getAvailable() {
		long returnCurrentUsageMemory=0;
		Scanner in=null;
		try {
			in = new Scanner(new FileReader("/proc/meminfo"));
		} catch (FileNotFoundException e) {
			return returnCurrentUsageMemory;
		}
		in.useDelimiter("\n");
		while (in.hasNext()) {
			String checkLine = in.next();
			if (checkLine.startsWith("MemFree:") || checkLine.startsWith("MemAvailable:")) {
				String[] memorySplit = checkLine.split("\\s+");
				returnCurrentUsageMemory=new Long(memorySplit[1]);
				if (memorySplit[2].equals("kB")) {
					returnCurrentUsageMemory*=1024;
				}
				if (memorySplit[0].equals("MemAvailable:")) {
					break;
				}
			}
		}
		in.close();
		return returnCurrentUsageMemory;
	}

	public long getTotal() {
		if (totalMemory == 0) {
			Scanner in=null;
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
					totalMemory=new Long(memorySplit[1]);
					if (memorySplit[2].equals("kB")) {
						totalMemory*=1024;
					}
					break;
				}
			}
			in.close();
		}
		return totalMemory;
	}
}
