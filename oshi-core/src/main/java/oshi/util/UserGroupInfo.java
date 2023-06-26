/*
 * Copyright 2022-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static oshi.util.Memoizer.memoize;

import com.sun.jna.Platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Utility class to temporarily cache the userID and group maps in *nix, for
 * parsing process ownership. Cache expires after one minute.
 */
@ThreadSafe
public final class UserGroupInfo {

    // Temporarily cache users and groups in concurrent maps, completely refresh
    // every 5 minutes
    private static final Supplier<Map<String, String>> USERS_ID_MAP = memoize(UserGroupInfo::getUserMap,
            TimeUnit.MINUTES.toNanos(5));
    private static final Supplier<Map<String, String>> GROUPS_ID_MAP = memoize(UserGroupInfo::getGroupMap,
            TimeUnit.MINUTES.toNanos(5));

    private static final boolean ELEVATED = 0 == ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("id -u"),
            -1);

    private UserGroupInfo() {
    }

    /**
     * Determine whether the current process has elevated permissions such as sudo /
     * Administrator
     *
     * @return True if this process has elevated permissions
     */
    public static boolean isElevated() {
        return ELEVATED;
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
        // If value is in cached /etc/passwd return, else do getent passwd uid
        return USERS_ID_MAP.get().getOrDefault(userId, getentPasswd(userId));
    }

    /**
     * Gets the group name for a given ID
     *
     * @param groupId
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getGroupName(String groupId) {
        // If value is in cached /etc/passwd return, else do getent group gid
        return GROUPS_ID_MAP.get().getOrDefault(groupId, getentGroup(groupId));
    }

    private static Map<String, String> getUserMap() {
        return parsePasswd(FileUtil.readFile("/etc/passwd"));
    }

    private static String getentPasswd(String userId) {
        if (Platform.isAIX()) {
            return Constants.UNKNOWN;
        }
        Map<String, String> newUsers = parsePasswd(ExecutingCommand.runNative("getent passwd " + userId));
        // add to user map for future queries
        USERS_ID_MAP.get().putAll(newUsers);
        return newUsers.getOrDefault(userId, Constants.UNKNOWN);
    }

    private static Map<String, String> parsePasswd(List<String> passwd) {
        Map<String, String> userMap = new ConcurrentHashMap<>();
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
        return parseGroup(FileUtil.readFile("/etc/group"));
    }

    private static String getentGroup(String groupId) {
        if (Platform.isAIX()) {
            return Constants.UNKNOWN;
        }
        Map<String, String> newGroups = parseGroup(ExecutingCommand.runNative("getent group " + groupId));
        // add to group map for future queries
        GROUPS_ID_MAP.get().putAll(newGroups);
        return newGroups.getOrDefault(groupId, Constants.UNKNOWN);
    }

    private static Map<String, String> parseGroup(List<String> group) {
        Map<String, String> groupMap = new ConcurrentHashMap<>();
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
