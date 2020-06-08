/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.software.common.AbstractOSThread;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;

public class LinuxOSThread extends AbstractOSThread {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxOSThread.class);

    private int processID;
    private int threadID;
    private String name;
    private String exePath;
    private String commandLine;
    private String userID;
    private String user;
    private String groupID;
    private String group;


    public LinuxOSThread(int pID, int tID) {
        this.processID = pID;
        this.threadID = tID;
        updateAttributes();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getExePath() {
        return this.exePath;
    }

    @Override
    public String getCommandLine() {
        return this.commandLine;
    }

    @Override
    public String getCurrentWorkingDirectory() {
        try {
            String cwdLink = String.format(ProcPath.THREAD_CWD, this.processID, this.threadID);
            String cwd = new File(cwdLink).getCanonicalPath();
            if (!cwd.equals(cwdLink)) {
                return cwd;
            }
        } catch (IOException e) {
            LOG.trace("Couldn't find cwd for thread id {}: {}", this.threadID, e.getMessage());
        }
        return "";
    }

    @Override
    public String getUser() {
        return this.user;
    }

    @Override
    public String getUserID() {
        return this.userID;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public String getGroupID() {
        return this.groupID;
    }

    private boolean updateAttributes() {
        Map<String, String> status = FileUtil.getKeyValueMapFromFile(String.format(ProcPath.THREAD_STATUS, this.processID, this.threadID),
                ":");
        this.name = status.getOrDefault("Name", "");
        this.exePath = getPath(String.format(ProcPath.THREAD_EXE_PATH, this.processID, this.threadID));
        this.commandLine = FileUtil.getStringFromFile(String.format(ProcPath.THREAD_CMDLINE, this.processID, this.threadID));
        this.userID = ParseUtil.whitespaces.split(status.getOrDefault("Uid", ""))[0];
        this.user = UserGroupInfo.getUser(userID);
        this.groupID = ParseUtil.whitespaces.split(status.getOrDefault("Gid", ""))[0];
        this.group = UserGroupInfo.getGroupName(groupID);

        return true;
    }

    private String getPath(String exePath) {
        String path = "";
        try {
            Path link = Paths.get(exePath);
            path = Files.readSymbolicLink(link).toString();
            // For some services the symbolic link process has terminated
            int index = path.indexOf(" (deleted)");
            if (index != -1) {
                path = path.substring(0, index);
            }
        } catch (InvalidPathException | IOException | UnsupportedOperationException | SecurityException e) {
            LOG.debug("Unable to open symbolic link {}", exePath);
        }
        return path;
    }
}
