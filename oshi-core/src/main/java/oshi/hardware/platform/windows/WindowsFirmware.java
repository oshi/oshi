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

import java.util.Calendar;

import oshi.hardware.common.AbstractFirmware;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.ValueType;
import oshi.util.platform.windows.WmiUtil.WmiProperty;
import oshi.util.platform.windows.WmiUtil.WmiQuery;
import oshi.util.platform.windows.WmiUtil.WmiResult;

/**
 * Firmware data obtained from WMI
 *
 * @author SchiTho1 [at] Securiton AG
 */
final class WindowsFirmware extends AbstractFirmware {

    private static final long serialVersionUID = 1L;

    enum BiosProperty implements WmiProperty {
        MANUFACTURER(ValueType.STRING), //
        NAME(ValueType.STRING), //
        DESCRIPTION(ValueType.STRING), //
        VERSION(ValueType.STRING), //
        RELEASEDATE(ValueType.DATETIME);

        private ValueType type;

        BiosProperty(ValueType type) {
            this.type = type;
        }

        @Override
        public ValueType getType() {
            return this.type;
        }
    }

    WindowsFirmware() {
        init();
    }

    private void init() {
        WmiQuery<BiosProperty> biosQuery = WmiUtil.createQuery("Win32_BIOS where PrimaryBIOS=true", BiosProperty.class);

        WmiResult<BiosProperty> win32BIOS = WmiUtil.queryWMI(biosQuery);
        if (win32BIOS.getResultCount() > 0) {
            setManufacturer((String) win32BIOS.get(BiosProperty.MANUFACTURER).get(0));
            setName((String) win32BIOS.get(BiosProperty.NAME).get(0));
            setDescription((String) win32BIOS.get(BiosProperty.DESCRIPTION).get(0));
            setVersion((String) win32BIOS.get(BiosProperty.VERSION).get(0));
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis((Long) win32BIOS.get(BiosProperty.RELEASEDATE).get(0));
            setReleaseDate(String.format("%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
                    c.get(Calendar.DATE)));
        }
    }
}
