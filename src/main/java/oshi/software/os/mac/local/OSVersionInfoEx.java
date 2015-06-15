/*
 * Copyright (c) Alessandro Perucchi, 2014
 * alessandro[at]perucchi[dot]org
 * Daniel Widdis, 2015
 * widdis[at]gmail[dot]com
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.mac.local;

import oshi.software.os.OperatingSystemVersion;

import com.sun.jna.LastErrorException;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */

public class OSVersionInfoEx implements OperatingSystemVersion {

	private String _version = null;
	private String _codeName = null;
	private String _versionStr = null;
	private String _buildNumber = null;

	public OSVersionInfoEx() {
	}

	/**
	 * @return the _version
	 */
	public String getVersion() {
		if (_version == null)
			_version = System.getProperty("os.version");
		return _version;
	}

	/**
	 * @param version
	 *            the version to set
	 */
	public void setVersion(String version) {
		_version = version;
	}

	/**
	 * @return the _codeName
	 */
	public String getCodeName() {
		if (_codeName == null) {
			if (getVersion() != null) {
				String[] versionSplit = getVersion().split("\\.");
				if (versionSplit.length > 1 && versionSplit[0].equals("10")) {
					switch (Integer.parseInt(versionSplit[1])) {
					case 0:
						_codeName = "Cheetah";
						break;
					case 1:
						_codeName = "Puma";
						break;
					case 2:
						_codeName = "Jaguar";
						break;
					case 3:
						_codeName = "Panther";
						break;
					case 4:
						_codeName = "Tiger";
						break;
					case 5:
						_codeName = "Leopard";
						break;
					case 6:
						_codeName = "Snow Leopard";
						break;
					case 7:
						_codeName = "Lion";
						break;
					case 8:
						_codeName = "Mountain Lion";
						break;
					case 9:
						_codeName = "Mavericks";
						break;
					case 10:
						_codeName = "Yosemite";
						break;
					case 11:
						_codeName = "El Capitan";
						break;
					default:
						_codeName = "";
					}

				} else
					_codeName = "";
			}
		}
		return _codeName;
	}

	/**
	 * @param codeName
	 *            the codeName to set
	 */
	public void setCodeName(String codeName) {
		_codeName = codeName;
	}

	public String getBuildNumber() {
		if (_buildNumber == null) {
			int[] mib = { SystemB.CTL_KERN, SystemB.KERN_OSVERSION };
			IntByReference size = new IntByReference();
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, null, size, null,
					0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			Pointer p = new Memory(size.getValue() + 1);
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, p, size, null, 0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			_buildNumber = p.getString(0);
		}
		return _buildNumber;
	}

	public void setBuildNumber(String buildNumber) {
		this._buildNumber = buildNumber;
	}

	@Override
	public String toString() {
		if (_versionStr == null) {
			StringBuilder sb = new StringBuilder(getVersion());
			if (getCodeName().length() > 0)
				sb.append(" (").append(getCodeName()).append(")");
			sb.append(" build ").append(getBuildNumber());
			_versionStr = sb.toString();
		}
		return _versionStr;
	}
}
