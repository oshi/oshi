/*
 * MIT License
 *
 * Copyright (c) 2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.jna.platform.unix;

import com.sun.jna.Native;

/**
 * Exception encapsulating {@code Kstat2} Error Return Values, defined as
 * {@code kstat2_status} values in {@code kstat2.h}
 */
public class Kstat2StatusException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int kstat2Status;

    /**
     * New exception from {@code kstat2_status}
     *
     * @param ks
     *            The return value
     */
    public Kstat2StatusException(int ks) {
        this(ks, formatMessage(ks));
    }

    /**
     * New exception from {@code kstat2_status} with specified message
     *
     * @param ks
     *            The return value
     * @param msg
     *            The exception message
     */
    protected Kstat2StatusException(int ks, String msg) {
        super(msg);
        this.kstat2Status = ks;
    }

    /**
     * @return the Kstat2Status code
     */
    public int getKstat2Status() {
        return kstat2Status;
    }

    private static String formatMessage(int ks) {
        String status = Kstat2.INSTANCE.kstat2_status_string(ks);
        if (ks == Kstat2.KSTAT2_S_SYS_FAIL) {
            status += " (errno=" + Native.getLastError() + ")";
        }
        return "Kstat2Status error code " + ks + ": " + status;
    }
}
