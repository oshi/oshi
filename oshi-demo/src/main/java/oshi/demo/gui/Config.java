/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.gui;

/**
 * Configuration for the GUI. Ideally would read this information from an external config file, and more items should be
 * added.
 */
public final class Config {

    private Config() {
    }

    /** GUI window title. */
    public static final String GUI_TITLE = "Operating System & Hardware Information";
    /** GUI window width. */
    public static final int GUI_WIDTH = 800;
    /** GUI window height. */
    public static final int GUI_HEIGHT = 500;

    /** Fast refresh interval in ms. */
    public static final int REFRESH_FAST = 1000;
    /** Slow refresh interval in ms. */
    public static final int REFRESH_SLOW = 5000;
    /** Slow refresh interval in ms. */
    /** Slower refresh interval in ms. */
    public static final int REFRESH_SLOWER = 15_000;
}
