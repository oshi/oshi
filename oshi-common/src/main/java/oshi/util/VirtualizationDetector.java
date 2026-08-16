/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.util.tuples.Pair;

/**
 * Identifies a virtualized or containerized environment from signatures a hypervisor leaves in hardware information.
 * <p>
 * The signature tables ship as the {@code oshi.vm.properties} and {@code oshi.vmmacaddr.properties} resources. Placing
 * a file of either name earlier on the classpath replaces that table wholesale, which is the supported way to teach
 * OSHI about a platform it does not know; note that it replaces rather than merges, so an override loses the built-in
 * entries.
 */
@ThreadSafe
public final class VirtualizationDetector {

    private static final String OSHI_VM_PROPERTIES = "oshi.vm.properties";
    private static final String OSHI_VM_MAC_ADDR_PROPERTIES = "oshi.vmmacaddr.properties";

    private static final String CPUID_PREFIX = "cpuid.";
    private static final String MODEL_PREFIX = "model.";

    /**
     * Whether the running platform reports {@link NetworkIF#isConnectorPresent()}. Only Linux and Windows override the
     * interface default, and reading the default as "no connector" would discard every interface elsewhere.
     */
    private static final boolean CONNECTOR_REPORTED = PlatformEnum.getCurrentPlatform() == PlatformEnum.LINUX
            || PlatformEnum.getCurrentPlatform() == PlatformEnum.WINDOWS;

    /** Orders signatures longest first, so a longer signature is tested before one it contains. */
    private static final Comparator<Pair<String, String>> LONGEST_FIRST = (a, b) -> {
        int byLength = Integer.compare(b.getA().length(), a.getA().length());
        return byLength == 0 ? a.getA().compareTo(b.getA()) : byLength;
    };

    private static final Supplier<Properties> VM_PROPS = memoize(VirtualizationDetector::queryVmProps);
    private static final Supplier<Properties> MAC_PROPS = memoize(VirtualizationDetector::queryMacProps);
    private static final Supplier<List<Pair<String, String>>> MODEL_TABLE = memoize(
            VirtualizationDetector::queryModelTable);

    private VirtualizationDetector() {
    }

    /**
     * Attempts to identify the virtualized or containerized environment this machine is running in.
     * <p>
     * Three signatures are tested, and the first to match wins: the hypervisor's CPUID vendor string, the computer
     * system's manufacturer and model, and the OUI of a network interface's MAC address. Where two disagree the earlier
     * one is reported, which for a QEMU guest running under KVM is the normal case rather than an error.
     *
     * @param hw The {@link HardwareAbstractionLayer} to read signatures from.
     * @return The name of the virtualization platform if one was identified, otherwise an empty {@link Optional}. An
     *         empty value means no signature matched, which is not proof that the machine is physical.
     */
    public static Optional<String> identify(HardwareAbstractionLayer hw) {
        Optional<String> cpuid = matchCpuid(hw.getProcessor().getProcessorIdentifier().getVendor(), VM_PROPS.get());
        if (cpuid.isPresent()) {
            return cpuid;
        }
        Optional<String> system = matchSystem(hw.getComputerSystem().getManufacturer(),
                hw.getComputerSystem().getModel(), MODEL_TABLE.get());
        if (system.isPresent()) {
            return system;
        }
        return matchMac(candidateMacAddresses(hw.getNetworkIFs()), MAC_PROPS.get());
    }

    /**
     * Collects the MAC addresses worth testing for a virtualization OUI.
     * <p>
     * A physical host with VirtualBox or Docker merely installed carries an adapter with a virtualization OUI, which
     * would identify the host as a guest. Interfaces reporting no connector are excluded to suppress that, but only on
     * the platforms that report connector presence at all.
     *
     * @param networkIFs The interfaces to filter.
     * @return The MAC addresses to test, in interface order.
     */
    private static List<String> candidateMacAddresses(List<NetworkIF> networkIFs) {
        List<String> macs = new ArrayList<>();
        for (NetworkIF nif : networkIFs) {
            if (!CONNECTOR_REPORTED || nif.isConnectorPresent()) {
                macs.add(nif.getMacaddr());
            }
        }
        return macs;
    }

