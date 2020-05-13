/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.jna.platform.windows;

import com.sun.jna.Memory; // NOSONAR squid:S1191
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

public interface WinNT extends com.sun.jna.platform.win32.WinNT {
    /**
     * The TOKEN_PRIMARY_GROUP structure specifies a group security identifier (SID)
     * for an access token.
     */
    @FieldOrder({ "PrimaryGroup" })
    public static class TOKEN_PRIMARY_GROUP extends Structure {
        /**
         * A pointer to a SID structure representing a group that will become the
         * primary group of any objects created by a process using this access token.
         * The SID must be one of the group SIDs already in the token.
         */
        public PSID.ByReference PrimaryGroup;

        public TOKEN_PRIMARY_GROUP() {
            super();
        }

        public TOKEN_PRIMARY_GROUP(Pointer memory) {
            super(memory);
            read();
        }

        public TOKEN_PRIMARY_GROUP(int size) {
            super(new Memory(size));
        }
    }
}