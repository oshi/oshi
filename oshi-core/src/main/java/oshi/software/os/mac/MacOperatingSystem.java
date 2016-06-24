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
package oshi.software.os.mac;

import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;

public class MacOperatingSystem extends AbstractOperatingSystem {

    private static final long serialVersionUID = 1L;

    public MacOperatingSystem() {
        this.manufacturer = "Apple";
        this.family = System.getProperty("os.name");
        this.version = new MacOSVersionInfoEx();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem() {
        return new MacFileSystem();
    }
}
