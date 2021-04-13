/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.driver.linux.proc;

import static oshi.util.Memoizer.memoize;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;

/**
 * Utility class to temporarily cache the userID and group maps in Linux, for
 * parsing process ownership. Cache expires after one minute.
 */
@ThreadSafe
public final class UserGroupInfo {

    // Temporarily cache users and groups, update each minute
    private static final Supplier<Map<String, String>> usersIdMap = memoize(UserGroupInfo::getUserMap,
            TimeUnit.MINUTES.toNanos(1));
    private static final Supplier<Map<String, String>> groupsIdMap = memoize(UserGroupInfo::getGroupMap,
            TimeUnit.MINUTES.toNanos(1));

    private UserGroupInfo() {
    }

    /**
     * Gets a user from their ID
     *
     * @param userId
     *            a user ID
     * @return a pair containing that user id as the first element and the user name
     *         as the second
     */
    public static String getUser(String userId) {
        return usersIdMap.get().getOrDefault(userId, Constants.UNKNOWN);
    }

    /**
     * Gets the group name for a given ID
     *
     * @param groupId
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getGroupName(String groupId) {
        return groupsIdMap.get().getOrDefault(groupId, Constants.UNKNOWN);
    }

    private static Map<String, String> getUserMap() {
        HashMap<String, String> userMap = new HashMap<>();
        List<String> passwd = ExecutingCommand.runNative("getent passwd");
        // see man 5 passwd for the fields
        for (String entry : passwd) {
            String[] split = entry.split(":");
            if (split.length > 2) {
                String userName = split[0];
                String uid = split[2];
                // it is allowed to have multiple entries for the same userId,
                // we use the first one
                userMap.putIfAbsent(uid, userName);
            }
        }
        return userMap;
    }

    private static Map<String, String> getGroupMap() {
        Map<String, String> groupMap = new HashMap<>();
        List<String> group = ExecutingCommand.runNative("getent group");
        // see man 5 group for the fields
        for (String entry : group) {
            String[] split = entry.split(":");
            if (split.length > 2) {
                String groupName = split[0];
                String gid = split[2];
                groupMap.putIfAbsent(gid, groupName);
            }
        }
        return groupMap;
    }
}
