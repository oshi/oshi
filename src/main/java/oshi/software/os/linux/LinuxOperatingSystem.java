/**
 * Copyright (c) Alessandro Perucchi, 2014
 * alessandro[at]perucchi[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.linux;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystemVersion;
import oshi.software.os.linux.proc.OSVersionInfoEx;

/**
 * Linux is a family of free operating systems most commonly used on personal
 * computers.
 *
 * @author alessandro[at]perucchi[dot]org
 */
public class LinuxOperatingSystem implements OperatingSystem {

	private OperatingSystemVersion _version = null;
	private String _manufacturer = null;

	public String getFamily() {
		if (_manufacturer == null) {
			Scanner in;
			try {
				in = new Scanner(new FileReader("/etc/os-release"));
			} catch (FileNotFoundException e) {
				return "";
			}
			in.useDelimiter("\n");
			while (in.hasNext()) {
				String[] splittedLine = in.next().split("=");
				if (splittedLine[0].equals("NAME")) {
					// remove beginning and ending '"' characters, etc from
					// NAME="Ubuntu"
					_manufacturer = splittedLine[1].replaceAll("^\"|\"$", "");
					break;
				}
			}
			in.close();
		}
		return _manufacturer;
	}

	public String getManufacturer() {
		return "GNU/Linux";
	}

	public OperatingSystemVersion getVersion() {
		if (_version == null) {
			_version = new OSVersionInfoEx();
		}
		return _version;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getManufacturer());
		sb.append(" ");
		sb.append(getFamily());
		sb.append(" ");
		sb.append(getVersion().toString());
		return sb.toString();
	}
}
