/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisabledOnOs(OS.WINDOWS)
class UserGroupInfoTest {

    @Test
    void testGetUser() {
        List<String> checkedUid = new ArrayList<>();
        List<String> passwd = ExecutingCommand.runNative("getent passwd");
        passwd.stream().map(s -> s.split(":")).filter(arr -> arr.length > 2).forEach(split -> {
            String uid = split[2];
            String userName = split[0];
            if (!checkedUid.contains(uid)) {
                assertThat("Incorrect result for USER_ID_MAP", UserGroupInfo.getUser(uid), is(userName));
                checkedUid.add(uid);
            }
        });
    }

    @Test
    void testGetGroupName() {
        List<String> checkedGid = new ArrayList<>();
        List<String> group = ExecutingCommand.runNative("getent group");
        group.stream().map(s -> s.split(":")).filter(arr -> arr.length > 2).forEach(split -> {
            String gid = split[2];
            String groupName = split[0];
            if (!checkedGid.contains(gid)) {
                assertThat("Incorrect result for GROUPS_ID_MAP", UserGroupInfo.getGroupName(gid), is(groupName));
                checkedGid.add(gid);
            }
        });
    }
}
