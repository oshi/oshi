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
package oshi.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads from lsof into a map
 *
 * @author widdis[at]gmail[dot]com
 */
public class LsofUtil {

    private LsofUtil() {
    }

    public static Map<Integer, String> getCwdMap(int pid) {
        List<String> lsof = ExecutingCommand.runNative("lsof -Fn -d cwd" + (pid < 0 ? "" : " -p " + pid));
        Map<Integer, String> cwdMap = new HashMap<>();
        Integer key = -1;
        for (String line : lsof) {
            if (line.isEmpty()) {
                continue;
            }
            switch (line.charAt(0)) {
            case 'p':
                key = ParseUtil.parseIntOrDefault(line.substring(1), -1);
                break;
            case 'n':
                cwdMap.put(key, line.substring(1));
                break;
            case 'f':
                // ignore the 'cwd' file descriptor
            default:
                break;
            }
        }
        return cwdMap;
    }
}
