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

import com.sun.jna.IntegerType; // NOSONAR squid:S1191
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.Union;

public interface WinNT extends com.sun.jna.platform.win32.WinNT {
    /**
     * The TOKEN_PRIMARY_GROUP structure specifies a group security identifier (SID)
     * for an access token.
     */
    @FieldOrder({ "PrimaryGroup" })
    class TOKEN_PRIMARY_GROUP extends Structure {
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

    /**
     * A 64-bit integer;
     */
    @FieldOrder({ "u" })
    class LARGE_INTEGER extends Structure implements Comparable<LARGE_INTEGER> {
        public static class ByReference extends LARGE_INTEGER implements Structure.ByReference {
        }

        @FieldOrder({ "LowPart", "HighPart" })
        public static class LowHigh extends Structure {
            public DWORD LowPart;
            public DWORD HighPart;

            public LowHigh() {
                super();
            }

            public LowHigh(long value) {
                this(new DWORD(value & 0xFFFFFFFFL), new DWORD((value >> 32) & 0xFFFFFFFFL));
            }

            public LowHigh(DWORD low, DWORD high) {
                LowPart = low;
                HighPart = high;
            }

            public long longValue() {
                long loValue = LowPart.longValue();
                long hiValue = HighPart.longValue();
                return ((hiValue << 32) & 0xFFFFFFFF00000000L) | (loValue & 0xFFFFFFFFL);
            }

            @Override
            public String toString() {
                if ((LowPart == null) || (HighPart == null)) {
                    return "null";
                } else {
                    return Long.toString(longValue());
                }
            }
        }

        public static class UNION extends Union {
            public LowHigh lh;
            public long value;

            public UNION() {
                super();
            }

            public UNION(long value) {
                this.value = value;
                this.lh = new LowHigh(value);
            }

            public long longValue() {
                return value;
            }

            @Override
            public void read() {
                readField("lh");
                readField("value");
            }

            @Override
            public String toString() {
                return Long.toString(longValue());
            }
        }

        public UNION u;

        public LARGE_INTEGER() {
            super();
        }

        public LARGE_INTEGER(long value) {
            this.u = new UNION(value);
        }

        /**
         * Low DWORD.
         *
         * @return Low DWORD value
         */
        public DWORD getLow() {
            return u.lh.LowPart;
        }

        /**
         * High DWORD.
         *
         * @return High DWORD value
         */
        public DWORD getHigh() {
            return u.lh.HighPart;
        }

        /**
         * 64-bit value.
         *
         * @return The 64-bit value.
         */
        public long getValue() {
            return u.value;
        }

        @Override
        public int compareTo(LARGE_INTEGER other) {
            return compare(this, other);
        }

        @Override
        public String toString() {
            return (u == null) ? "null" : Long.toString(getValue());
        }

        /**
         * Compares 2 LARGE_INTEGER values - - <B>Note:</B> a {@code null} value is
         * considered <U>greater</U> than any non-{@code null} one (i.e., {@code null}
         * values are &quot;pushed&quot; to the end of a sorted array / list of values)
         *
         * @param v1
         *            The 1st value
         * @param v2
         *            The 2nd value
         * @return 0 if values are equal (including if <U>both</U> are {@code null},
         *         negative if 1st value less than 2nd one, positive otherwise.
         *         <B>Note:</B> the comparison uses the {@link #getValue()}.
         * @see IntegerType#compare(long, long)
         */
        public static int compare(LARGE_INTEGER v1, LARGE_INTEGER v2) {
            if (v1 == v2) {
                return 0;
            } else if (v1 == null) {
                return 1; // v2 cannot be null or v1 == v2 would hold
            } else if (v2 == null) {
                return -1;
            } else {
                return IntegerType.compare(v1.getValue(), v2.getValue());
            }
        }

        /**
         * Compares a LARGE_INTEGER value with a {@code long} one. <B>Note:</B> if the
         * LARGE_INTEGER value is {@code null} then it is consider <U>greater</U> than
         * any {@code long} value.
         *
         * @param v1
         *            The {@link LARGE_INTEGER} value
         * @param v2
         *            The {@code long} value
         * @return 0 if values are equal, negative if 1st value less than 2nd one,
         *         positive otherwise. <B>Note:</B> the comparison uses the
         *         {@link #getValue()}.
         * @see IntegerType#compare(long, long)
         */
        public static int compare(LARGE_INTEGER v1, long v2) {
            if (v1 == null) {
                return 1;
            } else {
                return IntegerType.compare(v1.getValue(), v2);
            }
        }
    }
}
