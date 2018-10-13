package oshi.hardware.platform.mac;

import oshi.hardware.common.AbstractUser;

public class MacUser extends AbstractUser {
        public MacUser(String userName, int userId, String homeDir, String realName) {
                super(userName, userId, homeDir, realName);
        }
}
