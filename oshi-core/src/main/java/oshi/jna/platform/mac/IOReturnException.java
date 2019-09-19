/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.jna.platform.mac;

/**
 * Exception encapsulating {@code IOReturn} I/O Kit Error Return Values, defined
 * as {@code kern_return_t} values in {@code IOKit/IOReturn.h}
 * <p>
 * The return value supplies information in three separate bit fields: the high
 * 6 bits specify the system in which the error occurred, the next 12 bits
 * specify the subsystem, and the final 14 bits specify the error code itself.
 */
public class IOReturnException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private int ioReturn;

    /**
     * New exception from {@code kern_return_t}
     *
     * @param kr
     *            The return value
     */
    public IOReturnException(int kr) {
        this(kr, formatMessage(kr));
    }

    /**
     * New exception from {@code kern_return_t} with specified message
     *
     * @param kr
     *            The return value
     * @param msg
     *            The exception message
     */
    protected IOReturnException(int kr, String msg) {
        super(msg);
        this.ioReturn = kr;
    }

    /**
     * @return the IOReturn code
     */
    public int getIOReturnCode() {
        return ioReturn;
    }

    /**
     * The high 6 bits of the return value encode the system.
     *
     * @param kr
     *            The return value
     * @return the system value
     */
    public static int getSystem(int kr) {
        return (kr >> 26) & 0x3f;
    }

    /**
     * The middle 12 bits of the return value encode the subsystem.
     *
     * @param kr
     *            The return value
     * @return the subsystem value
     */
    public static int getSubSystem(int kr) {
        return (kr >> 14) & 0xfff;
    }

    /**
     * The low 14 bits of the return value encode the return code.
     *
     * @param kr
     *            The return value
     * @return the return code
     */
    public static int getCode(int kr) {
        return kr & 0x3fff;
    }

    private static String formatMessage(int kr) {
        return "IOReturn error code: " + kr + " (system=" + getSystem(kr) + ", subSystem=" + getSubSystem(kr)
                + ", code=" + getCode(kr) + ")";
    }
}
