/*
 * MIT License
 *
 * Copyright (c) 2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
