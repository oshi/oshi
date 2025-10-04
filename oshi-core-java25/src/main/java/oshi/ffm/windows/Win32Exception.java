/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

public class Win32Exception extends RuntimeException {

    private final int errorCode;

    public Win32Exception(int errorCode) {
        super(formatMessage(errorCode));
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    private static String formatMessage(int errorCode) {
        return String.format("Win32 API call failed with error code 0x%08X (%d)", errorCode, errorCode);
    }
}
