/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.windows.nt;

import oshi.software.os.OperatingSystemVersion;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.OSVERSIONINFOEX;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.WinUser;

/**
 * Contains operating system version information. The information includes major
 * and minor version numbers, a build number, a platform identifier, and
 * descriptive text about the operating system.
 * 
 * @author dblock[at]dblock[dot]org
 */
public class OSVersionInfoEx implements OperatingSystemVersion {
	private OSVERSIONINFOEX _versionInfo;

	public OSVersionInfoEx() {
		_versionInfo = new OSVERSIONINFOEX();
		if (!Kernel32.INSTANCE.GetVersionEx(_versionInfo)) {
			throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
		}
	}

	/**
	 * The major version number of the operating system.
	 * 
	 * @return The major version within the following supported operating
	 *         systems. Windows 8: 6.2 Windows Server 2012: 6.2 Windows 7: 6.1
	 *         Windows Server 2008 R2: 6.1 Windows Server 2008: 6.0 Windows
	 *         Vista: 6.0 Windows Server 2003 R2: 5.2 Windows Home Server: 5.2
	 *         Windows Server 2003: 5.2 Windows XP Professional x64 Edition: 5.2
	 *         Windows XP: 5.1 Windows 2000: 5.0
	 */
	public int getMajor() {
		return _versionInfo.dwMajorVersion.intValue();
	}

	/**
	 * The minor version number of the operating system.
	 * 
	 * @return The minor version within the following supported operating
	 *         systems. Windows 8: 6.2 Windows Server 2012: 6.2 Windows 7: 6.1
	 *         Windows Server 2008 R2: 6.1 Windows Server 2008: 6.0 Windows
	 *         Vista: 6.0 Windows Server 2003 R2: 5.2 Windows Home Server: 5.2
	 *         Windows Server 2003: 5.2 Windows XP Professional x64 Edition: 5.2
	 *         Windows XP: 5.1 Windows 2000: 5.0
	 */
	public int getMinor() {
		return _versionInfo.dwMinorVersion.intValue();
	}

	/**
	 * The build number of the operating system.
	 * 
	 * @return Build number.
	 */
	public int getBuildNumber() {
		return _versionInfo.dwBuildNumber.intValue();
	}

	/**
	 * The operating system platform. This member can be VER_PLATFORM_WIN32_NT.
	 * 
	 * @return Platform ID.
	 */
	public int getPlatformId() {
		return _versionInfo.dwPlatformId.intValue();
	}

	/**
	 * String, such as "Service Pack 3", that indicates the latest Service Pack
	 * installed on the system. If no Service Pack has been installed, the
	 * string is empty.
	 * 
	 * @return Service pack.
	 */
	public String getServicePack() {
		return Native.toString(_versionInfo.szCSDVersion);
	}

	/**
	 * A bit mask that identifies the product suites available on the system.
	 * 
	 * @return Suite mask.
	 */
	public int getSuiteMask() {
		return _versionInfo.wSuiteMask.intValue();
	}

	/**
	 * Any additional information about the system.
	 * 
	 * @return Product type.
	 */
	public byte getProductType() {
		return _versionInfo.wProductType;
	}

	@Override
	public String toString() {
		String version = null;

		// see
		// http://msdn.microsoft.com/en-us/library/windows/desktop/ms724833%28v=vs.85%29.aspx
		if (getPlatformId() == WinNT.VER_PLATFORM_WIN32_NT) {
			// 8.1
			if (getMajor() == 6 && getMinor() == 3
					&& getProductType() == WinNT.VER_NT_WORKSTATION) {
				version = "8.1";
			}
			// Server 2008 R2
			else if (getMajor() == 6 && getMinor() == 3
					&& getProductType() != WinNT.VER_NT_WORKSTATION) {
				version = "Server 2012 R2";
			}
			// 8
			else if (getMajor() == 6 && getMinor() == 2
					&& getProductType() == WinNT.VER_NT_WORKSTATION) {
				version = "8";
			}
			// Server 2008
			else if (getMajor() == 6 && getMinor() == 2
					&& getProductType() != WinNT.VER_NT_WORKSTATION) {
				version = "Server 2012";
			}
			// 7
			else if (getMajor() == 6 && getMinor() == 1
					&& getProductType() == WinNT.VER_NT_WORKSTATION) {
				version = "7";
			}
			// Server 2008 R2
			else if (getMajor() == 6 && getMinor() == 1
					&& getProductType() != WinNT.VER_NT_WORKSTATION) {
				version = "Server 2008 R2";
			}
			// Server 2008
			else if (getMajor() == 6 && getMinor() == 0
					&& getProductType() != WinNT.VER_NT_WORKSTATION) {
				version = "Server 2008";
			}
			// Vista
			else if (getMajor() == 6 && getMinor() == 0
					&& getProductType() == WinNT.VER_NT_WORKSTATION) {
				version = "Vista";
			}
			// Server 2003
			else if (getMajor() == 5
					&& getMinor() == 2
					&& getProductType() != WinNT.VER_NT_WORKSTATION
					&& User32.INSTANCE.GetSystemMetrics(WinUser.SM_SERVERR2) != 0) {
				version = "Server 2003";
			}
			// Server 2003 R2
			else if (getMajor() == 5
					&& getMinor() == 2
					&& getProductType() != WinNT.VER_NT_WORKSTATION
					&& User32.INSTANCE.GetSystemMetrics(WinUser.SM_SERVERR2) == 0) {
				version = "Server 2003 R2";
			}
			// XP 64 bit
			else if (getMajor() == 5 && getMinor() == 2
					&& getProductType() == WinNT.VER_NT_WORKSTATION) {
				version = "XP";
			}
			// XP 32 bit
			else if (getMajor() == 5 && getMinor() == 1) {
				version = "XP";
			}
			// 2000
			else if (getMajor() == 5 && getMinor() == 0) {
				version = "2000";
			}
			// Windows NT
			else if (getMajor() == 4) {
				version = "NT 4";

				if ("Service Pack 6".equals(getServicePack())) {
					if (Advapi32Util
							.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE,
									"SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Hotfix\\Q246009")) {
						return "NT4 SP6a";
					}
				}

			} else {
				throw new RuntimeException("Unsupported Windows NT version: "
						+ _versionInfo.toString());
			}

			if (_versionInfo.wServicePackMajor.intValue() > 0) {
				version = version + " SP"
						+ _versionInfo.wServicePackMajor.intValue();
			}

		} else if (getPlatformId() == WinNT.VER_PLATFORM_WIN32_WINDOWS) {
			if (getMajor() == 4 && getMinor() == 90) {
				version = "ME";
			} else if (getMajor() == 4 && getMinor() == 10) {
				if (_versionInfo.szCSDVersion[1] == 'A') {
					version = "98 SE";
				} else {
					version = "98";
				}
			} else if (getMajor() == 4 && getMinor() == 0) {
				if (_versionInfo.szCSDVersion[1] == 'C'
						|| _versionInfo.szCSDVersion[1] == 'B') {
					version = "95 OSR2";
				} else {
					version = "95";
				}
			} else {
				throw new RuntimeException("Unsupported Windows 9x version: "
						+ _versionInfo.toString());
			}
		} else {
			throw new RuntimeException("Unsupported Windows platform: "
					+ _versionInfo.toString());
		}

		return version;
	}

	public OSVersionInfoEx(OSVERSIONINFOEX versionInfo) {
		_versionInfo = versionInfo;
	}
}
