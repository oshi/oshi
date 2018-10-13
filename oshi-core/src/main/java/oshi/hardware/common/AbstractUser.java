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
package oshi.hardware.common;

import oshi.hardware.User;

/**
 * An abstract user class having information about the current user.
 *
 * @author : BilalAM
 */
public abstract class AbstractUser implements User {
        private String userName;
        private int userId;
        private String homeDir;

        public AbstractUser(String userName, int userId, String homeDir, String realName) {
                this.userName = userName;
                this.userId = userId;
                this.homeDir = homeDir;
                this.realName = realName;
        }

        private String realName;

        @Override
        public String getUserName() {
                return userName;
        }

        public void setUserName(String userName) {
                this.userName = userName;
        }

        @Override
        public int getUserId() {
                return userId;
        }

        public void setUserId(int userId) {
                this.userId = userId;
        }

        @Override
        public String getHomeDir() {
                return homeDir;
        }

        public void setHomeDir(String homeDir) {
                this.homeDir = homeDir;
        }

        @Override
        public String getRealName() {
                return realName;
        }

        public void setRealName(String realName) {
                this.realName = realName;
        }

        @Override
        public String toString(){
                StringBuilder builder = new StringBuilder();
                builder.append("User@");
                builder.append(Integer.toHexString(hashCode()));
                builder.append(" [name=");
                builder.append(userName);
                builder.append(", id=");
                builder.append(userId);
                builder.append(", home-dir=");
                builder.append(homeDir);
                builder.append(", real-name=");
                builder.append(realName);
                builder.append(']');
                return builder.toString();
        }

}
