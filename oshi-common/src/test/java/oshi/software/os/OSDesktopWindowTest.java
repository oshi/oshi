/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

class OSDesktopWindowTest {

    private static final OSDesktopWindow WINDOW = new OSDesktopWindow(42L, "My Window", "/usr/bin/app",
            new Rectangle(10, 20, 800, 600), 1234L, 5, true);

    @Test
    void testGetters() {
        assertThat(WINDOW.getWindowId(), is(42L));
        assertThat(WINDOW.getTitle(), is("My Window"));
        assertThat(WINDOW.getCommand(), is("/usr/bin/app"));
        assertThat(WINDOW.getLocAndSize(), is(new Rectangle(10, 20, 800, 600)));
        assertThat(WINDOW.getOwningProcessId(), is(1234L));
        assertThat(WINDOW.getOrder(), is(5));
        assertThat(WINDOW.isVisible(), is(true));
    }

    @Test
    void testLocAndSizeIsDefensivelyCopied() {
        Rectangle supplied = new Rectangle(1, 2, 3, 4);
        OSDesktopWindow window = new OSDesktopWindow(1L, "t", "c", supplied, 2L, 0, true);

        // Mutating the caller's rectangle must not reach into the window
        supplied.setBounds(99, 99, 99, 99);
        assertThat(window.getLocAndSize(), is(new Rectangle(1, 2, 3, 4)));

        // Nor must mutating what the getter handed back
        window.getLocAndSize().setBounds(77, 77, 77, 77);
        assertThat(window.getLocAndSize(), is(new Rectangle(1, 2, 3, 4)));
    }

    @Test
    void testToString() {
        String s = WINDOW.toString();
        assertThat(s, containsString("42"));
        assertThat(s, containsString("My Window"));
        assertThat(s, containsString("1234"));
    }
}
