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
import java.util.List;

import com.sun.jna.platform.win32.Advapi32Util; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinReg;

import oshi.hardware.common.AbstractSoundCard;

/**
 * Sound Card data obtained from registry
 */
public class WindowsSoundCard extends AbstractSoundCard {

    private static final String REGISTRY_SOUNDCARDS = "SYSTEM\\CurrentControlSet\\Control\\Class\\{4d36e96c-e325-11ce-bfc1-08002be10318}\\";

    public WindowsSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    /**
     * Returns Windows audio device driver information, which represents the
     * closest proxy we have to sound cards.
     * <p>
     * NOTE : The reason why the codec name is same as the card name is because
     * windows does not provide the name of the codec chip but sometimes the
     * name of the card returned is infact the name of the codec chip also.
     * Example : Realtek ALC887 HD Audio Device
     *
     * @return List of sound cards
     */
    public static List<WindowsSoundCard> getSoundCards() {
        List<WindowsSoundCard> soundCards = new ArrayList<>();
        String[] keys = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, REGISTRY_SOUNDCARDS);
        for (String key : keys) {
            String fullKey = REGISTRY_SOUNDCARDS + key;
            try {
                if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, fullKey, "Driver")) {
                    soundCards.add(new WindowsSoundCard(
                            Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, "Driver") + " "
                                    + Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey,
                                            "DriverVersion"),
                            Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, "ProviderName")
                                    + " "
                                    + Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey,
                                            "DriverDesc"),
                            Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, "DriverDesc")));
                }
            } catch (Win32Exception e) {
                if (e.getErrorCode() != WinError.ERROR_ACCESS_DENIED) {
                    // Ignore access denied errors, re-throw others
                    throw e;
                }
            }
        }
        return soundCards;
    }
}
