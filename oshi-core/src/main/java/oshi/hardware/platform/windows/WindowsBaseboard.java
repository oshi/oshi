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

import oshi.hardware.common.AbstractBaseboard;
import oshi.util.platform.windows.WmiUtil;

/**
 * Baseboard data obtained from WMI
 *
 * @author widdis [at] gmail [dot] com
 */
public class WindowsBaseboard extends AbstractBaseboard {

    private static final long serialVersionUID = 1L;

    WindowsBaseboard() {
        init();
    }

    private void init() {

        final Map<String, List<String>> win32BaseBoard = WmiUtil.selectStringsFrom(null, "Win32_BaseBoard",
                "Manufacturer,Model,Version,SerialNumber", null);

        final List<String> baseboardManufacturers = win32BaseBoard.get("Manufacturer");
        if (baseboardManufacturers != null && !baseboardManufacturers.isEmpty()) {
            setManufacturer(baseboardManufacturers.get(0));
        }

        final List<String> baseboardModels = win32BaseBoard.get("Model");
        if (baseboardModels != null && !baseboardModels.isEmpty()) {
            setModel(baseboardModels.get(0));
        }

        final List<String> baseboardVersions = win32BaseBoard.get("Version");
        if (baseboardVersions != null && !baseboardVersions.isEmpty()) {
            setVersion(baseboardVersions.get(0));
        }

        final List<String> baseboardSerials = win32BaseBoard.get("SerialNumber");
        if (baseboardSerials != null && !baseboardSerials.isEmpty()) {
            setSerialNumber(baseboardSerials.get(0));
        }
    }
}
