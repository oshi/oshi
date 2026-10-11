/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.hardware.common.platform.unix.UnixBaseboard;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.common.platform.unix.freebsd.KenvUtil;
import oshi.util.tuples.Quintet;

/**
 * Abstract base for the FreeBSD ComputerSystem, also used by DragonFly BSD. Reads the SMBIOS strings the loader copies
 * into the kernel environment, then {@code dmidecode} (root only) for any it lacks.
 */
@Immutable
public abstract class FreeBsdComputerSystem extends AbstractComputerSystem {

    private final Supplier<Quintet<String, String, String, String, String>> manufModelSerialUuidVers = memoize(
            this::readSmbios);
    private final Supplier<Map<String, String>> kenv = memoize(KenvUtil::queryKenv);

    @Override
    public String getManufacturer() {
        return manufModelSerialUuidVers.get().getA();
    }

    @Override
    public String getModel() {
        return manufModelSerialUuidVers.get().getB();
    }

    @Override
    public String getSerialNumber() {
        return manufModelSerialUuidVers.get().getC();
    }

    @Override
    public String getHardwareUUID() {
        return manufModelSerialUuidVers.get().getD();
    }

    @Override
    public Firmware createFirmware() {
        return new FreeBsdFirmware();
    }

    @Override
    public Baseboard createBaseboard() {
        // Without a baseboard (type 2) table the loader sets no smbios.planar.* variables; use the system values
        Map<String, String> env = kenv.get();
        Quintet<String, String, String, String, String> sys = manufModelSerialUuidVers.get();
        return new UnixBaseboard(ParseUtil.getStringValueOrDefault(env.get("smbios.planar.maker"), sys.getA()),
                ParseUtil.getStringValueOrDefault(env.get("smbios.planar.product"), sys.getB()),
                ParseUtil.getStringValueOrDefault(env.get("smbios.planar.serial"), sys.getC()),
                ParseUtil.getStringValueOrDefault(env.get("smbios.planar.version"), sys.getE()));
    }

    /**
     * Reads {@code kern.hostuuid} via the subclass's sysctl mechanism. Used as a fallback when {@code dmidecode} does
     * not yield a UUID (commonly the case without root).
     *
     * @return the host UUID, or {@link oshi.util.Constants#UNKNOWN} if the sysctl read fails
     */
    protected abstract String queryHostUuid();

    private Quintet<String, String, String, String, String> readSmbios() {
        Map<String, String> env = kenv.get();
        String manufacturer = env.get("smbios.system.maker");
        String model = env.get("smbios.system.product");
        String serialNumber = env.get("smbios.system.serial");
        String uuid = env.get("smbios.system.uuid");
        String version = env.get("smbios.system.version");
        if (Util.isBlank(manufacturer) || Util.isBlank(model) || Util.isBlank(serialNumber) || Util.isBlank(uuid)
                || Util.isBlank(version)) {
            // Only works with root permissions
            Quintet<String, String, String, String, String> dmi = parseDmiDecode(
                    ExecutingCommand.runNative("dmidecode -t system"));
            manufacturer = ParseUtil.getStringValueOrDefault(manufacturer, dmi.getA());
            model = ParseUtil.getStringValueOrDefault(model, dmi.getB());
            serialNumber = ParseUtil.getStringValueOrDefault(serialNumber, dmi.getC());
            uuid = ParseUtil.getStringValueOrDefault(uuid, dmi.getD());
            version = ParseUtil.getStringValueOrDefault(version, dmi.getE());
        }
        // If we get to end and haven't assigned, use fallback
        if (Util.isBlank(serialNumber)) {
            serialNumber = querySystemSerialNumber();
        }
        if (Util.isBlank(uuid)) {
            uuid = queryHostUuid();
        }
        return new Quintet<>(ParseUtil.getStringValueOrUnknown(manufacturer), ParseUtil.getStringValueOrUnknown(model),
                ParseUtil.getStringValueOrUnknown(serialNumber), ParseUtil.getStringValueOrUnknown(uuid),
                ParseUtil.getStringValueOrUnknown(version));
    }

    /**
     * Parses the output of {@code dmidecode -t system} into its manufacturer, model, serial number, UUID, and version
     * fields. Any field not present in the output is returned empty; the caller applies fallbacks.
     *
     * @param dmidecode the lines emitted by {@code dmidecode -t system}
     * @return a {@link Quintet} of manufacturer, model, serial number, UUID, and version
     */
    static Quintet<String, String, String, String, String> parseDmiDecode(List<String> dmidecode) {
        String manufacturer = "";
        String model = "";
        String serialNumber = "";
        String uuid = "";
        String version = "";

        final String manufacturerMarker = "Manufacturer:";
        final String productNameMarker = "Product Name:";
        final String serialNumMarker = "Serial Number:";
        final String uuidMarker = "UUID:";
        final String versionMarker = "Version:";

        for (final String checkLine : dmidecode) {
            if (checkLine.contains(manufacturerMarker)) {
                manufacturer = ParseUtil.getTextAfterString(checkLine, manufacturerMarker).trim();
            } else if (checkLine.contains(productNameMarker)) {
                model = ParseUtil.getTextAfterString(checkLine, productNameMarker).trim();
            } else if (checkLine.contains(serialNumMarker)) {
                serialNumber = ParseUtil.getTextAfterString(checkLine, serialNumMarker).trim();
            } else if (checkLine.contains(uuidMarker)) {
                uuid = ParseUtil.getTextAfterString(checkLine, uuidMarker).trim();
            } else if (checkLine.contains(versionMarker)) {
                version = ParseUtil.getTextAfterString(checkLine, versionMarker).trim();
            }
        }
        return new Quintet<>(manufacturer, model, serialNumber, uuid, version);
    }

    private static String querySystemSerialNumber() {
        String marker = "system.hardware.serial =";
        for (String checkLine : ExecutingCommand.runNative("lshal")) {
            if (checkLine.contains(marker)) {
                return ParseUtil.getSingleQuoteStringValue(checkLine);
            }
        }
        return Constants.UNKNOWN;
    }
}
