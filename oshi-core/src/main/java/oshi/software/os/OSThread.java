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
package oshi.software.os;

import oshi.driver.windows.wmi.Win32ProcessCached;

public interface OSThread {

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     *
     * @return Returns the name of the thread.
     */
    String getName();

    /**
     * <p>
     * Getter for the field <code>path</code>.
     * </p>
     *
     * @return Returns the full path of the executing thread.
     */
    String getExePath();

    /**
     * <p>
     * Getter for the field <code>commandLine</code>.
     * </p>
     *
     * @return Returns the process command line.
     */
    String getCommandLine();

    /**
     * <p>
     * Getter for the field <code>currentWorkingDirectory</code>.
     * </p>
     *
     * @return Returns the thread current working directory.
     *
     */
    String getCurrentWorkingDirectory();

    /**
     * <p>
     * Getter for the field <code>user</code>.
     * </p>
     *
     * @return Returns the user name. On Windows systems, also returns the domain
     *         prepended to the username.
     */
    String getUser();

    /**
     * <p>
     * Getter for the field <code>userID</code>.
     * </p>
     *
     * @return Returns the userID. On Windows systems, returns the Security ID (SID)
     */
    String getUserID();

    /**
     * <p>
     * Getter for the field <code>group</code>.
     * </p>
     *
     * @return Returns the group.
     *
     */
    String getGroup();

    /**
     * <p>
     * Getter for the field <code>groupID</code>.
     * </p>
     *
     * @return Returns the groupID.
     */
    String getGroupID();
}
