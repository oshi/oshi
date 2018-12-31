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

public class LinuxUserGroupInfo {

    private static final long CACHE_REFRESH_TIME_MS = 3L * 60000; // 3 minutes
    private static final UsersCache usersCache = new UsersCache();
    private static final GroupsCache groupsCache = new GroupsCache();

    public OSUser getUser(String userId) {
        return usersCache.getUser(userId);
    }

    public String getGroupName(String groupId) {
        return groupsCache.getGroup(groupId);
    }

    private static class UsersCache {

        private Map<String, OSUser> usersIdMap = new HashMap<>();
        private long lastRefreshedTimestamp;

        public OSUser getUser(String userId) {
            if (System.currentTimeMillis() - this.lastRefreshedTimestamp > CACHE_REFRESH_TIME_MS) {
                refresh();
            }
            OSUser osUser = this.usersIdMap.get(userId);
            if (osUser == null) {
                refresh();
                return this.usersIdMap.get(userId);
            }
            return osUser;
        }

        private void refresh() {
            this.usersIdMap.clear();
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
            this.lastRefreshedTimestamp = System.currentTimeMillis();
        }
    }

    private static class GroupsCache {
        private Map<String, String> groupsIdMap = new HashMap<>();
        private long lastRefreshedTimestamp;

        public String getGroup(String groupId) {
            if (System.currentTimeMillis() - this.lastRefreshedTimestamp > CACHE_REFRESH_TIME_MS) {
                refresh();
            }
            String groupName = this.groupsIdMap.get(groupId);
            if (groupName == null) {
                refresh();
                return this.groupsIdMap.get(groupId);
            }
            return groupName;
        }

        private void refresh() {
            this.groupsIdMap.clear();
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
            this.lastRefreshedTimestamp = System.currentTimeMillis();
        }
    }
}
