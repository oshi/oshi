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

import oshi.software.os.OperatingSystemVersion;

/**
 * Contains operating system version information. The information includes major
 * and minor version numbers, a build number, a platform identifier, and
 * descriptive text about the operating system.
 * 
 * @author alessandro[at]perucchi[dot]org
 */
public class OSVersionInfoEx implements OperatingSystemVersion {

	private String _version = null;
	private String _codeName = null;
	private String version = null;

	public OSVersionInfoEx() {
		Scanner in = null;
		try {
			in = new Scanner(new FileReader("/etc/os-release"));
		} catch (FileNotFoundException e) {
			return;
		}
		in.useDelimiter("\n");
		while (in.hasNext()) {
			String[] splittedLine = in.next().split("=");
			if (splittedLine[0].equals("VERSION_ID")) {
				setVersion(splittedLine[1]);
			}
			if (splittedLine[0].equals("VERSION")) {
				setCodeName(splittedLine[1].split("[()]")[1]);
			}
		}
		in.close();
	}

	/**
	 * @return the _version
	 */
	public String getVersion() {
		return _version;
	}

	/**
	 * @param _version
	 *            the _version to set
	 */
	public void setVersion(String _version) {
		this._version = _version;
	}

	/**
	 * @return the _codeName
	 */
	public String getCodeName() {
		return _codeName;
	}

	/**
	 * @param _codeName
	 *            the _codeName to set
	 */
	public void setCodeName(String _codeName) {
		this._codeName = _codeName;
	}

	@Override
	public String toString() {
		if (version == null) {
			version=getVersion()+" ("+getCodeName()+")";
		}
		return version;
	}

}
