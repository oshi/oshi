package oshi.hardware.platform.windows;

import oshi.hardware.common.AbstractAssembly;
import oshi.util.platform.windows.WmiUtil;

import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: SchiTho1
 * Date: 14.10.2016
 * Time: 07:58
 * <p>
 * Copyright 2008-2013 - Securiton AG all rights reserved
 */
final class WindowsAssembly extends AbstractAssembly {

    WindowsAssembly() {

        init();
    }

    private void init() {

        final Map<String, List<String>> win32ComputerSystem = WmiUtil.selectStringsFrom("root\\cimv2", "Win32_ComputerSystem", "Manufacturer,Model", null);

        final List<String> manufacturers = win32ComputerSystem.get("Manufacturer");
        if (manufacturers != null && manufacturers.size() == 1) {
            setManufacturer(manufacturers.get(0));
        }

        final List<String> models = win32ComputerSystem.get("Model");
        if (models != null && models.size() == 1) {
            setModel(models.get(0));
        }

        final Map<String, List<String>> win32BIOS = WmiUtil.selectStringsFrom("root\\cimv2", "Win32_BIOS", "SerialNumber", "where PrimaryBIOS=true");
        final List<String> serialNumbers = win32BIOS.get("SerialNumber");
        if (serialNumbers != null && serialNumbers.size() == 1) {
            setSerialNumber(serialNumbers.get(0));
        }
    }
}
