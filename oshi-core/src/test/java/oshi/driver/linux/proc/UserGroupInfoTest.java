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
