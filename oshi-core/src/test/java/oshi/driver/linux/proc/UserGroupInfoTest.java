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

import org.junit.jupiter.api.Test;
import oshi.util.ExecutingCommand;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserGroupInfoTest {

    @Test
    public void testGetUser() {
        List<String> checkedUid = new ArrayList<>();
        List<String> passwd = ExecutingCommand.runNative("getent passwd");
        passwd.stream().map(s -> s.split(":")).filter(arr -> arr.length > 2).forEach(split -> {
            String uid = split[2];
            String userName = split[0];
            if (!checkedUid.contains(uid)) {
                assertEquals(UserGroupInfo.getUser(uid), userName, "Incorrect result for USER_ID_MAP");
                checkedUid.add(uid);
            }
        });
    }

    @Test
    public void testGetGroupName() {
        List<String> checkedGid = new ArrayList<>();
        List<String> group = ExecutingCommand.runNative("getent group");
        group.stream().map(s -> s.split(":")).filter(arr -> arr.length > 2).forEach(split -> {
            String gid = split[2];
            String groupName = split[0];
            if (!checkedGid.contains(gid)) {
                assertEquals(UserGroupInfo.getGroupName(gid), groupName, "Incorrect result for GROUPS_ID_MAP");
                checkedGid.add(gid);
            }
        });
    }
}
