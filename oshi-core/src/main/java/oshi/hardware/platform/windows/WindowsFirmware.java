/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware.platform.windows;

import java.util.List;
import java.util.Map;

import org.threeten.bp.Instant;
import org.threeten.bp.ZoneOffset;

import oshi.hardware.common.AbstractFirmware;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.ValueType;

/**
 * Firmware data obtained from WMI
 *
 * @author SchiTho1 [at] Securiton AG
 */
final class WindowsFirmware extends AbstractFirmware {

    private static final long serialVersionUID = 1L;

    private static final ValueType[] BIOS_TYPES = { ValueType.STRING, ValueType.STRING, ValueType.STRING,
            ValueType.STRING, ValueType.DATETIME };

    WindowsFirmware() {
        init();
    }

    private void init() {

        final Map<String, List<Object>> win32BIOS = WmiUtil.selectObjectsFrom(null, "Win32_BIOS",
                "Manufacturer,Name,Description,Version,ReleaseDate", "where PrimaryBIOS=true", BIOS_TYPES);

        final List<Object> manufacturers = win32BIOS.get("Manufacturer");
        if (manufacturers != null && manufacturers.size() == 1) {
            setManufacturer((String) manufacturers.get(0));
        }

        final List<Object> names = win32BIOS.get("Name");
        if (names != null && names.size() == 1) {
            setName((String) names.get(0));
        }

        final List<Object> descriptions = win32BIOS.get("Description");
        if (descriptions != null && descriptions.size() == 1) {
            setDescription((String) descriptions.get(0));
        }

        final List<Object> version = win32BIOS.get("Version");
        if (version != null && version.size() == 1) {
            setVersion((String) version.get(0));
        }

        final List<Object> releaseDate = win32BIOS.get("ReleaseDate");
        if (releaseDate != null && releaseDate.size() == 1) {
            setReleaseDate(Instant.ofEpochMilli((Long) releaseDate.get(0)).atZone(ZoneOffset.UTC).toLocalDate());
        }
    }
}
