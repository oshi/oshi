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
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.util.platform.mac;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import oshi.jna.platform.mac.CoreFoundation;

/**
 * Provides utilities for Core Foundations
 * 
 * @author widdis[at]gmail[dot]com
 */
public class CfUtil {
    /**
     * Convert a pointer representing a Core Foundations String into its string
     * 
     * @param p
     *            The pointer to a CFString
     * @return The corresponding string
     */
    public static String cfPointerToString(Pointer p) {
        long length = CoreFoundation.INSTANCE.CFStringGetLength(p);
        long maxSize = CoreFoundation.INSTANCE.CFStringGetMaximumSizeForEncoding(length, CoreFoundation.UTF_8);
        Pointer buf = new Memory(maxSize);
        CoreFoundation.INSTANCE.CFStringGetCString(p, buf, maxSize, CoreFoundation.UTF_8);
        return buf.getString(0);
    }
}