/*
 * Copyright (c) 2016 com.github.dblock.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * enrico[dot]bianchi[at]gmail[dot]com
 *    com.github.dblock - initial API and implementation and/or initial documentation
 */
package oshi.software.os;

import oshi.json.OshiJsonObject;
import oshi.software.common.OSFileStore;

public interface FileSystem extends OshiJsonObject {

    /**
     * Get file stores on this machine
     * 
     * @return Array of {@link OSFileStore} objects
     */
    public OSFileStore[] getFileStores();
}
