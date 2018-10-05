/**
 * Oshi (https://github.com/oshi/oshi)
 * <p>
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p>
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 * <p>
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware.platform.windows;

import oshi.hardware.common.AbstractSoundCard;
import oshi.jna.platform.windows.WbemcliUtil;
import oshi.util.platform.windows.WmiUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sound Card data obtained by Wmi Queries for Windows.
 *
 * @author : BilalAM
 */
public class WindowsSoundCard extends AbstractSoundCard {
    public WindowsSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    enum SoundCardName {
        MANUFACTURER, NAME;
    }

    enum SoundCardKernel {
        DRIVERPROVIDERNAME, DRIVERNAME, DRIVERVERSION, DEVICENAME;
    }

    private static Map<String, String> NAME_MAP = new HashMap<>();
    private static String driverQueryClause = "";

    /**
     * Runs the Win32_SoundDevice query only once and initializes
     * a Map with Name as key and Manufacturer as Value
     *
     * Also calls the createClause() method to build our WHERE clause query only once
     */
    static {
        WbemcliUtil.WmiQuery<SoundCardName> SOUND_CARD_QUERY = new WbemcliUtil.WmiQuery<>("Win32_SoundDevice",SoundCardName.class);
        WbemcliUtil.WmiResult<SoundCardName> SOUND_CARD_QUERY_RESULT = WmiUtil.queryWMI(SOUND_CARD_QUERY);
        for (int i = 0; i < SOUND_CARD_QUERY_RESULT.getResultCount(); i++) {
            NAME_MAP.put(WmiUtil.getString(SOUND_CARD_QUERY_RESULT, SoundCardName.NAME, i)
                    , WmiUtil.getString(SOUND_CARD_QUERY_RESULT, SoundCardName.MANUFACTURER, i));
        }
        createClause(NAME_MAP);

    }

    /**
     * Creates our Win32_PnPSignedDevice query with the WHERE clause taking the attributes
     * from our map.
     * @param map
     *       The map whose keys will be used inside the Where Clause.
     */
    private static void createClause(Map<String, String> map) {
        driverQueryClause = "Win32_PnPSignedDriver WHERE DeviceName LIKE ";
        boolean isEnd = true;
        for (String key : map.keySet()) {
            if (isEnd) {
                driverQueryClause += "\"%" + key + "%\"";
                isEnd = false;
            } else {
                driverQueryClause += " OR DeviceName LIKE";
                driverQueryClause += "\"%" + key + "%\"";
            }
        }
    }


    /**
     * Short method to build our kernel string.
     * @param index
     *       The row to fetch data.
     * @param cardKernelQueryResult
     *       The result to take data from.
     * @return
     *       The kernel string.
     */
    private static String getAudioCardKernelVersion(int index, WbemcliUtil.WmiResult cardKernelQueryResult) {
        String audioCardKernel;
        audioCardKernel =
                WmiUtil.getString(cardKernelQueryResult,SoundCardKernel.DRIVERPROVIDERNAME, index) + " "
                        + WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DEVICENAME, index) + " "
                        + WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DRIVERNAME, index) + " "
                        + WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DRIVERVERSION, index);
        return audioCardKernel;
    }


    /**
     * Does the following :
     * <ul>
     *     <li>Creates and runs our 'built where clause' query</li>
     *     <li>Then iterates over the key values of our map and compares the key
     *     with the DeviceName attribute returned by our where-clause query.If its a
     *     match then we create our SoundCard object
     *     </li>
     * </ul>
     * <br>
     * NOTE : The reason why the codec name is same as the card name is because windows does not provide
     * the name of the codec chip but sometimes the name of the card returned is infact the name of the codec
     * chip also.
     * Example : Realtek ALC887 HD Audio Device
     * @return List of sound cards
     */
    public static List<WindowsSoundCard> getSoundCards() {
        WbemcliUtil.WmiQuery<SoundCardKernel> cardKernelQuery = new WbemcliUtil.WmiQuery<>(driverQueryClause, SoundCardKernel.class);
        WbemcliUtil.WmiResult<SoundCardKernel> cardKernelQueryResult = WmiUtil.queryWMI(cardKernelQuery);

        List<WindowsSoundCard> soundCards = new ArrayList<>();
        for (int i = 0; i < cardKernelQueryResult.getResultCount(); i++) {
            // If the map has a key that is equal to the value returned by cardKernelQuery
            if (NAME_MAP.containsKey(WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DEVICENAME, i))) {
                // then build a sound card by extracting values from cardKernelQuery
                soundCards.add(new WindowsSoundCard(getAudioCardKernelVersion(i, cardKernelQueryResult), WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DRIVERPROVIDERNAME, i)
                        + " " + WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DEVICENAME, i),
                        (WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DEVICENAME, i))));
            }
        }
        return soundCards;
    }
}
