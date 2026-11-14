/*
 * Copyright 2018-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractSoundCard;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * FreeBSD and DragonFly BSD sound card, read from the device tree the kernel publishes under {@code dev.}.
 */
@Immutable
public class FreeBsdSoundCard extends AbstractSoundCard {

    /** A device-tree sysctl line, {@code dev.pcm.0.%desc: Dummy Audio Device}. */
    private static final Pattern DEV_SYSCTL = Pattern.compile("dev\\.(\\w+)\\.(\\d+)\\.%(desc|parent): ?(.*)");
    /** A device name, {@code hdaa0}: the driver, then the unit. */
    private static final Pattern DEVICE_NAME = Pattern.compile("([a-z_]\\w*?)(\\d+)");
    /** The buses a sound controller sits on, which end the walk up the tree. */
    private static final Pattern BUS = Pattern.compile("(nexus|pci|uhub|usbus|acpi|isa|ofwbus|simplebus)\\d*");
    /** The kernel release, which is the version of the in-kernel sound drivers. */
    private static final String OSRELEASE = "kern.osrelease";
    /** Bounds the walk: HDA is pcm, hdaa, hdacc, hdac. */
    private static final int MAX_DEPTH = 4;

    /**
     * Constructor for FreeBsdSoundCard.
     *
     * @param kernelVersion The version
     * @param name          The name
     * @param codec         The codec
     */
    FreeBsdSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    /**
     * Gets the sound cards: one for each {@code pcm} device.
     *
     * @return a {@link java.util.List} object.
     */
    public static List<SoundCard> getSoundCards() {
        return parseSoundCards(oids -> ExecutingCommand.runNative("sysctl " + String.join(" ", oids)));
    }

    /**
     * Builds one sound card for each {@code pcm} device. Package-private for testing.
     * <p>
     * Each {@code pcm} device is an audio endpoint, whose description names it, as in
     * {@code Realtek ALC892 (Rear Analog)}; that is the codec. Its card is the controller it hangs from, found by
     * following each device's {@code %parent} up the tree until the bus: on an HDA system {@code pcm0} is on
     * {@code hdaa0}, which is on the codec {@code hdacc0}, which is on the controller {@code hdac0} on {@code pci0}. A
     * device attached directly to a bus, such as {@code snd_dummy} on {@code nexus0}, is its own card. Reading the tree
     * needs no privilege, where {@code /dev/sndstat} may not be readable, and {@code lshal}, the previous source, is no
     * longer packaged.
     *
     * @param sysctl runs {@code sysctl} on the given names and returns the lines it prints
     * @return the sound cards, in {@code pcm} unit order
     */
    static List<SoundCard> parseSoundCards(Function<List<String>, List<String>> sysctl) {
        List<String> first = sysctl.apply(Arrays.asList(OSRELEASE, "dev.pcm"));
        Map<String, Map<String, String>> devices = parseDevices(first);
        Map<String, Map<String, String>> pcms = devices.entrySet().stream()
                .filter(e -> e.getKey().startsWith("pcm") && DEVICE_NAME.matcher(e.getKey()).matches())
                .sorted(Map.Entry.comparingByKey((a, b) -> Integer.compare(unit(a), unit(b))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        if (pcms.isEmpty()) {
            return new ArrayList<>();
        }
        String version = ParseUtil.getStringValueOrUnknown(version(first));
        // Walk every pcm device's ancestors together, one sysctl call for each level of the tree
        Set<String> tried = new HashSet<>();
        List<String> pending = parentsToFetch(pcms.values(), tried);
        for (int depth = 1; depth < MAX_DEPTH && !pending.isEmpty(); depth++) {
            tried.addAll(pending);
            List<String> oids = new ArrayList<>();
            for (String device : pending) {
                Matcher m = DEVICE_NAME.matcher(device);
                if (m.matches()) {
                    oids.add("dev." + m.group(1) + "." + m.group(2) + ".%desc");
                    oids.add("dev." + m.group(1) + "." + m.group(2) + ".%parent");
                }
            }
            devices.putAll(parseDevices(sysctl.apply(oids)));
            pending = parentsToFetch(devices.values(), tried);
        }
        List<SoundCard> soundCards = new ArrayList<>();
        for (Map<String, String> pcm : pcms.values()) {
            String codec = ParseUtil.getStringValueOrUnknown(pcm.get("desc"));
            String card = codec;
            Map<String, String> device = pcm;
            for (int depth = 0; depth < MAX_DEPTH; depth++) {
                Map<String, String> parent = devices.get(device.getOrDefault("parent", ""));
                if (parent == null) {
                    break;
                }
                card = ParseUtil.getStringValueOrDefault(parent.get("desc"), card);
                device = parent;
            }
            soundCards.add(new FreeBsdSoundCard(version, card, codec));
        }
        return soundCards;
    }

    /**
     * Parses the {@code %desc} and {@code %parent} lines of {@code sysctl dev.<driver>} output, keyed by device name.
     *
     * @param lines the lines emitted by {@code sysctl}
     * @return each device's {@code desc} and {@code parent}
     */
    private static Map<String, Map<String, String>> parseDevices(List<String> lines) {
        Map<String, Map<String, String>> devices = new HashMap<>();
        for (String line : lines) {
            Matcher m = DEV_SYSCTL.matcher(line.trim());
            if (m.matches()) {
                devices.computeIfAbsent(m.group(1) + m.group(2), k -> new HashMap<>()).put(m.group(3),
                        m.group(4).trim());
            }
        }
        return devices;
    }

    /**
     * Finds the parents not yet read that are devices rather than the bus that ends the walk.
     *
     * @param children the devices whose parents to look up
     * @param tried    the devices already asked for, whether or not {@code sysctl} knew them
     * @return the device names still to read
     */
    private static List<String> parentsToFetch(Iterable<Map<String, String>> children, Set<String> tried) {
        List<String> pending = new ArrayList<>();
        for (Map<String, String> child : children) {
            String parent = child.getOrDefault("parent", "");
            if (!parent.isEmpty() && !tried.contains(parent) && !pending.contains(parent)
                    && !BUS.matcher(parent).matches() && DEVICE_NAME.matcher(parent).matches()) {
                pending.add(parent);
            }
        }
        return pending;
    }

    private static int unit(String device) {
        Matcher m = DEVICE_NAME.matcher(device);
        return m.matches() ? ParseUtil.parseIntOrDefault(m.group(2), 0) : 0;
    }

    private static String version(List<String> lines) {
        for (String line : lines) {
            if (line.startsWith(OSRELEASE + ":")) {
                return line.substring(OSRELEASE.length() + 1).trim();
            }
        }
        return Constants.UNKNOWN;
    }
}
