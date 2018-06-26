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

    enum WmiProperty {
        MANUFACTURER(ValueType.STRING), //
        NAME(ValueType.STRING), //
        DESCRIPTION(ValueType.STRING), //
        VERSION(ValueType.STRING), //
        RELEASEDATE(ValueType.DATETIME);

        private ValueType type;

        public ValueType getType() {
            return this.type;
        }

        WmiProperty(ValueType type) {
            this.type = type;
        }
    }

    // BIOS
    private static final WmiProperty[] BIOS_PROPERTIES = new WmiProperty[] { WmiProperty.MANUFACTURER, WmiProperty.NAME,
            WmiProperty.DESCRIPTION, WmiProperty.VERSION, WmiProperty.RELEASEDATE };
    private static final String[] BIOS_STRINGS = new String[BIOS_PROPERTIES.length];
    static {
        for (int i = 0; i < BIOS_PROPERTIES.length; i++) {
            BIOS_STRINGS[i] = BIOS_PROPERTIES[i].name();
        }
    }
    private static final ValueType[] BIOS_TYPES = new ValueType[BIOS_PROPERTIES.length];
    static {
        for (int i = 0; i < BIOS_PROPERTIES.length; i++) {
            BIOS_TYPES[i] = BIOS_PROPERTIES[i].getType();
        }
    }

    WindowsFirmware() {
        init();
    }

    private void init() {

        final Map<String, List<Object>> win32BIOS = WmiUtil.selectObjectsFrom(null, "Win32_BIOS",
                BIOS_STRINGS, "where PrimaryBIOS=true", BIOS_TYPES);

        final List<Object> manufacturers = win32BIOS.get(WmiProperty.MANUFACTURER.name());
        if (manufacturers != null && manufacturers.size() == 1) {
            setManufacturer((String) manufacturers.get(0));
        }

        final List<Object> names = win32BIOS.get(WmiProperty.NAME.name());
        if (names != null && names.size() == 1) {
            setName((String) names.get(0));
        }

        final List<Object> descriptions = win32BIOS.get(WmiProperty.DESCRIPTION.name());
        if (descriptions != null && descriptions.size() == 1) {
            setDescription((String) descriptions.get(0));
        }

        final List<Object> version = win32BIOS.get(WmiProperty.VERSION.name());
        if (version != null && version.size() == 1) {
            setVersion((String) version.get(0));
        }

        final List<Object> releaseDate = win32BIOS.get(WmiProperty.RELEASEDATE.name());
        if (releaseDate != null && releaseDate.size() == 1) {
            setReleaseDate(Instant.ofEpochMilli((Long) releaseDate.get(0)).atZone(ZoneOffset.UTC).toLocalDate());
        }
    }
}
