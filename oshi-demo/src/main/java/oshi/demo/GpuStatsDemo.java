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

/**
 * Demonstrates GPU metrics by polling all graphics cards 10 times at 1-second intervals using the session-based
 * {@link GpuStats} API. Each iteration opens a session, samples metrics, and closes it via try-with-resources.
 */
public final class GpuStatsDemo {

    private static final int ITERATIONS = 10;
    private static final long INTERVAL_MS = 1_000L;

    private GpuStatsDemo() {
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
            System.out.printf(Locale.ROOT, "VRAM  : %s%n", formatBytes(card.getVRam()));
            System.out.println();
        }

        int nameWidth = cards.stream().mapToInt(c -> c.getName().length()).max().orElse(10);
        String hdr = String.format(Locale.ROOT, "%-" + nameWidth + "s  %8s  %10s  %10s  %8s  %8s  %8s", "Card",
                "Ticks%", "API Util%", "VRAM Used", "Temp(C)", "Power(W)", "Clock(MHz)");
        System.out.println(hdr);
        StringBuilder sep = new StringBuilder(hdr.length());
        for (int s = 0; s < hdr.length(); s++)
            sep.append('-');
        System.out.println(sep.toString());

        // Seed first tick snapshots (one session per card, closed immediately after seeding)
        GpuTicks[] prev = new GpuTicks[cards.size()];
        for (int i = 0; i < cards.size(); i++) {
            try (GpuStats stats = cards.get(i).createStatsSession()) {
                prev[i] = stats.getGpuTicks();
                stats.getPowerDraw(); // prime power delta
            }
        }

        for (int iter = 0; iter < ITERATIONS; iter++) {
            Thread.sleep(INTERVAL_MS);
            for (int i = 0; i < cards.size(); i++) {
                GraphicsCard card = cards.get(i);
                try (GpuStats stats = card.createStatsSession()) {
                    GpuTicks curr = stats.getGpuTicks();
                    long dtTicks = curr.getTimestamp() - prev[i].getTimestamp();
                    long dActive = curr.getActiveTicks() - prev[i].getActiveTicks();
                    double tickUtil = dtTicks > 0 && dActive >= 0 ? dActive * 100.0 / dtTicks : -1d;
                    prev[i] = curr;

                    double apiUtil = stats.getGpuUtilization();
                    long vramUsed = stats.getVramUsed();
                    double temp = stats.getTemperature();
                    double power = stats.getPowerDraw();
                    long clock = stats.getCoreClockMhz();

                    System.out.printf(Locale.ROOT, "%-" + nameWidth + "s  %7.1f%%  %9s  %10s  %8s  %8s  %8s%n",
                            card.getName(), tickUtil >= 0 ? tickUtil : Double.NaN,
                            apiUtil >= 0 ? String.format(Locale.ROOT, "%.1f%%", apiUtil) : "n/a",
                            vramUsed >= 0 ? formatBytes(vramUsed) : "n/a",
                            temp >= 0 ? String.format(Locale.ROOT, "%.1f", temp) : "n/a",
                            power >= 0 ? String.format(Locale.ROOT, "%.2f", power) : "n/a",
                            clock >= 0 ? Long.toString(clock) : "n/a");
                }
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
