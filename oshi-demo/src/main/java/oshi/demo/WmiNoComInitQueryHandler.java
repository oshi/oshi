/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo;

import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Query handler class that avoids COM initialization overhead assuming COM is already initialized by the user.
 */
public class WmiNoComInitQueryHandler extends WmiQueryHandler {

    /**
     * Don't initialize COM, despite the method name. Overrides the superclass {@link WmiQueryHandler#initCOM()} method
     * to bypass COM initialization.
     */
    @Override
    public boolean initCOM() {
        return false;
    }
}
