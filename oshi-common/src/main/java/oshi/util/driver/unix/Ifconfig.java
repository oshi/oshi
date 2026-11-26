/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import java.util.Arrays;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF.IfOperStatus;

/**
 * Utility to parse the output of {@code ifconfig <interface>}, which the BSDs and AIX print in the same
 * {@code flags=<UP,BROADCAST,RUNNING,...>} form.
 */
@ThreadSafe
public final class Ifconfig {

    private static final String STATUS = "status: ";

    private Ifconfig() {
    }

    /**
     * Parses the operational status from {@code ifconfig <interface>}.
     * <p>
     * An interface without {@code UP} in its flags is down, whatever its {@code status:} line says: NetBSD prints
     * {@code status: active} on a loopback that is administratively down, and OpenBSD on an {@code enc0} with no flags
     * at all. Otherwise the {@code status:} line decides: {@code active}, {@code associated} on a wireless interface,
     * and {@code master} on a CARP interface mean the link is up; {@code no carrier}, {@code no network} and NetBSD's
     * {@code down}, for a tunnel without endpoints, that it is down; a CARP {@code backup} is dormant; and NetBSD's
     * {@code unknown} and OpenBSD's {@code invalid} are unknown, as is any word not listed here. An interface with no
     * {@code status:} line, such as a loopback on FreeBSD or any interface on AIX, is up when it is {@code RUNNING}.
     *
     * @param ifconfig the lines emitted by {@code ifconfig <interface>}
     * @return the operational status, or {@link IfOperStatus#UNKNOWN} if there is no output
     */
    public static IfOperStatus parseOperStatus(List<String> ifconfig) {
        if (ifconfig.isEmpty()) {
            return IfOperStatus.UNKNOWN;
        }
        String first = ifconfig.get(0);
        int open = first.indexOf('<');
        int close = first.indexOf('>', open + 1);
        if (open < 0 || close < 0) {
            return IfOperStatus.UNKNOWN;
        }
        List<String> flags = Arrays.asList(first.substring(open + 1, close).split(",", -1));
        if (!flags.contains("UP")) {
            return IfOperStatus.DOWN;
        }
        for (String line : ifconfig) {
            String trimmed = line.trim();
            if (trimmed.startsWith(STATUS)) {
                return linkStatus(trimmed.substring(STATUS.length()).trim());
            }
        }
        return flags.contains("RUNNING") ? IfOperStatus.UP : IfOperStatus.DOWN;
    }

    private static IfOperStatus linkStatus(String status) {
        if (status.startsWith("no ")) {
            // "no carrier", "no network" on wireless, "no ring" on FDDI
            return IfOperStatus.DOWN;
        }
        switch (status) {
            case "active":
            case "associated":
            case "running":
            case "inserted":
            case "master":
            case "half-duplex":
            case "full-duplex":
                return IfOperStatus.UP;
            case "down":
                return IfOperStatus.DOWN;
            case "backup":
                // A CARP interface standing by for the master
                return IfOperStatus.DORMANT;
            default:
                // "unknown" on NetBSD, "invalid" on OpenBSD, or a word this list does not know
                return IfOperStatus.UNKNOWN;
        }
    }
}
