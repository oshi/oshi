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
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.jna.platform.mac;

import com.sun.jna.Native;

/**
 * Power Supply stats. This class should be considered non-API as it may be
 * removed if/when its code is incorporated into the JNA project.
 * 
 * @author widdis[at]gmail[dot]com
 */
public interface SystemB extends com.sun.jna.platform.mac.SystemB {
    SystemB INSTANCE = (SystemB) Native.loadLibrary("System", SystemB.class);

    int mach_task_self();
}
