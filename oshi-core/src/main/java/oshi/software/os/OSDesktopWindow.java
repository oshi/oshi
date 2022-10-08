/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import java.awt.Rectangle;

import com.sun.jna.platform.win32.WinDef.HWND;

import oshi.annotation.concurrent.Immutable;

/**
 * This class encapsulates information about a window on the operating system's GUI desktop
 */
@Immutable
public class OSDesktopWindow {
    private final long windowId;
    private final String title;
    private final String command;
    private final Rectangle locAndSize;
    private final long owningProcessId;
    private final int order;
    private final boolean visible;

    public OSDesktopWindow(long windowId, String title, String command, Rectangle locAndSize, long owningProcessId,
            int order, boolean visible) {
        super();
        this.windowId = windowId;
        this.title = title;
        this.command = command;
        this.locAndSize = locAndSize;
        this.owningProcessId = owningProcessId;
        this.order = order;
        this.visible = visible;
    }

    /**
     * Gets the operating system's handle, window ID, or other unique identifier for this window.
     * <p>
     * On Winodws, this can be converted to a {@link HWND} using {@code new HWND(new Pointer(windowId))}. On macOS, this
     * is the Core Graphics Window ID. On Unix-like systems, this is the X11 Window ID.
     *
     * @return the windowId
     */
    public long getWindowId() {
        return windowId;
    }

    /**
     * Gets the Window title, if any.
     *
     * @return the title, which may be an empty string if the window has no title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the command name (possibly the full file path) of the window's executable program, if known.
     *
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * Gets a {@link Rectangle} representing the window's location and size.
     *
     * @return the location and size
     */
    public Rectangle getLocAndSize() {
        return locAndSize;
    }

    /**
     * Gets the process ID of the process which owns this window, if known.
     *
     * @return the owningProcessId
     */
    public long getOwningProcessId() {
        return owningProcessId;
    }

    /**
     * Makes a best effort to get the order in which this window appears on the desktop. Higher values are more in the
     * foreground.
     * <p>
     * On Windows, this represents the Z Order of the window, and is not guaranteed to be atomic, as it could be
     * impacted by race conditions.
     * <p>
     * On macOS this is the window layer. Note that multiple windows may share the same layer.
     * <p>
     * On X11 this represents the stacking order of the windows.
     *
     * @return a best effort identification of the window's Z order relative to other windows
     */
    public int getOrder() {
        return order;
    }

    /**
     * Makes a best effort to report whether the window is visible to the user. A "visible" window may be completely
     * transparent.
     *
     * @return {@code true} if the window is visible to users or if visibility can not be determined, {@code false}
     *         otherwise.
     */
    public boolean isVisible() {
        return visible;
    }

    @Override
    public String toString() {
        return "OSDesktopWindow [windowId=" + windowId + ", title=" + title + ", command=" + command + ", locAndSize="
                + locAndSize.toString() + ", owningProcessId=" + owningProcessId + ", order=" + order + ", visible="
                + visible + "]";
    }
}
