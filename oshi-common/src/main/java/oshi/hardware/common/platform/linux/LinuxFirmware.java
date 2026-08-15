/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static oshi.util.Memoizer.memoize;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractFirmware;
import oshi.util.Constants;
import oshi.util.ExceptionUtil;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.driver.linux.Dmidecode;
import oshi.util.driver.linux.Sysfs;
import oshi.util.tuples.Pair;

/**
 * Firmware data obtained by sysfs.
 */
@Immutable
final class LinuxFirmware extends AbstractFirmware {
    // Jan 13 2013 16:24:29
    private static final DateTimeFormatter VCGEN_FORMATTER = DateTimeFormatter.ofPattern("MMM d uuuu HH:mm:ss",
            Locale.ENGLISH);

    private final Supplier<String> manufacturer = memoize(this::queryManufacturer);

    private final Supplier<String> description = memoize(this::queryDescription);

    private final Supplier<String> version = memoize(this::queryVersion);

    private final Supplier<String> releaseDate = memoize(this::queryReleaseDate);

    private final Supplier<String> name = memoize(this::queryName);

    private final Supplier<VcGenCmdStrings> vcGenCmd = memoize(LinuxFirmware::queryVcGenCmd);

    private final Supplier<Pair<@Nullable String, @Nullable String>> biosNameRev = memoize(Dmidecode::queryBiosNameRev);

    @Override
    public String getManufacturer() {
        return manufacturer.get();
    }

    @Override
    public String getDescription() {
        return description.get();
    }

    @Override
    public String getVersion() {
        return version.get();
    }

    @Override
    public String getReleaseDate() {
        return releaseDate.get();
    }

    @Override
    public String getName() {
        return name.get();
    }

    private String queryManufacturer() {
        String result = null;
        if ((result = Sysfs.queryBiosVendor()) == null && (result = vcGenCmd.get().getManufacturer()) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private String queryDescription() {
        String result = null;
        if ((result = Sysfs.queryBiosDescription()) == null && (result = vcGenCmd.get().getDescription()) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private String queryVersion() {
        String result = null;
        if ((result = Sysfs.queryBiosVersion(this.biosNameRev.get().getB())) == null
                && (result = vcGenCmd.get().getVersion()) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private String queryReleaseDate() {
        String result = null;
        if ((result = Sysfs.queryBiosReleaseDate()) == null && (result = vcGenCmd.get().getReleaseDate()) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private String queryName() {
        String result = null;
        if ((result = biosNameRev.get().getA()) == null && (result = vcGenCmd.get().getName()) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private static VcGenCmdStrings queryVcGenCmd() {
        return queryVcGenCmd(ExecutingCommand.runNative("vcgencmd version"));
    }

    /**
     * Parse vcgencmd version output for Raspberry Pi firmware info.
     *
     * @param output output of {@code vcgencmd version}
     * @return parsed firmware strings
     */
    static VcGenCmdStrings queryVcGenCmd(List<String> output) {
        String vcReleaseDate = null;
        String vcManufacturer = null;
        String vcVersion = null;

        if (output.size() >= 3) {
            // First line is date
            vcReleaseDate = ExceptionUtil.getOrDefault(
                    () -> DateTimeFormatter.ISO_LOCAL_DATE.format(VCGEN_FORMATTER.parse(output.get(0))),
                    Constants.UNKNOWN);
            // Second line is copyright
            String[] copyright = ParseUtil.whitespaces.split(output.get(1).trim(), -1);
            vcManufacturer = copyright.length > 0 && !copyright[copyright.length - 1].isEmpty()
                    ? copyright[copyright.length - 1]
                    : Constants.UNKNOWN;
            // Third line is version
            vcVersion = output.get(2).replace("version ", "");
            return new VcGenCmdStrings(vcReleaseDate, vcManufacturer, vcVersion, "RPi", "Bootloader");
        }
        return new VcGenCmdStrings(null, null, null, null, null);
    }

    static final class VcGenCmdStrings {
        private final @Nullable String releaseDate;
        private final @Nullable String manufacturer;
        private final @Nullable String version;
        private final @Nullable String name;
        private final @Nullable String description;

        VcGenCmdStrings(@Nullable String releaseDate, @Nullable String manufacturer, @Nullable String version,
                @Nullable String name, @Nullable String description) {
            this.releaseDate = releaseDate;
            this.manufacturer = manufacturer;
            this.version = version;
            this.name = name;
            this.description = description;
        }

        @Nullable
        String getReleaseDate() {
            return releaseDate;
        }

        @Nullable
        String getManufacturer() {
            return manufacturer;
        }

        @Nullable
        String getVersion() {
            return version;
        }

        @Nullable
        String getName() {
            return name;
        }

        @Nullable
        String getDescription() {
            return description;
        }
    }
}
