/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.windows.Win32Exception;
import oshi.ffm.windows.WinErrorFFM;
import oshi.ffm.windows.WinRegFFM;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractSoundCard;
import oshi.util.platform.windows.Advapi32UtilFFM;

/**
 * Sound Card data obtained from registry using FFM.
 */
@Immutable
final class WindowsSoundCardFFM extends AbstractSoundCard {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsSoundCardFFM.class);

    private static final String REGISTRY_SOUNDCARDS = "SYSTEM\\CurrentControlSet\\Control\\Class\\{4d36e96c-e325-11ce-bfc1-08002be10318}\\";

    private static final MemorySegment HKLM = MemorySegment.ofAddress(WinRegFFM.HKEY_LOCAL_MACHINE);

    WindowsSoundCardFFM(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    public static List<SoundCard> getSoundCards() {
        List<SoundCard> soundCards = new ArrayList<>();
        String[] keys;
        try {
            keys = Advapi32UtilFFM.registryGetKeys(HKLM, REGISTRY_SOUNDCARDS, 0);
        } catch (Win32Exception e) {
            if (e.getErrorCode() != WinErrorFFM.ERROR_ACCESS_DENIED) {
                throw e;
            }
            return soundCards;
        } catch (Throwable t) { // NOSONAR squid:S1181
            LOG.debug("Failed to enumerate sound card registry keys: {}", t.getMessage());
            return soundCards;
        }
        for (String key : keys) {
            try {
                String fullKey = REGISTRY_SOUNDCARDS + key;
                if (Advapi32UtilFFM.registryValueExists(HKLM, fullKey, "Driver")) {
                    String driver = getRegString(fullKey, "Driver");
                    String driverVersion = getRegString(fullKey, "DriverVersion");
                    String providerName = getRegString(fullKey, "ProviderName");
                    String driverDesc = getRegString(fullKey, "DriverDesc");
                    soundCards.add(new WindowsSoundCardFFM(driver + " " + driverVersion,
                            providerName + " " + driverDesc, driverDesc));
                }
            } catch (Win32Exception e) {
                if (e.getErrorCode() != WinErrorFFM.ERROR_ACCESS_DENIED) {
                    throw e;
                }
            } catch (Throwable t) { // NOSONAR squid:S1181
                LOG.debug("Failed to read sound card registry key {}: {}", key, t.getMessage());
            }
        }
        return soundCards;
    }

    private static String getRegString(String keyPath, String valueName) {
        Object val = Advapi32UtilFFM.registryGetValue(HKLM, keyPath, valueName);
        return val instanceof String ? (String) val : "";
    }
}
