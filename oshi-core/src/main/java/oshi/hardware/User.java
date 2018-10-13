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
package oshi.hardware;

/**
 * @author: BilalAM
 */
public interface User {

        /**
         * Gets the user name currently logged in
         * @return : The user name.
         */
        String getUserName();

        /**
         * Gets the current user's Id.
         * @return : The user id
         */
        int getUserId();

        /**
         * Gets the user home directory path
         * @return : The user's home directory path
         */
        String getHomeDir();

        /**
         * Gets the logged-in user's real name (results maybe equal to getUserName() )
         * @return : The 'real' name of the user.
         */
        String getRealName();
}
