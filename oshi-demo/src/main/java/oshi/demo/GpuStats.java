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
import oshi.hardware.GpuTicks;

/**
 * Demonstrates GPU metrics by polling all graphics cards 10 times at 1-second intervals. For each interval, tick-based
 * utilization is computed from two consecutive {@link GpuTicks} snapshots and compared against the platform's
 * instantaneous utilization value.
 */
public final class GpuStats {

    private static final int ITERATIONS = 10;
    private static final long INTERVAL_MS = 1_000L;

    private GpuStats() {
    }

    @SuppressForbidden(reason = "Using System.out in a demo class")
    public static void main(String[] args) throws InterruptedException {
        List<GraphicsCard> cards = new SystemInfo().getHardware().getGraphicsCards();
        if (cards.isEmpty()) {
            System.out.println("No graphics cards found.");
            return;
        }

        // Print static info once
        for (GraphicsCard card : cards) {
            System.out.printf(Locale.ROOT, "GPU   : %s%n", card.getName());
            System.out.printf(Locale.ROOT, "Vendor: %s%n", card.getVendor());
            System.out.printf(Locale.ROOT, "VRAM  : %s%n", formatBytes(card.getVRam()));
            System.out.println();
        }

        // Header
        int nameWidth = cards.stream().mapToInt(c -> c.getName().length()).max().orElse(10);
        String hdr = String.format(Locale.ROOT, "%-" + nameWidth + "s  %8s  %10s  %10s  %8s  %8s  %8s", "Card",
                "Ticks%", "API Util%", "VRAM Used", "Temp(C)", "Power(W)", "Clock(MHz)");
        System.out.println(hdr);
        StringBuilder sep = new StringBuilder();
        for (int s = 0; s < hdr.length(); s++) {
            sep.append('-');
        }
        System.out.println(sep);

        // Seed first tick and power snapshots
        GpuTicks[] prev = new GpuTicks[cards.size()];
        for (int i = 0; i < cards.size(); i++) {
            prev[i] = cards.get(i).getGpuTicks();
            cards.get(i).getPowerDraw();
        }

        for (int iter = 0; iter < ITERATIONS; iter++) {
            Thread.sleep(INTERVAL_MS);
            for (int i = 0; i < cards.size(); i++) {
                GraphicsCard card = cards.get(i);
                GpuTicks curr = card.getGpuTicks();

                long dtTicks = curr.getTimestamp() - prev[i].getTimestamp();
                long dActive = curr.getActiveTicks() - prev[i].getActiveTicks();
                double tickUtil = dtTicks > 0 && dActive >= 0 ? dActive * 100.0 / dtTicks : -1d;
                prev[i] = curr;

                double apiUtil = card.getGpuUtilization();
                long vramUsed = card.getVramUsed();
                double temp = card.getTemperature();
                double power = card.getPowerDraw();
                long clock = card.getCoreClockMhz();

                System.out.printf(Locale.ROOT, "%-" + nameWidth + "s  %7.1f%%  %9s  %10s  %8s  %8s  %8s%n",
                        card.getName(), tickUtil >= 0 ? tickUtil : Double.NaN,
                        apiUtil >= 0 ? String.format(Locale.ROOT, "%.1f%%", apiUtil) : "n/a",
                        vramUsed >= 0 ? formatBytes(vramUsed) : "n/a",
                        temp >= 0 ? String.format(Locale.ROOT, "%.1f", temp) : "n/a",
                        power >= 0 ? String.format(Locale.ROOT, "%.2f", power) : "n/a",
                        clock >= 0 ? Long.toString(clock) : "n/a");
            }
            if (cards.size() > 1) {
                System.out.println();
            }
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 0) {
            return "n/a";
        }
        if (bytes >= 1L << 30) {
            return String.format(Locale.ROOT, "%.1f GiB", bytes / (double) (1L << 30));
        }
        if (bytes >= 1L << 20) {
            return String.format(Locale.ROOT, "%.1f MiB", bytes / (double) (1L << 20));
        }
        return String.format(Locale.ROOT, "%d B", bytes);
    }
}
