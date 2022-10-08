/*
 * Copyright 2020-2022 The OSHI Project Contributors
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

    public static final String GUI_TITLE = "Operating System & Hardware Information";
    public static final int GUI_WIDTH = 800;
    public static final int GUI_HEIGHT = 500;

    public static final int REFRESH_FAST = 1000;
    public static final int REFRESH_SLOW = 5000;
    public static final int REFRESH_SLOWER = 15_000;
}
