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

import oshi.hardware.common.AbstractBaseboard;
import oshi.jna.platform.windows.WbemcliUtil.WmiQuery;
import oshi.jna.platform.windows.WbemcliUtil.WmiResult;
import oshi.util.platform.windows.WmiUtil;

/**
 * Baseboard data obtained from WMI
 *
 * @author widdis [at] gmail [dot] com
 */
public class WindowsBaseboard extends AbstractBaseboard {

    private static final long serialVersionUID = 1L;

    enum BaseboardProperty {
        MANUFACTURER, MODEL, VERSION, SERIALNUMBER;
    }

    WindowsBaseboard() {
        init();
    }

    private void init() {
        WmiQuery<BaseboardProperty> baseboardQuery = new WmiQuery<>("Win32_BaseBoard", BaseboardProperty.class);
        WmiResult<BaseboardProperty> win32BaseBoard = WmiUtil.queryWMI(baseboardQuery);
        if (win32BaseBoard.getResultCount() > 0) {
            setManufacturer(WmiUtil.getString(win32BaseBoard, BaseboardProperty.MANUFACTURER, 0));
            setModel(WmiUtil.getString(win32BaseBoard, BaseboardProperty.MODEL, 0));
            setVersion(WmiUtil.getString(win32BaseBoard, BaseboardProperty.VERSION, 0));
            setSerialNumber(WmiUtil.getString(win32BaseBoard, BaseboardProperty.SERIALNUMBER, 0));
        }
    }
}
