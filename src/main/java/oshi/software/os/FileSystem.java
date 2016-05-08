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
package oshi.software.os;

import oshi.json.OshiJsonObject;

public interface FileSystem extends OshiJsonObject {

    /**
     * Get file stores on this machine
     * 
     * @return Array of {@link OSFileStore} objects
     */
    OSFileStore[] getFileStores();

    /**
     * Open file descriptors.
     *
     * @return long
     */
    long getOpenFileDescriptors();

    /**
     * Maximum file descriptors.
     *
     * @return long
     */
    long getMaxFileDescriptors();
}