    /**
     * Matches a CPUID vendor string against the {@code cpuid.} signatures.
     *
     * @param vendor The processor identifier's vendor string. Surrounding whitespace is ignored.
     * @param props  The signature table.
     * @return The platform name, or an empty {@link Optional} if the vendor is not a known hypervisor.
     */
    static Optional<String> matchCpuid(String vendor, Properties props) {
        return Optional.ofNullable(props.getProperty(CPUID_PREFIX + vendor.trim()));
    }

    /**
     * Matches a computer system's manufacturer and model against the {@code model.} signatures.
     * <p>
     * The two fields are joined by a space and searched as one string, because several platforms identify themselves
     * only in the manufacturer: a QEMU guest reports model {@code Standard PC (i440FX + PIIX, 1996)}.
     *
     * @param manufacturer The computer system manufacturer.
     * @param model        The computer system model.
     * @param table        The signature table, longest signature first.
     * @return The platform name, or an empty {@link Optional} if no signature is present.
     */
    static Optional<String> matchSystem(String manufacturer, String model, List<Pair<String, String>> table) {
        String haystack = manufacturer + ' ' + model;
        for (Pair<String, String> signature : table) {
            if (haystack.contains(signature.getA())) {
                return Optional.of(signature.getB());
            }
        }
        return Optional.empty();
    }

    /**
     * Matches MAC addresses against the OUI table.
     *
     * @param macAddresses The MAC addresses to test.
     * @param props        The OUI table, keyed by uppercase colon-delimited OUI.
     * @return The platform name of the first address that matches, or an empty {@link Optional} if none do.
     */
    static Optional<String> matchMac(List<String> macAddresses, Properties props) {
        for (String mac : macAddresses) {
            String oui = extractOui(mac);
            if (!oui.isEmpty()) {
                String platform = props.getProperty(oui);
                if (platform != null) {
                    return Optional.of(platform);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Extracts the Organizationally Unique Identifier from a MAC address.
     * <p>
     * Any non-hexadecimal character is discarded before the first three octets are read, so colon-delimited,
     * hyphen-delimited and undelimited forms all parse.
     *
     * @param macaddr The MAC address.
     * @return The OUI as uppercase colon-delimited hex, or an empty string if the address holds fewer than three
     *         octets.
     */
    static String extractOui(String macaddr) {
        StringBuilder hex = new StringBuilder(6);
        for (int i = 0; i < macaddr.length() && hex.length() < 6; i++) {
            char c = macaddr.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f')) {
                hex.append(c);
            }
        }
        if (hex.length() < 6) {
            return "";
        }
        String digits = hex.toString().toUpperCase(Locale.ROOT);
        return digits.substring(0, 2) + ':' + digits.substring(2, 4) + ':' + digits.substring(4, 6);
    }

    /**
     * Reads the {@code model.} signatures into a list ordered longest first, so that matching does not depend on the
     * unordered {@link Properties} and a longer signature is tested before one it contains.
     *
     * @param props The signature table.
     * @return The signatures as (token, platform name) pairs, longest token first.
     */
    static List<Pair<String, String>> buildModelTable(Properties props) {
        List<Pair<String, String>> table = new ArrayList<>();
        for (String key : props.stringPropertyNames()) {
            // Strip the fixed-length prefix rather than splitting on a dot, because a signature may contain one
            if (key.startsWith(MODEL_PREFIX)) {
                String value = props.getProperty(key);
                if (value != null) {
                    table.add(new Pair<>(key.substring(MODEL_PREFIX.length()), value));
                }
            }
        }
        table.sort(LONGEST_FIRST);
        return table;
    }

    private static List<Pair<String, String>> queryModelTable() {
        return buildModelTable(VM_PROPS.get());
    }

    private static Properties queryVmProps() {
        return FileUtil.readPropertiesFromFilename(OSHI_VM_PROPERTIES);
    }

    private static Properties queryMacProps() {
        return FileUtil.readPropertiesFromFilename(OSHI_VM_MAC_ADDR_PROPERTIES);
    }
}
