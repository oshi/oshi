/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo;

import java.util.List;
import java.util.Locale;

import oshi.SystemInfo;
import oshi.annotation.SuppressForbidden;
import oshi.hardware.GraphicsCard;
import oshi.hardware.GpuStats;
import oshi.hardware.GpuTicks;
import oshi.util.FormatUtil;

/**
 * Demonstrates GPU metrics by polling all graphics cards 10 times at 1-second intervals using the session-based
 * {@link GpuStats} API. One session per card is held open for the entire polling window so that session-scoped native
 * state (e.g. IOReport power baselines on macOS) is preserved across iterations.
 */
public final class PollGpuStats {

    private static final int ITERATIONS = 10;
    private static final long INTERVAL_MS = 1_000L;

    private PollGpuStats() {
    }

    @SuppressForbidden(reason = "Using System.out in a demo class")
    public static void main(String[] args) throws InterruptedException {
        List<GraphicsCard> cards = new SystemInfo().getHardware().getGraphicsCards();
        if (cards.isEmpty()) {
            System.out.println("No graphics cards found.");
            return;
        }

        for (GraphicsCard card : cards) {
            System.out.printf(Locale.ROOT, "GPU   : %s%n", card.getName());
            System.out.printf(Locale.ROOT, "Vendor: %s%n", card.getVendor());
            System.out.printf(Locale.ROOT, "VRAM  : %s%n", FormatUtil.formatBytes(card.getVRam()));
            System.out.println();
        }

        int nameWidth = cards.stream().mapToInt(c -> c.getName().length()).max().orElse(10);
        String hdr = String.format(Locale.ROOT, "%-" + nameWidth + "s  %8s  %10s  %10s  %8s  %8s  %10s", "Card",
                "Ticks%", "API Util%", "VRAM Used", "Temp(C)", "Power(W)", "Clock(MHz)");
        System.out.println(hdr);
        StringBuilder sep = new StringBuilder(hdr.length());
        for (int s = 0; s < hdr.length(); s++)
            sep.append('-');
        System.out.println(sep.toString());

        // Open one session per card for the entire polling window
        GpuStats[] sessions = new GpuStats[cards.size()];
        GpuTicks[] prev = new GpuTicks[cards.size()];
        try {
            for (int i = 0; i < cards.size(); i++) {
                sessions[i] = cards.get(i).createStatsSession();
                prev[i] = sessions[i].getGpuTicks();
                sessions[i].getPowerDraw();
                sessions[i].getGpuUtilization();
            }

            for (int iter = 0; iter < ITERATIONS; iter++) {
                Thread.sleep(INTERVAL_MS);
                for (int i = 0; i < cards.size(); i++) {
                    GraphicsCard card = cards.get(i);
                    GpuStats stats = sessions[i];
                    GpuTicks curr = stats.getGpuTicks();
                    long dActive = curr.getActiveTicks() - prev[i].getActiveTicks();
                    long dIdle = curr.getIdleTicks() - prev[i].getIdleTicks();
                    long dTotal = dActive + dIdle;
                    String tickStr = dTotal > 0 ? String.format(Locale.ROOT, "%5.1f%%", dActive * 100.0 / dTotal)
                            : "n/a";
                    prev[i] = curr;

                    double apiUtil = stats.getGpuUtilization();
                    long vramUsed = stats.getVramUsed();
                    double temp = stats.getTemperature();
                    double power = stats.getPowerDraw();
                    long clock = stats.getCoreClockMhz();

                    System.out.printf(Locale.ROOT, "%-" + nameWidth + "s  %8s  %10s  %10s  %8s  %8s  %10s%n",
                            card.getName(), tickStr,
                            apiUtil >= 0 ? String.format(Locale.ROOT, "%.1f%%", apiUtil) : "n/a",
                            vramUsed >= 0 ? FormatUtil.formatBytes(vramUsed) : "n/a",
                            temp >= 0 ? String.format(Locale.ROOT, "%.1f", temp) : "n/a",
                            power >= 0 ? String.format(Locale.ROOT, "%.2f", power) : "n/a",
                            clock >= 0 ? Long.toString(clock) : "n/a");
                }
                if (cards.size() > 1) {
                    System.out.println();
                }
            }
        } finally {
            for (GpuStats s : sessions) {
                if (s != null) {
                    try {
                        s.close();
                    } catch (Exception e) {
                        // best-effort: attempt to close all sessions even if one throws
                    }
                }
            }
        }
    }

}
