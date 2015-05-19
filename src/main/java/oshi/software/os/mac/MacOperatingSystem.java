/*
 * Copyright (c) Alessandro Perucchi, 2014
 * alessandro[at]perucchi[dot]org
 * Daniel Widdis, 2015
 * widdis[at]gmail[dot]com
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.mac;

import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystemVersion;
import oshi.software.os.mac.local.OSVersionInfoEx;

/**
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */

public class MacOperatingSystem implements OperatingSystem {
	private String _family;

	private OperatingSystemVersion _version = null;

	public OperatingSystemVersion getVersion() {
		if (_version == null) {
			_version = new OSVersionInfoEx();
		}
		return _version;
	}

	public String getFamily() {
		if (_family == null)
			_family = System.getProperty("os.name");
		return _family;
	}

	public String getManufacturer() {
		return "Apple";
	}

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
