/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.linux;

import static oshi.util.Memoizer.memoize;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.linux.Dmidecode;
import oshi.driver.linux.Sysfs;
import oshi.hardware.common.AbstractFirmware;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
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

    private final Supplier<Pair<String, String>> biosNameRev = memoize(Dmidecode::queryBiosNameRev);

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
        if ((result = Sysfs.queryBiosVendor()) == null && (result = vcGenCmd.get().manufacturer) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private String queryDescription() {
        String result = null;
        if ((result = Sysfs.queryBiosDescription()) == null && (result = vcGenCmd.get().description) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private String queryVersion() {
        String result = null;
        if ((result = Sysfs.queryBiosVersion(this.biosNameRev.get().getB())) == null
                && (result = vcGenCmd.get().version) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private String queryReleaseDate() {
        String result = null;
        if ((result = Sysfs.queryBiosReleaseDate()) == null && (result = vcGenCmd.get().releaseDate) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private String queryName() {
        String result = null;
        if ((result = biosNameRev.get().getA()) == null && (result = vcGenCmd.get().name) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private static VcGenCmdStrings queryVcGenCmd() {
        String vcReleaseDate = null;
        String vcManufacturer = null;
        String vcVersion = null;

        List<String> vcgencmd = ExecutingCommand.runNative("vcgencmd version");
        if (vcgencmd.size() >= 3) {
            // First line is date
            try {
                vcReleaseDate = DateTimeFormatter.ISO_LOCAL_DATE.format(VCGEN_FORMATTER.parse(vcgencmd.get(0)));
            } catch (DateTimeParseException e) {
                vcReleaseDate = Constants.UNKNOWN;
            }
            // Second line is copyright
            String[] copyright = ParseUtil.whitespaces.split(vcgencmd.get(1));
            vcManufacturer = copyright[copyright.length - 1];
            // Third line is version
            vcVersion = vcgencmd.get(2).replace("version ", "");
            return new VcGenCmdStrings(vcReleaseDate, vcManufacturer, vcVersion, "RPi", "Bootloader");
        }
        return new VcGenCmdStrings(null, null, null, null, null);
    }

    private static final class VcGenCmdStrings {
        private final String releaseDate;
        private final String manufacturer;
        private final String version;
        private final String name;
        private final String description;

        private VcGenCmdStrings(String releaseDate, String manufacturer, String version, String name,
                String description) {
            this.releaseDate = releaseDate;
            this.manufacturer = manufacturer;
            this.version = version;
            this.name = name;
            this.description = description;
        }
    }
}
