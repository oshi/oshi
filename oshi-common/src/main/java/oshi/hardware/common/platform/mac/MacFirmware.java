/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractFirmware;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quintet;

/**
 * Firmware data obtained from ioreg. Subclasses provide platform-specific IOKit queries.
 */
@Immutable
public abstract class MacFirmware extends AbstractFirmware {

    /**
     * Default constructor.
     */
    protected MacFirmware() {
    }

    private final Supplier<Quintet<String, String, String, String, String>> manufNameDescVersRelease = memoize(
            this::queryEfi);

    @Override
    public String getManufacturer() {
        return manufNameDescVersRelease.get().getA();
    }

    @Override
    public String getName() {
        return manufNameDescVersRelease.get().getB();
    }

    @Override
    public String getDescription() {
        return manufNameDescVersRelease.get().getC();
    }

    @Override
    public String getVersion() {
        return manufNameDescVersRelease.get().getD();
    }

    @Override
    public String getReleaseDate() {
        return manufNameDescVersRelease.get().getE();
    }

    /**
     * Queries EFI firmware information.
     *
     * @return a quintet of manufacturer, name, description, version, release date
     */
    protected abstract Quintet<String, String, String, String, String> queryEfi();

    /**
     * Splits the device tree's {@code /chosen/system-firmware-version} value, published on Apple Silicon as the boot
     * firmware name and its version joined by a hyphen (for example {@code mBoot-18000.161.10}).
     *
     * @param value the property value, or {@code null} if not published
     * @return a pair of the firmware name (empty if the value has no hyphen) and the version (empty if the value is
     *         absent)
     */
    protected static Pair<String, String> splitSystemFirmwareVersion(@Nullable String value) {
        String trimmed = ParseUtil.getStringValueOrEmpty(value).trim();
        int dash = trimmed.indexOf('-');
        if (dash < 0) {
            return new Pair<>("", trimmed);
        }
        return new Pair<>(trimmed.substring(0, dash), trimmed.substring(dash + 1));
    }
}
