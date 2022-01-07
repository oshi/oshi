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

import java.io.Closeable;

import com.sun.jna.Library; // NOSONAR squid:S1191
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.Union;
import com.sun.jna.platform.unix.solaris.LibKstat.Kstat;
import com.sun.jna.platform.unix.solaris.LibKstat.KstatCtl;
import com.sun.jna.ptr.PointerByReference;

/**
 * Kstat2 library. The kstat2 facility is a general-purpose mechanism for
 * providing kernel statistics to users.
 * <p>
 * Kstat2 is available in Solaris 11.4 and later.
 */
public interface LibKstat2 extends Library {

    LibKstat2 INSTANCE = Native.load("kstat2", LibKstat2.class);

    // enum kstat2_status
    int KSTAT2_S_OK = 0; // Request was successful
    int KSTAT2_S_NO_PERM = 1; // Insufficient permissions for request
    int KSTAT2_S_NO_MEM = 2; // Insufficient memory available
    int KSTAT2_S_NO_SPACE = 3; // No space available for operation
    int KSTAT2_S_INVAL_ARG = 4; // Invalid argument supplied
    int KSTAT2_S_INVAL_STATE = 5; // Invalid state for this request
    int KSTAT2_S_INVAL_TYPE = 6; // Invalid data type found
    int KSTAT2_S_NOT_FOUND = 7; // Resource not found
    int KSTAT2_S_CONC_MOD = 8; // Concurrent modification of map detected
    int KSTAT2_S_DEL_MAP = 9; // Referenced map has been deleted
    int KSTAT2_S_SYS_FAIL = 10; // System call has failed, see errno

    // enum kstat2_nv_kind
    int KSTAT2_NVK_SYS = 0x01; // System kstat value type
    int KSTAT2_NVK_USR = 0x02; // User-supplied value type
    int KSTAT2_NVK_MAP = 0x04; // Sub-map value type
    int KSTAT2_NVK_ALL = 0x07; // All value types (only for iteration)

    // enum kstat2_match_type
    int KSTAT2_M_STRING = 0;
    int KSTAT2_M_GLOB = 1;
    int KSTAT2_M_RE = 2;

    // enum kstat2_map_flag
    int KSTAT2_MAPF_NONE = 0x00; // No flags present
    int KSTAT2_MAPF_DORM = 0x01; // Kstat is dormant

    // enum kstat2_metatype
    int KSTAT2_MT_NONE = 0; // No type
    int KSTAT2_MT_QUEUE = 1; // Queue data
    int KSTAT2_MT_IO = 2; // IO data
    int KSTAT2_MT_INTR = 3; // Interrupt data
    int KSTAT2_MT_TIMER = 4; // Timer data (v1 compatibility)
    int KSTAT2_MT_HIST = 5; // Histogram data

    // enum kstat2_metaflag
    int KSTAT2_MF_NONE = 0x00; // No flags present
    int KSTAT2_MF_STABLE = 0x01; // Kstat is stable across Solaris releases
    int KSTAT2_MF_PRIV = 0x02; // Kstat has privileged read access

    // enum kstat2_nv_type
    int KSTAT2_NVVT_MAP = 0; // Nested Name/Value map
    int KSTAT2_NVVT_INT = 1; // 64-bit unsigned integer
    int KSTAT2_NVVT_INTS = 2; // Array of 64-bit unsigned integers
    int KSTAT2_NVVT_STR = 3; // Null-terminated C string
    int KSTAT2_NVVT_STRS = 4; // Array of null-terminated C strings

    // enum kstat2_nv_flag {
    int KSTAT2_NVF_NONE = 0x00; // No flags present
    int KSTAT2_NVF_INVAL = 0x01; // Value is invalid
    int _KSTAT2_NVF_V1ONLY = 0x8000; // Private flag indicating v1 kstat

