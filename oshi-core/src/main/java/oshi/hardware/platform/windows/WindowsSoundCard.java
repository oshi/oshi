/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.platform.win32.COM.WbemcliUtil; // NOSONAR squid:S1191

import oshi.hardware.common.AbstractSoundCard;
import oshi.util.platform.windows.WmiQueryHandler;
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

    public WindowsSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
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
        // Get the map of device manufacturer by name
        WmiQueryHandler wmiQueryHandler = WmiQueryHandler.createInstance();
        Map<String, String> deviceManufacturerMap = getDeviceManufacturerMap(wmiQueryHandler);
        String driverQuery = createClause(deviceManufacturerMap);

        WbemcliUtil.WmiQuery<SoundCardKernel> cardKernelQuery = new WbemcliUtil.WmiQuery<>(driverQuery,
                SoundCardKernel.class);
        WbemcliUtil.WmiResult<SoundCardKernel> cardKernelQueryResult = wmiQueryHandler.queryWMI(cardKernelQuery);

        List<WindowsSoundCard> soundCards = new ArrayList<>();
        for (int i = 0; i < cardKernelQueryResult.getResultCount(); i++) {
            // If the map has a key that is equal to the value returned by
            // cardKernelQuery
            if (deviceManufacturerMap
                    .containsKey(WmiUtil.getString(cardKernelQueryResult, SoundCardKernel.DEVICENAME, i))) {
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

    /**
     * Gets the map of device manufacturer by name
     * 
     * @param wmiQueryHandler
     * 
     * @return THe map
     */
    private static Map<String, String> getDeviceManufacturerMap(WmiQueryHandler wmiQueryHandler) {
        Map<String, String> deviceManufacturerMap = new HashMap<>();
        WbemcliUtil.WmiQuery<SoundCardName> soundCardQuery = new WbemcliUtil.WmiQuery<>("Win32_SoundDevice",
                SoundCardName.class);
        WbemcliUtil.WmiResult<SoundCardName> soundCardResult = wmiQueryHandler.queryWMI(soundCardQuery);
        for (int i = 0; i < soundCardResult.getResultCount(); i++) {
            deviceManufacturerMap.put(WmiUtil.getString(soundCardResult, SoundCardName.NAME, i),
                    WmiUtil.getString(soundCardResult, SoundCardName.MANUFACTURER, i));
        }
        return deviceManufacturerMap;
    }

    /**
     * Creates our Win32_PnPSignedDevice query with the WHERE clause taking the
     * attributes from our map.
     *
     * @param deviceManufacturerMap
     *            The map
     * @return The WHERE clause
     */
    private static String createClause(Map<String, String> deviceManufacturerMap) {
        StringBuilder sb = new StringBuilder("Win32_PnPSignedDriver");
        boolean first = true;
        for (String key : deviceManufacturerMap.keySet()) {
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
}
