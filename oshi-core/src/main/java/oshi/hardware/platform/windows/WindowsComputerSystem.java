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

import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.ValueType;
import oshi.util.platform.windows.WmiUtil.WmiProperty;
import oshi.util.platform.windows.WmiUtil.WmiQuery;
import oshi.util.platform.windows.WmiUtil.WmiResult;

/**
 * Hardware data obtained from WMI
 *
 * @author SchiTho1 [at] Securiton AG
 * @author widdis [at] gmail [dot] com
 */
final class WindowsComputerSystem extends AbstractComputerSystem {

    private static final long serialVersionUID = 1L;

    enum ComputerSystemProperty implements WmiProperty {
        MANUFACTURER(ValueType.STRING), //
        MODEL(ValueType.STRING);

        private ValueType type;

        ComputerSystemProperty(ValueType type) {
            this.type = type;
        }

        @Override
        public ValueType getType() {
            return this.type;
        }
    }

    enum BiosProperty implements WmiProperty {
        SERIALNUMBER(ValueType.STRING);

        private ValueType type;

        BiosProperty(ValueType type) {
            this.type = type;
        }

        @Override
        public ValueType getType() {
            return this.type;
        }
    }

    enum CsProductProperty implements WmiProperty {
        IDENTIFYINGNUMBER(ValueType.STRING);

        private ValueType type;

        CsProductProperty(ValueType type) {
            this.type = type;
        }

        @Override
        public ValueType getType() {
            return this.type;
        }
    }

    private static final WmiQuery<CsProductProperty> IDENTIFYINGNUMBER_QUERY = WmiUtil.createQuery("Win32_Csproduct",
            CsProductProperty.class);

    private String systemSerialNumber = "";

    WindowsComputerSystem() {
        init();
    }

    private void init() {
        WmiQuery<ComputerSystemProperty> computerSystemQuery = WmiUtil.createQuery("Win32_ComputerSystem",
                ComputerSystemProperty.class);
        WmiResult<ComputerSystemProperty> win32ComputerSystem = WmiUtil.queryWMI(computerSystemQuery);
        if (win32ComputerSystem.getResultCount() > 0) {
            setManufacturer((String) win32ComputerSystem.get(ComputerSystemProperty.MANUFACTURER).get(0));
            setModel((String) win32ComputerSystem.get(ComputerSystemProperty.MODEL).get(0));
        }

        setSerialNumber(getSystemSerialNumber());
        setFirmware(new WindowsFirmware());
        setBaseboard(new WindowsBaseboard());
    }

    private String getSystemSerialNumber() {
        if (!"".equals(this.systemSerialNumber)) {
            return this.systemSerialNumber;
        }
        // This should always work
        WmiQuery<BiosProperty> serialNumberQuery = WmiUtil.createQuery("Win32_BIOS where PrimaryBIOS=true",
                BiosProperty.class);
        WmiResult<BiosProperty> serialNumber = WmiUtil.queryWMI(serialNumberQuery);
        if (serialNumber.getResultCount() > 0) {
            this.systemSerialNumber = (String) serialNumber.get(BiosProperty.SERIALNUMBER).get(0);
        }
        // If the above doesn't work, this might
        if (!"".equals(this.systemSerialNumber)) {
            WmiResult<CsProductProperty> identifyingNumber = WmiUtil.queryWMI(IDENTIFYINGNUMBER_QUERY);
            if (identifyingNumber.getResultCount() > 0) {
                this.systemSerialNumber = (String) identifyingNumber.get(CsProductProperty.IDENTIFYINGNUMBER).get(0);
            }
        }
        // Nothing worked. Default.
        if (!"".equals(this.systemSerialNumber)) {
            this.systemSerialNumber = "unknown";
        }
        return this.systemSerialNumber;
    }
}
