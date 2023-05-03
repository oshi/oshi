/*
 * Copyright 2022-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static oshi.util.Memoizer.memoize;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.sun.jna.Platform;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Utility class to temporarily cache the userID and group maps in *nix, for parsing process ownership. Cache expires
 * after one minute.
 */
@ThreadSafe
public final class UserGroupInfo {

    // Temporarily cache users and groups, update each minute
    private static final Supplier<Map<String, String>> USERS_ID_MAP = memoize(UserGroupInfo::getUserMap,
            TimeUnit.MINUTES.toNanos(1));
    private static final Supplier<Map<String, String>> GROUPS_ID_MAP = memoize(UserGroupInfo::getGroupMap,
            TimeUnit.MINUTES.toNanos(1));

    private static final boolean ELEVATED = 0 == ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("id -u"),
            -1);

    private UserGroupInfo() {
    }

    /**
     * Determine whether the current process has elevated permissions such as sudo / Administrator
     *
     * @return True if this process has elevated permissions
     */
    public static boolean isElevated() {
        return ELEVATED;
    }

    /**
     * Gets a user from their ID
     *
     * @param userId a user ID
     * @return a pair containing that user id as the first element and the user name as the second
     */
    public static String getUser(String userId) {
        return USERS_ID_MAP.get().getOrDefault(userId, Constants.UNKNOWN);
    }

    /**
     * Gets the group name for a given ID
     *
     * @param groupId a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getGroupName(String groupId) {
        return GROUPS_ID_MAP.get().getOrDefault(groupId, Constants.UNKNOWN);
    }

    private static Map<String, String> getUserMap() {
        HashMap<String, String> userMap = new HashMap<>();
        List<String> passwd;
        if (Platform.isAIX()) {
            passwd = FileUtil.readFile("/etc/passwd");
        } else {
            passwd = ExecutingCommand.runNative("getent passwd");
        }
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
        List<String> group;
        if (Platform.isAIX()) {
            group = FileUtil.readFile("/etc/group");
        } else {
            group = ExecutingCommand.runNative("getent group");
        }
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
