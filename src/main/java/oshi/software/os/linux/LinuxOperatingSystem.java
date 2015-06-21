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
	private String _family = null;

	@Override
	public String getFamily() {
		if (this._family == null) {
			try (final Scanner in = new Scanner(new FileReader("/etc/os-release"))) {
				in.useDelimiter("\n");
				while (in.hasNext()) {
					String[] splittedLine = in.next().split("=");
					if (splittedLine[0].equals("NAME")) {
						// remove beginning and ending '"' characters, etc from
						// NAME="Ubuntu"
						this._family = splittedLine[1].replaceAll("^\"|\"$", "");
						break;
					}
				}
			} catch (FileNotFoundException e) {
				return "";
			}
		}
		return this._family;
	}

	@Override
	public String getManufacturer() {
		return "GNU/Linux";
	}

	@Override
	public OperatingSystemVersion getVersion() {
		if (this._version == null) {
			this._version = new OSVersionInfoEx();
		}
		return this._version;
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