    // enum kstat2_nv_metatype
    int KSTAT2_NVMT_UNK = 0; // Unknown type
    int KSTAT2_NVMT_ID = 1; // Identifier, e.g. CPU type, CPU chip ID
    int KSTAT2_NVMT_CNT = 2; // Count, e.g. network packets, disk blocks
    int KSTAT2_NVMT_T_EPOCH = 3; // Time since UNIX epoch
    int KSTAT2_NVMT_T_REL = 4; // Time relative to boot
    int KSTAT2_NVMT_T_ACC = 5; // Accumulated time since boot
    int KSTAT2_NVMT_PCT = 6; // Percentage, 0-100
    int KSTAT2_NVMT_ADDR = 7; // Memory address
    int KSTAT2_NVMT_TEMP_C = 8; // Temperature in centigrade
    int KSTAT2_NVMT_RPM = 9; // Revolutions per minute
    int KSTAT2_NVMT_VOLT = 10; // Voltage
    int KSTAT2_NVMT_WATT = 11; // Power consumption
    int KSTAT2_NVMT_CURR = 12; // Current
    int KSTAT2_NVMT_BYTES = 13; // Byte count
    int KSTAT2_NVMT_BITS = 14; // Bit count
    int KSTAT2_NVMT_STATE = 15; // State, e.g. CPU state
    int KSTAT2_NVMT_FREQ = 16; // Frequency (Hz)
    int KSTAT2_NVMT_FLAGS = 17; // Bitwise flags
    int KSTAT2_NVMT_JOULE = 18; // Energy in Joules

    /**
     * Opaque kstat handle.
     */
    class Kstat2Handle extends PointerType implements Closeable {
        /**
         * Instantiate a new Kstat2Handle without any filtering. All of the system's
         * kstats will be available.
         */
        public Kstat2Handle() {
            this(null);
        }

        /**
         * Instantiate a new Kstat2Handle filtered with the provided matcher.
         * Restricting the number of kstats available will improve performance and
         * reduce the memory footprint.
         *
         * @param matchers
         *            A list of matchers
         */
        public Kstat2Handle(Kstat2MatcherList matchers) {
            super();
            int ks = INSTANCE.kstat2_open(this, matchers);
            if (ks != KSTAT2_S_OK) {
                throw new Kstat2StatusException(ks);
            }
        }

        /**
         * Synchronises the user's view with that of the kernel. The kernel may at any
         * point add or remove kstats, causing the user's view of the available kstats
         * to become out of date.
         *
         * @return Upon successful completion, returns a int value of
         *         {@link KSTAT2_S_OK}. If an error occurs a value other than
         *         KSTAT2_S_OK is returned.
         */
        public int update() {
            return INSTANCE.kstat2_update(this);
        }

        @Override
        public void close() {
            int ks = INSTANCE.kstat2_close(this);
            if (ks != KSTAT2_S_OK) {
                throw new Kstat2StatusException(ks);
            }
        }
    }

    /**
     * Opaque kstat match list.
     */
    class Kstat2MatcherList extends PointerType implements Closeable {
        private PointerByReference ref = new PointerByReference();

        public Kstat2MatcherList() {
            super();
            int ks = INSTANCE.kstat2_alloc_matcher_list(ref);
            if (ks != KSTAT2_S_OK) {
                throw new Kstat2StatusException(ks);
            }
            this.setPointer(ref.getValue());
        }

        public void addMatcher(int type, String match) {
            int ks = INSTANCE.kstat2_add_matcher(type, match, this);
            if (ks != KSTAT2_S_OK) {
                throw new Kstat2StatusException(ks);
            }
        }

        @Override
        public void close() {
            int ks = INSTANCE.kstat2_free_matcher_list(ref);
            if (ks != KSTAT2_S_OK) {
                throw new Kstat2StatusException(ks);
            }
        }
    }

    /**
     * Opaque kstat map handle.
     */
    class Kstat2Map extends PointerType {
    }

    /**
     * Opaque map iterator handle.
     */
    class Kstat2MapIter extends PointerType {
    }

    /**
     * Opaque map reference.
     */
    class Kstat2MapRef extends PointerType {
    }

    /**
     * Metadata structure for maps
     */
    @FieldOrder({ "type", "flags", "desc" })
    class Kstat2MapMeta extends Structure {
        public short type; // Metadata type value
        public int flags; // Metadata flag values
        public String desc; // Descriptive string
    }

    /**
     * Immutable Name/Value pair.
     */
    @FieldOrder({ "name", "type", "kind", "flags", "data" })
    class Kstat2NV extends Structure {
        public String name; // Name of the pair
        public byte type; // Value type of the pair
        public byte kind; // Kind of the pair
        public short flags; // Flags of the pair
        public UNION data; // Data value

        public static class UNION extends Union {
            public Kstat2Map map;
            public long integerVal;
            public IntegersArr integers;
            public StringsArr strings;

            @FieldOrder({ "addr", "len" })
            public static class IntegersArr extends Structure {
                public Pointer addr;
                public int len; // length of array
            }

