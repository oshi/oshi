/*
 * Copyright 2018-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinReg;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractSoundCard;

/**
 * Sound Card data obtained from registry
 */
@Immutable
final class WindowsSoundCard extends AbstractSoundCard {

    private static final String REGISTRY_SOUNDCARDS = "SYSTEM\\CurrentControlSet\\Control\\Class\\{4d36e96c-e325-11ce-bfc1-08002be10318}\\";

    /**
     * Constructor for WindowsSoundCard.
     *
     * @param kernelVersion The version
     * @param name          The name
     * @param codec         The codec
     */
    WindowsSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    /**
     * Returns Windows audio device driver information, which represents the closest proxy we have to sound cards.
     * <p>
     * NOTE : The reason why the codec name is same as the card name is because windows does not provide the name of the
     * codec chip but sometimes the name of the card returned is infact the name of the codec chip also. Example :
     * Realtek ALC887 HD Audio Device
     *
     * @return List of sound cards
     */
    public static List<SoundCard> getSoundCards() {
        List<SoundCard> soundCards = new ArrayList<>();
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
