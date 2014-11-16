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
				// remove beginning and ending '"' characters, etc from
				// VERSION_ID="14.04"
				setVersion(splittedLine[1].replaceAll("^\"|\"$", ""));
			}
			if (splittedLine[0].equals("VERSION")) {
				// remove beginning and ending '"' characters
				splittedLine[1] = splittedLine[1].replaceAll("^\"|\"$", "");

				// Check basically if the code is between parenthesis or after
				// the coma-space

				// Basically, until now, that seems to be the standard to use
				// parenthesis for the codename.
				String[] split = splittedLine[1].split("[()]");
				if (split.length <= 1)
					// We are probably with Ubuntu, so need to get that part
					// correctly.
					split = splittedLine[1].split(", ");

				if (split.length > 1)
					setCodeName(split[1]);
				else
					setCodeName(splittedLine[1]);
			}
		}
		in.close();
	}

	/**
	 * @return the _codeName
	 */
	public String getCodeName() {
		return _codeName;
	}

	/**
	 * @return the _version
	 */
	public String getVersion() {
		return _version;
	}

	/**
	 * @param _codeName
	 *            the _codeName to set
	 */
	public void setCodeName(String _codeName) {
		this._codeName = _codeName;
	}

	/**
	 * @param _version
	 *            the _version to set
	 */
	public void setVersion(String _version) {
		this._version = _version;
	}

	@Override
	public String toString() {
		if (version == null) {
			version = getVersion() + " (" + getCodeName() + ")";
		}
		return version;
	}

}
