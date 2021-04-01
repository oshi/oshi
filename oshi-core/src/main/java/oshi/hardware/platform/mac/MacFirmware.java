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
package oshi.hardware.platform.mac;

import static oshi.util.Memoizer.memoize;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractFirmware;
import oshi.util.Constants;
import oshi.util.Util;
import oshi.util.tuples.Quintet;

/**
 * Firmware data obtained from ioreg.
 */
@Immutable
final class MacFirmware extends AbstractFirmware {

    private final Supplier<Quintet<String, String, String, String, String>> manufNameDescVersRelease = memoize(
            MacFirmware::queryEfi);

    @Override
    public String getManufacturer() {
        return manufNameDescVersRelease.get().getA();
    }

    @Override
    public String getName() {
        return manufNameDescVersRelease.get().getB();
    }

    @Override
    public String getDescription() {
        return manufNameDescVersRelease.get().getC();
    }

    @Override
    public String getVersion() {
        return manufNameDescVersRelease.get().getD();
    }

    @Override
    public String getReleaseDate() {
        return manufNameDescVersRelease.get().getE();
    }

    private static Quintet<String, String, String, String, String> queryEfi() {
        String manufacturer = null;
        String name = null;
        String description = null;
        String version = null;
        String releaseDate = null;

        IORegistryEntry platformExpert = IOKitUtil.getMatchingService("IOPlatformExpertDevice");
        byte[] data;
        if (platformExpert != null) {
            IOIterator iter = platformExpert.getChildIterator("IODeviceTree");
            if (iter != null) {
                IORegistryEntry entry = iter.next();
                while (entry != null) {
                    switch (entry.getName()) {
                    case "rom":
                        data = entry.getByteArrayProperty("vendor");
                        if (data != null) {
                            manufacturer = Native.toString(data, StandardCharsets.UTF_8);
                        }
                        data = entry.getByteArrayProperty("version");
                        if (data != null) {
                            version = Native.toString(data, StandardCharsets.UTF_8);
                        }
                        data = entry.getByteArrayProperty("release-date");
                        if (data != null) {
                            releaseDate = Native.toString(data, StandardCharsets.UTF_8);
                        }
                        break;
                    case "chosen":
                        data = entry.getByteArrayProperty("booter-name");
                        if (data != null) {
                            name = Native.toString(data, StandardCharsets.UTF_8);
                        }
                        break;
                    case "efi":
                        data = entry.getByteArrayProperty("firmware-abi");
                        if (data != null) {
                            description = Native.toString(data, StandardCharsets.UTF_8);
                        }
                        break;
                    default:
                        if (Util.isBlank(name)) {
                            name = entry.getStringProperty("IONameMatch");
                        }
                        break;
                    }
                    entry.release();
                    entry = iter.next();
                }
                iter.release();
            }
            if (Util.isBlank(manufacturer)) {
                data = platformExpert.getByteArrayProperty("manufacturer");
                if (data != null) {
                    manufacturer = Native.toString(data, StandardCharsets.UTF_8);
                }
            }
            if (Util.isBlank(version)) {
                data = platformExpert.getByteArrayProperty("target-type");
                if (data != null) {
                    version = Native.toString(data, StandardCharsets.UTF_8);
                }
            }
            if (Util.isBlank(name)) {
                data = platformExpert.getByteArrayProperty("device_type");
                if (data != null) {
                    name = Native.toString(data, StandardCharsets.UTF_8);
                }
            }
            platformExpert.release();
        }
        return new Quintet<>(Util.isBlank(manufacturer) ? Constants.UNKNOWN : manufacturer,
                Util.isBlank(name) ? Constants.UNKNOWN : name,
                Util.isBlank(description) ? Constants.UNKNOWN : description,
                Util.isBlank(version) ? Constants.UNKNOWN : version,
                Util.isBlank(releaseDate) ? Constants.UNKNOWN : releaseDate);
    }
}
