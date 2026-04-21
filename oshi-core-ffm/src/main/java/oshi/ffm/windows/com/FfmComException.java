/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows.com;

/**
 * Unchecked exception thrown when a COM operation fails with an unexpected HRESULT.
 */
public class FfmComException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int hresult;

    public FfmComException(String message, int hresult) {
        super(message + " (HRESULT: 0x" + Integer.toHexString(hresult) + ")");
        this.hresult = hresult;
    }

    public int getHresult() {
        return hresult;
    }
}
