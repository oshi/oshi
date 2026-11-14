/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.bsd;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.ApplicationInfo;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Queries the packages installed on OpenBSD and NetBSD from {@code pkg_info}, which both systems' package tools
 * provide.
 */
@ThreadSafe
public final class PkgInfoInstalledApps {

    /** OpenBSD's package database: one directory per installed package, named for it. */
    public static final String OPENBSD_PKG_DB = "/var/db/pkg";
    /** NetBSD's package database since NetBSD 9, the {@code PKG_DBDIR} pkgsrc uses by default. */
    public static final String NETBSD_PKG_DB = "/usr/pkg/pkgdb";

    /** A {@code pkg_info} line: the full package name, then its comment after one or more spaces. */
    private static final Pattern PKG_INFO_LINE = Pattern.compile("(\\S+)\\s+(.*)");
    /**
     * OpenBSD's rule from packages-specs(7): the version starts at the first dash followed by a digit, and may carry a
     * flavor after it, as in {@code vim-9.1.1-no_x11}.
     */
    private static final Pattern OPENBSD_NAME = Pattern.compile("(.+?)-(\\d.*)");

    private PkgInfoInstalledApps() {
    }

    /**
     * Queries {@code pkg_info} for the installed packages. Neither the command nor the package database needs
     * privilege.
     *
     * @param pkgDb   the package database directory, {@link #OPENBSD_PKG_DB} or {@link #NETBSD_PKG_DB}
     * @param openBsd whether to split names by OpenBSD's rule, rather than NetBSD's
     * @return the installed applications
     */
    public static List<ApplicationInfo> queryInstalledApps(String pkgDb, boolean openBsd) {
        return parsePkgInfo(ExecutingCommand.runNative("pkg_info"), openBsd,
                pkg -> new File(pkgDb, pkg).lastModified());
    }

    /**
     * Parses the output of {@code pkg_info}. Package-private for testing.
     * <p>
     * Each line is a full package name, {@code name-version}, and its one-line comment. NetBSD's pkgsrc never puts a
     * dash in a version, so the name ends at the last dash; OpenBSD's versions may carry a flavor after a further dash,
     * so the name ends at the first dash followed by a digit. Neither tool records an install time, but each installs a
     * package by creating its directory in the package database, so the directory's modification time is the timestamp.
     *
     * @param lines       the lines emitted by {@code pkg_info}
     * @param openBsd     whether to split names by OpenBSD's rule, rather than NetBSD's
     * @param installTime the modification time in milliseconds of a package's database directory, or 0 if it has none
     * @return the installed applications
     */
    static List<ApplicationInfo> parsePkgInfo(List<String> lines, boolean openBsd, ToLongFunction<String> installTime) {
        List<ApplicationInfo> apps = new ArrayList<>();
        for (String line : lines) {
            Matcher m = PKG_INFO_LINE.matcher(line.trim());
            if (!m.matches()) {
                continue;
            }
            String pkg = m.group(1);
            String name = pkg;
            String version = "";
            if (openBsd) {
                Matcher n = OPENBSD_NAME.matcher(pkg);
                if (n.matches()) {
                    name = n.group(1);
                    version = n.group(2);
                }
            } else {
                int dash = pkg.lastIndexOf('-');
                if (dash > 0) {
                    name = pkg.substring(0, dash);
                    version = pkg.substring(dash + 1);
                }
            }
            Map<String, String> additionalInfo = new LinkedHashMap<>();
            additionalInfo.put("description", ParseUtil.getStringValueOrUnknown(m.group(2).trim()));
            apps.add(new ApplicationInfo(name, version, "", installTime.applyAsLong(pkg), additionalInfo));
        }
        return apps;
    }
}
