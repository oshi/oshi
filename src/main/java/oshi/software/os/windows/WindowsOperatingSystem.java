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
package oshi.software.os.windows;

import oshi.software.common.AbstractOperatingSystem;

public class WindowsOperatingSystem extends AbstractOperatingSystem {

    public WindowsOperatingSystem() {
        this.manufacturer = "Microsoft";
        this.family = "Windows";
        this.version = new WindowsOSVersionInfoEx();
    }

    @Override
    public long getOpenDescriptors() {
        return -1L;
    }

    @Override
    public long getMaxDescriptors() {
        return -1L;
    }
}
