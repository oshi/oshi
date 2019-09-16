/*
 * Copyright (c) 2019 Daniel Widdis
 *
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0. (starting with JNA version 4.0.0).
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "AL2.0".
 */
package oshi.jna.platform.mac;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import com.sun.jna.ptr.NativeLongByReference;

public interface IOKit extends com.sun.jna.platform.mac.IOKit {

    IOKit INSTANCE = Native.load("IOKit", IOKit.class);

    /*
     * Beta/Non-API do not commit to JNA
     */
    int IOConnectCallStructMethod(IOConnect connection, int selector, Structure inputStructure,
            NativeLong structureInputSize, Structure outputStructure, NativeLongByReference structureOutputSize);
}
