package oshi.software.os.mac.local;

import oshi.software.os.OperatingSystemVersion;
import oshi.util.ExecutingCommand;

/**
 * @author alessandro[at]perucchi[dot]org
 */

public class OSVersionInfoEx implements OperatingSystemVersion {

	private String _version = null;
	private String _codeName = null;
	private String version = null;
	private String _buildNumber = null;

	public OSVersionInfoEx() {
	}

	/**
	 * @return the _version
	 */
	public String getVersion() {
		if (_version == null) {
			_version = ExecutingCommand
					.getFirstAnswer("sw_vers -productVersion");
		}
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
		if (_codeName == null) {
			if (getVersion() != null) {
				if ("10.0".equals(getVersion())
						|| getVersion().startsWith("10.0."))
					_codeName = "Cheetah";
				else if ("10.1".equals(getVersion())
						|| getVersion().startsWith("10.1."))
					_codeName = "Puma";
				else if ("10.2".equals(getVersion())
						|| getVersion().startsWith("10.2."))
					_codeName = "Jaguar";
				else if ("10.3".equals(getVersion())
						|| getVersion().startsWith("10.3."))
					_codeName = "Panther";
				else if ("10.4".equals(getVersion())
						|| getVersion().startsWith("10.4."))
					_codeName = "Tiger";
				else if ("10.5".equals(getVersion())
						|| getVersion().startsWith("10.5."))
					_codeName = "Leopard";
				else if ("10.6".equals(getVersion())
						|| getVersion().startsWith("10.6."))
					_codeName = "Snow Leopard";
				else if ("10.7".equals(getVersion())
						|| getVersion().startsWith("10.7."))
					_codeName = "Lion";
				else if ("10.8".equals(getVersion())
						|| getVersion().startsWith("10.8."))
					_codeName = "Mountain Lion";
				else if ("10.9".equals(getVersion())
						|| getVersion().startsWith("10.9."))
					_codeName = "Mavericks";
				else if ("10.10".equals(getVersion())
						|| getVersion().startsWith("10.10."))
					_codeName = "Yosemite";
			}
		}
		return _codeName;
	}

	/**
	 * @param _codeName
	 *            the _codeName to set
	 */
	public void setCodeName(String _codeName) {
		this._codeName = _codeName;
	}

	public String getBuildNumber() {
		if (_buildNumber == null)
			_buildNumber=ExecutingCommand
					.getFirstAnswer("sw_vers -buildVersion");

		return _buildNumber;
	}

	public void setBuildNumber(String buildNumber) {
		this._buildNumber = buildNumber;
	}

	@Override
	public String toString() {
		if (version == null) {
			version = getCodeName() + " (" + getVersion() + ") build "
					+ getBuildNumber();
		}
		return version;
	}

}
