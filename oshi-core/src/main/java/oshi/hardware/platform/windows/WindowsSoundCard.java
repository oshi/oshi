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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.hardware.common.AbstractSoundCard;
import oshi.jna.platform.windows.WbemcliUtil;
import oshi.util.platform.windows.WmiUtil;

import java.util.ArrayList;
import java.util.List;

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

    private static final Logger LOG = LoggerFactory.getLogger(WindowsSoundCard.class);
    private static WbemcliUtil.WmiQuery<SoundCardName> AUDIO_CARD_QUERY = new WbemcliUtil.WmiQuery<>("Win32_SoundDevice",SoundCardName.class);
    private static WbemcliUtil.WmiResult<SoundCardName> AUDIO_CARD_QUERY_RESULT = WmiUtil.queryWMI(AUDIO_CARD_QUERY);

    public WindowsSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    /**
     * Gets a 'complete' name of the sound card. The 'complete' name is in fact a concatenated string from the Win32_SoundDevice query
     * consisting of :
     * <ul>
     *     <li>The Manufacturer property</li>
     *     <li>The Name property</li>
     * </ul>
     * @param index
     *            The row to fetch from
     * @return The audio card name
     */
    private static String getAudioCardCompleteName(int index) {
        return WmiUtil.getString(AUDIO_CARD_QUERY_RESULT,SoundCardName.MANUFACTURER,index) + " " +
                WmiUtil.getString(AUDIO_CARD_QUERY_RESULT,SoundCardName.NAME,index);
    }

    /**
     * Gets the standard audio card name returned by the Name property of Win32_SoundDevice query.
     * @param index
     *          The row to fetch from.
     * @return  The audio card name.
     */
    private static String getAudioCardName(int index) {
        return WmiUtil.getString(AUDIO_CARD_QUERY_RESULT,SoundCardName.NAME,index);
    }

    /**
     * Gets the 'complete' driver name . The 'complete' driver name is infact a concatenated string
     * of the values returned by the AUDIO_CARD_KERNEL_QUERY query result consisting of :
     * <ul>
     *     <li>The DriverName property </li>
     *     <li>The DriverVersion property</li>
     *     <li>The DeviceName property</li>
     *     <li>The DriverProvider property</li>
     * </ul>
     * Note: The query of AUDIO_CARD_KERNEL_QUERY is like Get-WmiObject Win32_PnPSignedDriver |
     * where {$_.devicename -like "*(<b>the name of the card returned by Win32_SoundDevice Name Property</b>)*"}
     * @param index
     *        The row to fetch from
     * @return  The 'complete' name of the driver.
     */
    private static String getAudioCardKernelVersion(int index) {
        String audioCardKernel;

        WbemcliUtil.WmiQuery<SoundCardKernel> cardKernelQuery = new WbemcliUtil.WmiQuery<>("Win32_PnPSignedDriver WHERE DeviceName=\""
                + AUDIO_CARD_QUERY_RESULT.getValue(SoundCardName.NAME, index)
                + "\"", SoundCardKernel.class);
        WbemcliUtil.WmiResult<SoundCardKernel> cardKernelQueryResult = WmiUtil.queryWMI(cardKernelQuery);

        audioCardKernel =
                WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DRIVERPROVIDERNAME, index) + " "
                        + WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DEVICENAME, index) + " "
                        + WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DRIVERNAME, index) + " "
                        + WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DRIVERVERSION, index);

        return audioCardKernel;
    }

    /**
     * Returns the sound cards..
     *
     * @return List of sound cards
     */
    public static List<WindowsSoundCard> getSoundCards() {
        List<WindowsSoundCard> cards = new ArrayList<>();
        for (int i = 0; i < AUDIO_CARD_QUERY_RESULT.getResultCount(); i++) {
            cards.add(new WindowsSoundCard(getAudioCardKernelVersion(i), getAudioCardCompleteName(i), getAudioCardName(i)));
        }
        return cards;
    }
}
