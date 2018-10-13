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
package oshi.hardware.platform.linux;

import oshi.hardware.common.AbstractUser;
import oshi.util.ExecutingCommand;

/**
 * A Linux User
 * Gets basic information about the currently logged in user.
 * @author : BilalAM
 */
public class LinuxUser extends AbstractUser {
        private static final String WHO_AM_I = "whoami";
        private static final String GETENT_PASSWD = "getent passwd";
        private static String userName;
        private static String[] values;

        public LinuxUser(String userName, int userId, String homeDir, String realName) {
                super(userName, userId, homeDir, realName);
        }

        /**
         * The command getent passwd returns a list of lines , each line consists of the following
         * information seperated by ':'
         *
         * ·   login name
         * ·   optional encrypted password
         * ·   numerical user ID
         * ·   numerical group ID
         * ·   user name or comment field
         * ·   user home directory
         * ·   optional user command interpreter
         */
        static {
                userName = ExecutingCommand.getFirstAnswer(WHO_AM_I);
                for(String line : ExecutingCommand.runNative(GETENT_PASSWD)){
                        if(line.contains(userName)){
                                values = line.split(":");
                        }
                }
        }
        public static LinuxUser getUser(){
              return new LinuxUser(values[0],Integer.parseInt(values[2]),values[5],values[4]);
        }


}
