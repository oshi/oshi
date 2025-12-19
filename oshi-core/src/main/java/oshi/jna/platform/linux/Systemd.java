/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.linux;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * JNA bindings for libsystemd. This class should be considered non-API as it may be removed if/when its code is
 * incorporated into the JNA project.
 */
@ThreadSafe
public interface Systemd extends Library {

    Systemd INSTANCE = Native.load("systemd", Systemd.class);

    /**
     * Get start time of session
     *
     * @param session Session ID or null for current session
     * @param usec    Pointer to store microseconds since epoch
     * @return 0 on success, negative errno on failure
     */
    int sd_session_get_start_time(String session, LongByReference usec);

    /**
     * Get username of session
     *
     * @param session  Session ID or null for current session
     * @param username Pointer to store username string (must be freed)
     * @return 0 on success, negative errno on failure
     */
    int sd_session_get_username(String session, PointerByReference username);

    /**
     * Get TTY of session
     *
     * @param session Session ID or null for current session
     * @param tty     Pointer to store TTY string (must be freed)
     * @return 0 on success, negative errno on failure
     */
    int sd_session_get_tty(String session, PointerByReference tty);

    /**
     * Get remote host of session
     *
     * @param session     Session ID or null for current session
     * @param remote_host Pointer to store remote host string (must be freed)
     * @return 0 on success, negative errno on failure
     */
    int sd_session_get_remote_host(String session, PointerByReference remote_host);

    /**
     * Enumerate sessions
     *
     * @param sessions Pointer to store array of session IDs (must be freed)
     * @return Number of sessions on success, negative errno on failure
     */
    int sd_get_sessions(PointerByReference sessions);
}
