/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.mac;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.hardware.common.AbstractFirmware;
import oshi.jna.platform.mac.IOKit.IOIterator;
import oshi.jna.platform.mac.IOKit.IORegistryEntry;
import oshi.jna.platform.mac.IOKitUtil;
import oshi.util.Constants;
import oshi.util.Util;

/**
 * Firmware data obtained from ioreg.
 */
final class MacFirmware extends AbstractFirmware {

    private final Supplier<EfiStrings> efi = memoize(this::queryEfi);

    @Override
    public String getManufacturer() {
        return efi.get().manufacturer;
    }

    @Override
    public String getName() {
        return efi.get().name;
    }

    @Override
    public String getDescription() {
        return efi.get().description;
    }

    @Override
    public String getVersion() {
        return efi.get().version;
    }

    @Override
    public String getReleaseDate() {
        return efi.get().releaseDate;
    }

    private EfiStrings queryEfi() {
        String releaseDate = null;
        String manufacturer = null;
        String version = null;
        String name = null;
        String description = null;

        IORegistryEntry platformExpert = IOKitUtil.getMatchingService("IOPlatformExpertDevice");
        if (platformExpert != null) {
            IOIterator iter = platformExpert.getChildIterator("IODeviceTree");
            if (iter != null) {
                IORegistryEntry entry = iter.next();
                while (entry != null) {
                    switch (entry.getName()) {
                    case "rom":
                        byte[] data = entry.getByteArrayProperty("vendor");
                        if (data != null) {
                            manufacturer = new String(data);
                        }
                        data = entry.getByteArrayProperty("version");
                        if (data != null) {
                            version = new String(data);
                        }
                        data = entry.getByteArrayProperty("release-date");
                        if (data != null) {
                            releaseDate = new String(data);
                        }
                        break;
                    case "chosen":
                        data = entry.getByteArrayProperty("booter-name");
                        if (data != null) {
                            name = new String(data);
                        }
                        break;
                    case "efi":
                        data = entry.getByteArrayProperty("firmware-abi");
                        if (data != null) {
                            description = new String(data);
                        }
                        break;
                    default:
                        break;
                    }
                    entry.release();
                    entry = iter.next();
                }
                iter.release();
            }
            platformExpert.release();
        }
        return new EfiStrings(releaseDate, manufacturer, version, name, description);
    }

    private static final class EfiStrings {
        private final String releaseDate;
        private final String manufacturer;
        private final String version;
        private final String name;
        private final String description;

        private EfiStrings(String releaseDate, String manufacturer, String version, String name, String description) {
            this.releaseDate = Util.isBlank(releaseDate) ? Constants.UNKNOWN : releaseDate;
            this.manufacturer = Util.isBlank(manufacturer) ? Constants.UNKNOWN : manufacturer;
            this.version = Util.isBlank(version) ? Constants.UNKNOWN : version;
            this.name = Util.isBlank(name) ? Constants.UNKNOWN : name;
            this.description = Util.isBlank(description) ? Constants.UNKNOWN : description;
        }
    }
}
