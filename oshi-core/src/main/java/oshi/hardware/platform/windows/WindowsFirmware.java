/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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

import oshi.hardware.common.AbstractFirmware;
import oshi.jna.platform.windows.WbemcliUtil.WmiQuery;
import oshi.jna.platform.windows.WbemcliUtil.WmiResult;
import oshi.util.platform.windows.WmiUtil;

/**
 * Firmware data obtained from WMI
 *
 * @author SchiTho1 [at] Securiton AG
 */
final class WindowsFirmware extends AbstractFirmware {

    private static final long serialVersionUID = 1L;

    enum BiosProperty {
        MANUFACTURER, NAME, DESCRIPTION, VERSION, RELEASEDATE;
    }

    WindowsFirmware() {
        init();
    }

    private void init() {
        WmiQuery<BiosProperty> biosQuery = new WmiQuery<>("Win32_BIOS where PrimaryBIOS=true", BiosProperty.class);

        WmiResult<BiosProperty> win32BIOS = WmiUtil.queryWMI(biosQuery);
        if (win32BIOS.getResultCount() > 0) {
            setManufacturer(WmiUtil.getString(win32BIOS, BiosProperty.MANUFACTURER, 0));
            setName(WmiUtil.getString(win32BIOS, BiosProperty.NAME, 0));
            setDescription(WmiUtil.getString(win32BIOS, BiosProperty.DESCRIPTION, 0));
            setVersion(WmiUtil.getString(win32BIOS, BiosProperty.VERSION, 0));
            setReleaseDate(WmiUtil.getDateString(win32BIOS, BiosProperty.RELEASEDATE, 0));
        }
    }
}
