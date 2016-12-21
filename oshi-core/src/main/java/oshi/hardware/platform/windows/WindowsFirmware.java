/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.windows;

import oshi.hardware.common.AbstractFirmware;
import oshi.util.platform.windows.WmiUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: SchiTho1
 * Date: 14.10.2016
 * Time: 07:59
 * <p>
 * Copyright 2008-2013 - Securiton AG all rights reserved
 */
final class WindowsFirmware extends AbstractFirmware {

    WindowsFirmware() {

        init();
    }

    private void init() {

        final Map<String, List<Object>> win32BIOS = WmiUtil.selectObjectsFrom("root\\cimv2", "Win32_BIOS", "Manufacturer,Name,Description,Version,ReleaseDate", "where PrimaryBIOS=true", new WmiUtil.ValueType[] {
                WmiUtil.ValueType.STRING, WmiUtil.ValueType.STRING, WmiUtil.ValueType.STRING, WmiUtil.ValueType.STRING, WmiUtil.ValueType.DATETIME
        });

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
            setReleaseDate(new Date((Long)releaseDate.get(0)));
        }
    }
}
