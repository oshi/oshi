package oshi.software.os.linux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.util.ExecutingCommand;

public class LinuxUserInfo {
	
	protected Map<String, String> getUsers() {
		Map<String, String> users = new HashMap<>();
		List<String> passwd = ExecutingCommand.runNative("getent passwd");
		// see man 5 passwd for the fields
		for (String entry : passwd) {
			String[] split = entry.split(":");
			// it is allowed to have multiple entries for the same userId, we use the first one
			if (!users.containsKey(split[2])){
				users.put(split[2], split[0]); 
			}
		}
		return users;
	}

	protected Map<String, String> getGroups() {
		Map<String, String> groups = new HashMap<>();
		List<String> group = ExecutingCommand.runNative("getent group");
		// see man 5 group for the fields
		for (String entry : group) {
			String[] split = entry.split(":");
			if (!groups.containsKey(split[2])) {
				groups.put(split[2], split[0]);
			}
		}
		return groups;
	}
}
