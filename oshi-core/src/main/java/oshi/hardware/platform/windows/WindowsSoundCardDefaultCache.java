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

import com.sun.jna.platform.win32.COM.WbemcliUtil;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Default static and thread-safe {@link WindowsSoundCardCache} implementation
 * which queries the Win32_SoundDevice only once.
 *
 * @see WindowsSoundCardCache
 * @see WindowsHardwareAbstractionLayer#createWindowsSoundCardCache()
 */
public class WindowsSoundCardDefaultCache extends WindowsSoundCardCache {

    private static final Object LOCK = new Object();

    //@GuardedBy("LOCK")
    private static volatile Map<String, String> MANUFACTURER_BY_NAME;

    //@GuardedBy("LOCK")
    private static volatile String DRIVER_QUERY;

    public WindowsSoundCardDefaultCache(WmiQueryHandler queryHandler) {
        super(queryHandler);
    }

    @Override
    protected String getDriverQuery() {
        if (DRIVER_QUERY == null) {
            synchronized (LOCK) {
                if (DRIVER_QUERY == null) {
                    buildCache();
                }
            }
        }
        return DRIVER_QUERY;
    }

    @Override
    protected Map<String, String> getManufacturerByName() {
        if (MANUFACTURER_BY_NAME == null) {
            synchronized (LOCK) {
                if (MANUFACTURER_BY_NAME == null) {
                    buildCache();
                }
            }
        }
        return MANUFACTURER_BY_NAME;
    }

    private void buildCache() {
        WbemcliUtil.WmiQuery<WindowsSoundCard.SoundCardName> soundCardQuery = new WbemcliUtil.WmiQuery<>("Win32_SoundDevice",
                WindowsSoundCard.SoundCardName.class);
        WbemcliUtil.WmiResult<WindowsSoundCard.SoundCardName> soundCardResult = queryHandler.queryWMI(soundCardQuery);
        Map<String, String> manufacturerByName = new HashMap<>(soundCardResult.getResultCount());
        for (int i = 0; i < soundCardResult.getResultCount(); i++) {
            manufacturerByName.put(WmiUtil.getString(soundCardResult, WindowsSoundCard.SoundCardName.NAME, i),
                    WmiUtil.getString(soundCardResult, WindowsSoundCard.SoundCardName.MANUFACTURER, i));
        }
        MANUFACTURER_BY_NAME = Collections.unmodifiableMap(manufacturerByName);
        DRIVER_QUERY = createClause(manufacturerByName);
    }
}
