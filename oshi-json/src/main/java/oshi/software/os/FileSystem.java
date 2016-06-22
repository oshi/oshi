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
     * The current number of open file descriptors. A file descriptor is an
     * abstract handle used to access I/O resources such as files and network
     * connections. On UNIX-based systems there is a system-wide limit on the
     * number of open file descriptors.
     *
     * On Windows systems, this method returns 0. While Windows handles are
     * conceptually similar to file descriptors, they may also refer to a number
     * of non-I/O related objects, and there does not appear to be a system-wide
     * limit for open handles.
     *
     * @return The number of open file descriptors if available, 0 otherwise.
     */
    long getOpenFileDescriptors();

    /**
     * The maximum number of open file descriptors. A file descriptor is an
     * abstract handle used to access I/O resources such as files and network
     * connections. On UNIX-based systems there is a system-wide limit on the
     * number of open file descriptors.
     *
     * On Windows systems, this method returns 0. While Windows handles are
     * conceptually similar to file descriptors, they may also refer to a number
     * of non-I/O related objects, and there does not appear to be a system-wide
     * limit for open handles.
     *
     * @return The maximum number of file descriptors if available, 0 otherwise.
     */
    long getMaxFileDescriptors();
}
