package oshi.hardware.platform.windows;

import oshi.hardware.common.AbstractUser;

public class WindowsUser extends AbstractUser {
        public WindowsUser(String userName, int userId, String homeDir, String realName) {
                super(userName, userId, homeDir, realName);
        }
}
