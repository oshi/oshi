/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.jna.platform.windows;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * Windows Performance Data Helper. This class should be considered non-API as
 * it may be removed if/when its code is incorporated into the JNA project.
 * 
 * @author widdis[at]gmail[dot]com
 */
public interface Pdh extends Library {
    Pdh INSTANCE = (Pdh) Native.loadLibrary("Pdh", Pdh.class);

    // Counter return types
    int PDH_FMT_LONG = 0x00000100; // Native Long

    int PDH_FMT_DOUBLE = 0x00000200; // double

    int PDH_FMT_LARGE = 0x00000400; // 64 bit long
    // These can be combined with above types with bitwise OR

    int PDH_FMT_NOSCALE = 0x00001000; // don't scale

    int PDH_FMT_1000 = 0x00002000; // multiply by 1000

    int PDH_FMT_NOCAP100 = 0x00008000; // don't cap at 100

    /**
     * Union included in return value of {@link PdhFmtCounterValue}
     */
    class ValueUnion extends Union {
        public int longValue;

        public double doubleValue;

        public long largeValue;

        public String AnsiStringValue;

        public WString WideStringValue;
    }

    /**
     * Holds the return value of a formatted data query.
     */
    class PdhFmtCounterValue extends Structure {
        public DWORD cStatus;

        public ValueUnion value;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "cStatus", "value" });
        }
    }

    /**
     * Creates a new query that is used to manage the collection of performance
     * data.
     * 
     * @param szDataSource
     *            Null-terminated string that specifies the name of the log file
     *            from which to retrieve performance data. If NULL, performance
     *            data is collected from a real-time data source.
     * @param dwUserData
     *            User-defined value to associate with this query.
     * @param phQuery
     *            Handle to the query. You use this handle in subsequent calls.
     * @return If the function succeeds, the return value is zero. If the
     *         function fails, the return value is a system error code or a PDH
     *         error code.
     */
    int PdhOpenQuery(String szDataSource, IntByReference dwUserData, PointerByReference phQuery);

    /**
     * Adds the specified language-neutral counter to the query.
     * 
     * @param pointer
     *            Handle to the query to which you want to add the counter. This
     *            handle is returned by the
     *            {@link #PdhOpenQuery(String, IntByReference, PointerByReference)}
     *            function.
     * @param counterPath
     *            Null-terminated string that contains the counter path.
     * @param dwUserData
     *            User-defined value.
     * @param phCounter
     *            Handle to the counter that was added to the query. You may
     *            need to reference this handle in subsequent calls.
     * @return If the function succeeds, the return value is zero. If the
     *         function fails, the return value is a system error code or a PDH
     *         error code.
     */
    int PdhAddEnglishCounterA(Pointer pointer, String counterPath, IntByReference dwUserData,
            PointerByReference phCounter);

    /**
     * Collects the current raw data value for all counters in the specified
     * query and updates the status code of each counter.
     * 
     * @param pointer
     *            Handle of the query for which you want to collect data. The
     *            {@link #PdhOpenQuery(String, IntByReference, PointerByReference)}
     *            function returns this handle.
     * @return If the function succeeds, the return value is nonzero. If the
     *         function fails, the return value is zero and errno is set.
     */
    int PdhCollectQueryData(Pointer pointer);

    /**
     * Computes a displayable value for the specified counter.
     * 
     * @param pointer
     *            Handle of the counter for which you want to compute a
     *            displayable value. The
     *            {@link #PdhAddEnglishCounterA(Pointer, String, IntByReference, PointerByReference)}
     *            function returns this handle.
     * @param dwFormat
     *            Determines the data type of the formatted value.
     * @param lpdwType
     *            Receives the counter type. This parameter is optional.
     * @param pValue
     *            A {@link PdhFmtCounterValue} structure that receives the
     *            counter value.
     * @return If the function succeeds, the return value is zero. If the
     *         function fails, the return value is a system error code or a PDH
     *         error code.
     */
    int PdhGetFormattedCounterValue(Pointer pointer, int dwFormat, IntByReference lpdwType, PdhFmtCounterValue pValue);

    /**
     * Closes all counters contained in the specified query, closes all handles
     * related to the query, and frees all memory associated with the query.
     * 
     * @param pointer
     *            Handle to the query to close. This handle is returned by the
     *            {@link #PdhOpenQuery(String, IntByReference, PointerByReference)}
     *            function.
     * @return If the function succeeds, the return value is zero. If the
     *         function fails, the return value is a system error code or a PDH
     *         error code.
     */
    int PdhCloseQuery(Pointer pointer);
}
