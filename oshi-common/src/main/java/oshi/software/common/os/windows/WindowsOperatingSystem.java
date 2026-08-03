/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.windows;

import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32OperatingSystem.OSVersionProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.OperatingSystem.OSVersionInfo;
import oshi.util.Constants;
import oshi.util.GlobalConfig;
import oshi.util.tuples.Pair;

/**
 * Common base class for Windows operating system implementations.
 */
@ThreadSafe
public abstract class WindowsOperatingSystem extends AbstractOperatingSystem {

    /**
     * Default constructor.
     */
    protected WindowsOperatingSystem() {
    }

    /** Whether to check thread states to determine if a process is suspended. */
    protected static final boolean USE_PROCSTATE_SUSPENDED = GlobalConfig
            .get(GlobalConfig.OSHI_OS_WINDOWS_PROCSTATE_SUSPENDED, false);

    @Override
    protected String queryManufacturer() {
        return "Microsoft";
    }

    /**
     * Queries WMI for the operating system version, service pack, suite mask, and build number.
     *
     * @return The {@code Win32_OperatingSystem} query result
     */
    protected abstract WmiResult<OSVersionProperty> queryOsVersion();

    @Override
    protected Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        String servicePack = "";
        int suiteMask = 0;
        String buildNumber = "";
        WmiResult<OSVersionProperty> versionInfo = queryOsVersion();
        if (versionInfo.getResultCount() > 0) {
            servicePack = WmiUtil.getString(versionInfo, OSVersionProperty.CSDVERSION, 0);
            suiteMask = WmiUtil.getUint32(versionInfo, OSVersionProperty.SUITEMASK, 0);
            buildNumber = WmiUtil.getString(versionInfo, OSVersionProperty.BUILDNUMBER, 0);
        }
        return parseVersionInfo(System.getProperty("os.name"), servicePack, suiteMask, buildNumber);
    }

    /**
     * Assembles the family and version information from the raw values reported by the JDK and by WMI.
     *
     * @param osName      The value of the {@code os.name} system property
     * @param servicePack The service pack name, empty or {@link Constants#UNKNOWN} if none
     * @param suiteMask   The suite mask bitmask
     * @param buildNumber The build number reported by WMI
     * @return A pair of the family name and the version information
     */
    static Pair<String, OSVersionInfo> parseVersionInfo(String osName, String servicePack, int suiteMask,
            String buildNumber) {
        String version = osName.startsWith("Windows ") ? osName.substring(8) : osName;
        if (!servicePack.isEmpty() && !Constants.UNKNOWN.equals(servicePack)) {
            version = version + " " + servicePack.replace("Service Pack ", "SP");
        }
        return new Pair<>("Windows",
                new OSVersionInfo(resolveVersionAlias(version, buildNumber), parseCodeName(suiteMask), buildNumber));
    }

    /**
     * Maps the version name reported by the JDK to the name of the release that actually shipped with the given build
     * number. Older JDKs predate Windows 11 and the Server releases after 2016, so {@code os.name} reports them under
     * the name of the last release the JDK knew about.
     *
     * @param version     The version name derived from {@code os.name}
     * @param buildNumber The build number reported by WMI
     * @return The version name of the release matching {@code buildNumber}
     */
    static String resolveVersionAlias(String version, String buildNumber) {
        if ("10".equals(version) && buildNumber.compareTo("22000") >= 0) {
            return "11";
        }
        if ("Server 2016".equals(version) && buildNumber.compareTo("17762") > 0) {
            version = "Server 2019";
        }
        if ("Server 2019".equals(version) && buildNumber.compareTo("20347") > 0) {
            version = "Server 2022";
        }
        if ("Server 2022".equals(version) && buildNumber.compareTo("26039") > 0) {
            version = "Server 2025";
        }
        return version;
    }

    /**
     * Gets suites available on the system and return as a codename.
     *
     * @param suiteMask The suite mask bitmask
     * @return Suites
     */
    protected static String parseCodeName(int suiteMask) {
        List<String> suites = new ArrayList<>();
        if ((suiteMask & 0x00000002) != 0) {
            suites.add("Enterprise");
        }
        if ((suiteMask & 0x00000004) != 0) {
            suites.add("BackOffice");
        }
        if ((suiteMask & 0x00000008) != 0) {
            suites.add("Communications Server");
        }
        if ((suiteMask & 0x00000080) != 0) {
            suites.add("Datacenter");
        }
        if ((suiteMask & 0x00000200) != 0) {
            suites.add("Home");
        }
        if ((suiteMask & 0x00000400) != 0) {
            suites.add("Web Server");
        }
        if ((suiteMask & 0x00002000) != 0) {
            suites.add("Storage Server");
        }
        if ((suiteMask & 0x00004000) != 0) {
            suites.add("Compute Cluster");
        }
        if ((suiteMask & 0x00008000) != 0) {
            suites.add("Home Server");
        }
        return String.join(",", suites);
    }
}
