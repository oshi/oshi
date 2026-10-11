/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import oshi.util.PlatformEnum;

/**
 * Conditions shared by FFM test classes, for use with {@code @EnabledIf}. JUnit's {@code OS} enum has no DragonFly BSD
 * constant, so {@code @EnabledOnOs} cannot name it.
 */
public final class FfmTestConditions {

    private FfmTestConditions() {
    }

    /**
     * The platforms whose FFM implementation these tests run on.
     *
     * @return true on a tested platform
     */
    public static boolean isTestedPlatform() {
        return switch (PlatformEnum.getCurrentPlatform()) {
            case LINUX, MACOS, WINDOWS, FREEBSD, OPENBSD, DRAGONFLYBSD, SOLARIS, AIX -> true;
            default -> false;
        };
    }
}
