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
package oshi.jna.platform.windows;

import com.sun.jna.platform.win32.Advapi32; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.W32Errors;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.SID_NAME_USE;
import com.sun.jna.ptr.IntByReference;

import oshi.jna.platform.windows.WinNT.TOKEN_PRIMARY_GROUP;

public class Advapi32Util extends com.sun.jna.platform.win32.Advapi32Util {
    /**
     * This function returns the primary group associated with a security token,
     * such as a user token.
     *
     * @param hToken
     *            Token.
     * @return Token primary group.
     */
    public static Account getTokenPrimaryGroup(HANDLE hToken) {
        // get token group information size
        IntByReference tokenInformationLength = new IntByReference();
        if (Advapi32.INSTANCE.GetTokenInformation(hToken, WinNT.TOKEN_INFORMATION_CLASS.TokenPrimaryGroup, null, 0,
                tokenInformationLength)) {
            throw new RuntimeException("Expected GetTokenInformation to fail with ERROR_INSUFFICIENT_BUFFER");
        }
        int rc = Kernel32.INSTANCE.GetLastError();
        if (rc != W32Errors.ERROR_INSUFFICIENT_BUFFER) {
            throw new Win32Exception(rc);
        }
        // get token group information
        TOKEN_PRIMARY_GROUP primaryGroup = new WinNT.TOKEN_PRIMARY_GROUP(tokenInformationLength.getValue());
        if (!Advapi32.INSTANCE.GetTokenInformation(hToken, WinNT.TOKEN_INFORMATION_CLASS.TokenPrimaryGroup,
                primaryGroup, tokenInformationLength.getValue(), tokenInformationLength)) {
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
        }
        Account group;
        try {
            group = Advapi32Util.getAccountBySid(primaryGroup.PrimaryGroup);
        } catch (Exception e) {
            group = new Account();
            group.sid = primaryGroup.PrimaryGroup.getBytes();
            group.sidString = Advapi32Util.convertSidToStringSid(primaryGroup.PrimaryGroup);
            group.name = group.sidString;
            group.fqn = group.sidString;
            group.accountType = SID_NAME_USE.SidTypeGroup;
        }
        return group;
    }
}