            @FieldOrder({ "addr", "len" })
            public static class StringsArr extends Structure {
                public Pointer addr;
                public int len; // length of array
            }
        }
    }

    /**
     * Immutable Name/value pair metadata.
     */
    @FieldOrder({ "type", "flags", "scale", "desc" })
    class Kstat2NVMeta extends Structure {
        public short type; // Metadata type value
        public int flags; // Metadata flag values
        public long scale; // Value units
        public String desc; // String
    }

    /**
     * Optional kstat data.
     */
    class Kstat2OptInfo extends Structure {
        public String id; // Optional kstats id
        public int estate; // Enabled state
        public int pstate; // Persisted state
        public boolean is_dflt_estate; // Default state?
    }

    /**
     * Initializes an opaque kstat2 handle that provides access to a specific view
     * of the kernel statistics.
     *
     * @param handle
     *            The handle to be initialized.
     * @param matchers
     *            Only kstats that match one or more of the provided matchers will
     *            be available. If a NULL or empty matcher list is provided, all of
     *            the system's kstats will be available, which is equivalent to
     *            calling the kstat2_open() function. Restricting the number of
     *            kstats available will improve performance and reduce the memory
     *            footprint.
     * @return Upon successful completion, returns a int value of
     *         {@link KSTAT2_S_OK}. If an error occurs a value other than
     *         KSTAT2_S_OK is returned.
     */
    int kstat2_open(Kstat2Handle handle, Kstat2MatcherList matchers);

    /**
     * Synchronises the user's view with that of the kernel. The kernel may at any
     * point add or remove kstats, causing the user's view of the available kstats
     * to become out of date. The kstat2_update() function should be called
     * periodically to resynchronise the two views.
     *
     * @param handle
     *            The handle to be updated.
     * @return Upon successful completion, returns a int value of
     *         {@link KSTAT2_S_OK}. If an error occurs a value other than
     *         KSTAT2_S_OK is returned.
     */
    int kstat2_update(Kstat2Handle handle);

    /**
     * The kstat2_close() function frees all resources that are associated with the
     * handle. It is the caller's responsibility to free any allocated matcher list
     * by calling the kstat2_free_matcher_list() function.
     */
    int kstat2_close(Kstat2Handle handle);

    /**
     * Allocates a new matcher list to allow matchers to be provided to the
     * {@link kstat2_open()} function.
     *
     * @param matchers
     * @return Upon successful completion, returns a int value of
     *         {@link KSTAT2_S_OK}. If an error occurs a value other than
     *         KSTAT2_S_OK is returned.
     */
    int kstat2_alloc_matcher_list(PointerByReference /* Kstat2MatcherList */ matchers);

    /**
     * Frees the resources associated with the matcher list.
     *
     * @param matchers
     *            A pointer to the {@link Kstat2MatcherList} to be freed.
     * @return Upon successful completion, returns a int value of
     *         {@link KSTAT2_S_OK}. If an error occurs a value other than
     *         KSTAT2_S_OK is returned.
     */
    int kstat2_free_matcher_list(PointerByReference /* Kstat2MatcherList */ matchers);

    /**
     * Adds matchers to the provided matcher list. Each call appends the new matcher
     * to the provided matcher list. Matches are on kstat URI, with the following
     * match types supported: {@link KSTAT2_M_STRING} which performs a direct
     * {@code strcmp} with the kstat URI, {@link KSTAT2_M_GLOB} which performs a
     * glob pattern match using {@code gmatch}, and {@link KSTAT2_M_RE} which
     * performs a Perl Compatible Regular Expression (PCRE) match using
     * {@code pcre_exec}.
     *
     * @param type
     *            The type of matcher, from the {@code kstat2_match_type_t}
     *            enumeration.
     * @param match
     *            The string to match.
     * @param matchers
     *            The list to which to append the matcher.
     */
    int kstat2_add_matcher(int type, String match, Kstat2MatcherList matchers);

    /**
     * Gives a descriptive error message for the supplied status value.
     *
     * @param status
     *            A value in the {@code kstat2_status} enumeration.
     * @return A descriptive string for the supplied status code.
     */
    String kstat2_status_string(int status);

    /**
     * Temp, remove
     */
    KstatCtl kstat_open();

    int kstat_read(KstatCtl kc, Kstat ksp, Object object);

    Kstat kstat_lookup(KstatCtl kc, String module, int instance2, String name);

    int kstat_chain_update(KstatCtl kc);

    Pointer kstat_data_lookup(Kstat ksp, String name);
}
