/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.jna.platform.mac;

import com.sun.jna.Native;

import oshi.jna.platform.unix.CLibrary;

/**
 * System class. This class should be considered non-API as it may be removed
 * if/when its code is incorporated into the JNA project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface SystemB extends CLibrary, com.sun.jna.platform.mac.SystemB {
    SystemB INSTANCE = Native.load("System", SystemB.class);
}
