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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.platform.win32.COM.WbemcliUtil; // NOSONAR squid:S1191

import oshi.hardware.common.AbstractSoundCard;
import oshi.util.platform.windows.WmiUtil;

/**
 * Sound Card data obtained by Wmi Queries for Windows.
 *
 * @author : BilalAM
 */
public class WindowsSoundCard extends AbstractSoundCard {
    enum SoundCardName {
        MANUFACTURER, NAME;
    }

    enum SoundCardKernel {
        DRIVERPROVIDERNAME, DRIVERNAME, DRIVERVERSION, DEVICENAME;
    }

    private static final Map<String, String> NAME_MAP = new HashMap<>();
    private static final String DRIVER_QUERY;

    /**
     * Runs the Win32_SoundDevice query only once and initializes a Map with
     * Name as key and Manufacturer as Value
     *
     * Also calls the createClause() method to build our WHERE clause query only
     * once
     */
    static {
        WbemcliUtil.WmiQuery<SoundCardName> soundCardQuery = new WbemcliUtil.WmiQuery<>("Win32_SoundDevice",
                SoundCardName.class);
        WbemcliUtil.WmiResult<SoundCardName> soundCardResult = WmiUtil.queryWMI(soundCardQuery);
        for (int i = 0; i < soundCardResult.getResultCount(); i++) {
            NAME_MAP.put(WmiUtil.getString(soundCardResult, SoundCardName.NAME, i),
                    WmiUtil.getString(soundCardResult, SoundCardName.MANUFACTURER, i));
        }
        DRIVER_QUERY = createClause();
    }

    public WindowsSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    /**
     * Creates our Win32_PnPSignedDevice query with the WHERE clause taking the
     * attributes from our map.
     *
     * @return The WHERE clause
     */
    private static String createClause() {
        StringBuilder sb = new StringBuilder("Win32_PnPSignedDriver");
        boolean first = true;
        for (String key : NAME_MAP.keySet()) {
            if (first) {
                sb.append(" WHERE");
                first = false;
            } else {
                sb.append(" OR");
            }
            sb.append(" DeviceName LIKE \"%").append(key).append("%\"");
        }
        return sb.toString();
    }

    /**
     * Short method to build our kernel string.
     *
     * @param index
     *            The row to fetch data.
     * @param cardKernelQueryResult
     *            The result to take data from.
     * @return The kernel string.
     */
    private static String getAudioCardKernelVersion(int index,
            WbemcliUtil.WmiResult<SoundCardKernel> cardKernelQueryResult) {
        String audioCardKernel;
        audioCardKernel = WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DRIVERPROVIDERNAME, index) + " "
                + WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DEVICENAME, index) + " "
                + WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DRIVERNAME, index) + " "
                + WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DRIVERVERSION, index);
        return audioCardKernel;
    }

    /**
     * Does the following :
     * <ul>
     * <li>Creates and runs our 'built where clause' query</li>
     * <li>Then iterates over the key values of our map and compares the key
     * with the DeviceName attribute returned by our where-clause query.If its a
     * match then we create our SoundCard object</li>
     * </ul>
     * <br>
     * NOTE : The reason why the codec name is same as the card name is because
     * windows does not provide the name of the codec chip but sometimes the
     * name of the card returned is infact the name of the codec chip also.
     * Example : Realtek ALC887 HD Audio Device
     *
     * @return List of sound cards
     */
    public static List<WindowsSoundCard> getSoundCards() {
        WbemcliUtil.WmiQuery<SoundCardKernel> cardKernelQuery = new WbemcliUtil.WmiQuery<>(DRIVER_QUERY,
                SoundCardKernel.class);
        WbemcliUtil.WmiResult<SoundCardKernel> cardKernelQueryResult = WmiUtil.queryWMI(cardKernelQuery);

        List<WindowsSoundCard> soundCards = new ArrayList<>();
        for (int i = 0; i < cardKernelQueryResult.getResultCount(); i++) {
            // If the map has a key that is equal to the value returned by
            // cardKernelQuery
            if (NAME_MAP.containsKey(WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DEVICENAME, i))) {
                // then build a sound card by extracting values from
                // cardKernelQuery
                soundCards.add(new WindowsSoundCard(getAudioCardKernelVersion(i, cardKernelQueryResult),
                        WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DRIVERPROVIDERNAME, i) + " "
                                + WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DEVICENAME, i),
                        WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DEVICENAME, i)));
            }
        }
        return soundCards;
    }
}
