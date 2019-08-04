/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.os.linux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.software.os.OSUser;
import oshi.util.ExecutingCommand;

/**
 * <p>
 * LinuxUserGroupInfo class.
 * </p>
 */
public class LinuxUserGroupInfo {

    // Temporarily cache users and groups
    private Map<String, OSUser> usersIdMap = new HashMap<>();
    private Map<String, String> groupsIdMap = new HashMap<>();

    /**
     * <p>
     * getUser.
     * </p>
     *
     * @param userId
     *            a {@link java.lang.String} object.
     * @return a {@link oshi.software.os.OSUser} object.
     */
    public OSUser getUser(String userId) {
        if (this.usersIdMap.isEmpty()) {
            cacheUsers();
        }
        OSUser user = this.usersIdMap.get(userId);
        if (user != null) {
            return user;
        }
        user = new OSUser();
        user.setUserId(userId);
        user.setUserName("Unknown");
        return user;
    }

    /**
     * <p>
     * getGroupName.
     * </p>
     *
     * @param groupId
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getGroupName(String groupId) {
        if (this.groupsIdMap.isEmpty()) {
            cacheGroups();
        }
        String group = this.groupsIdMap.get(groupId);
        return (group != null) ? group : "Unknown";
    }

    private void cacheUsers() {
        List<String> passwd = ExecutingCommand.runNative("getent passwd");
        // see man 5 passwd for the fields
        for (String entry : passwd) {
            String[] split = entry.split(":");
            if (split.length < 3) {
                continue;
            }
            String userName = split[0];
            String uid = split[2];
            // it is allowed to have multiple entries for the same userId,
            // we use the first one
            if (!this.usersIdMap.containsKey(uid)) {
                OSUser user = new OSUser();
                user.setUserId(uid);
                user.setUserName(userName);
                this.usersIdMap.put(uid, user);
            }
        }
    }

    private void cacheGroups() {
        List<String> group = ExecutingCommand.runNative("getent group");
        // see man 5 group for the fields
        for (String entry : group) {
            String[] split = entry.split(":");
            if (split.length < 3) {
                continue;
            }
            String groupName = split[0];
            String gid = split[2];
            if (!this.groupsIdMap.containsKey(gid)) {
                this.groupsIdMap.put(gid, groupName);
            }
        }
    }
}
