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
            if ((System.currentTimeMillis() - lastRefreshedTimestamp) > CACHE_REFRESH_TIME_MS) {
                refresh();
            }
            OSUser osUser = usersIdMap.get(userId);
            if (osUser == null) {
                refresh();
                return usersIdMap.get(userId);
            }
            return osUser;
        }

        private void refresh() {
            usersIdMap.clear();
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
                if (!usersIdMap.containsKey(uid)) {
                    OSUser user = new OSUser();
                    user.setUserId(uid);
                    user.setUserName(userName);
                    usersIdMap.put(uid, user);
                }
            }
            lastRefreshedTimestamp = System.currentTimeMillis();
        }
    }

    private static class GroupsCache {
        private Map<String, String> groupsIdMap = new HashMap<>();
        private long lastRefreshedTimestamp;

        public String getGroup(String groupId) {
            if ((System.currentTimeMillis() - lastRefreshedTimestamp) > CACHE_REFRESH_TIME_MS) {
                refresh();
            }
            String groupName = groupsIdMap.get(groupId);
            if (groupName == null) {
                refresh();
                return groupsIdMap.get(groupId);
            }
            return groupName;
        }

        private void refresh() {
            groupsIdMap.clear();
            List<String> group = ExecutingCommand.runNative("getent group");
            // see man 5 group for the fields
            for (String entry : group) {
                String[] split = entry.split(":");
                if (split.length < 3) {
                    continue;
                }
                String groupName = split[0];
                String gid = split[2];
                if (!groupsIdMap.containsKey(gid)) {
                    groupsIdMap.put(gid, groupName);
                }
            }
            lastRefreshedTimestamp = System.currentTimeMillis();
        }
    }
}
