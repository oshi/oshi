/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.LibCAPI.size_t;
import com.sun.jna.ptr.ByReference;

public class NativeSizeTByReference extends ByReference {

    public NativeSizeTByReference() {
        this(new size_t());
    }

    public NativeSizeTByReference(size_t value) {
        super(Native.SIZE_T_SIZE);
        setValue(value);
    }

    public void setValue(size_t value) {
        if (Native.SIZE_T_SIZE > 4) {
            getPointer().setLong(0, value.longValue());
        } else {
            getPointer().setInt(0, value.intValue());
        }
    }

    public size_t getValue() {
        return new size_t(Native.SIZE_T_SIZE > 4 ? getPointer().getLong(0) : getPointer().getInt(0));
    }

    @Override
    public String toString() {
        // Can't mix types with ternary operator
        if (Native.SIZE_T_SIZE > 4) {
            return String.format("size_t@0x1$%x=0x%2$x (%2$d)", Pointer.nativeValue(getPointer()),
                    getValue().longValue());
        } else {
            return String.format("size_t@0x1$%x=0x%2$x (%2$d)", Pointer.nativeValue(getPointer()),
                    getValue().intValue());
        }
    }
}
