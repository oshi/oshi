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

import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.win32.W32APITypeMapper;

import oshi.jna.platform.windows.WinNT.LARGE_INTEGER;

public interface Wtsapi32 extends com.sun.jna.platform.win32.Wtsapi32 {

    Wtsapi32 INSTANCE = Native.load("Wtsapi32", Wtsapi32.class, W32APIOptions.DEFAULT_OPTIONS);
    /**
     * Defined in {@code winsta.h} and present in this interface to properly size
     * the {@link WTSINFO} structure.
     */
    int DOMAIN_LENGTH = 17;

    /**
     * Defined in {@code winsta.h} and present in this interface to properly size
     * the {@link WTSINFO} structure.
     */
    int USERNAME_LENGTH = 20;

    /**
     * Defined in {@code winsta.h} and present in this interface to properly size
     * the {@link WTSINFO} structure.
     */
    int WINSTATIONNAME_LENGTH = 32;

    /**
     * Specifies the connection state of a Remote Desktop Services session.
     */
    public interface WTS_CONNECTSTATE_CLASS {
        int WTSActive = 0;
        int WTSConnected = 1;
        int WTSConnectQuery = 2;
        int WTSShadow = 3;
        int WTSDisconnected = 4;
        int WTSIdle = 5;
        int WTSListen = 6;
        int WTSReset = 7;
        int WTSDown = 8;
        int WTSInit = 9;
    }

    /**
     * Contains values that indicate the type of session information to retrieve in
     * a call to the {@code WTSQuerySessionInformation()} function.
     */
    public interface WTS_INFO_CLASS {
        int WTSInitialProgram = 0;
        int WTSApplicationName = 1;
        int WTSWorkingDirectory = 2;
        int WTSOEMId = 3;
        int WTSSessionId = 4;
        int WTSUserName = 5;
        int WTSWinStationName = 6;
        int WTSDomainName = 7;
        int WTSConnectState = 8;
        int WTSClientBuildNumber = 9;
        int WTSClientName = 10;
        int WTSClientDirectory = 11;
        int WTSClientProductId = 12;
        int WTSClientHardwareId = 13;
        int WTSClientAddress = 14;
        int WTSClientDisplay = 15;
        int WTSClientProtocolType = 16;
        int WTSIdleTime = 17;
        int WTSLogonTime = 18;
        int WTSIncomingBytes = 19;
        int WTSOutgoingBytes = 20;
        int WTSIncomingFrames = 21;
        int WTSOutgoingFrames = 22;
        int WTSClientInfo = 23;
        int WTSSessionInfo = 24;
        int WTSSessionInfoEx = 25;
        int WTSConfigInfo = 26;
        int WTSValidationInfo = 27;
        int WTSSessionAddressV4 = 28;
        int WTSIsRemoteSession = 29;
    }

    /**
     * Contains information about a client session on a Remote Desktop Session Host
     * (RD Session Host) server.
     */
    @FieldOrder({ "SessionId", "pWinStationName", "State" })
    class WTS_SESSION_INFO extends Structure {
        public int SessionId;
        public String pWinStationName; // either LPSTR or LPWSTR
        public int State; // WTS_CONNECTSTATE_CLASS

        public WTS_SESSION_INFO() {
            super(W32APITypeMapper.DEFAULT);
        }

        public WTS_SESSION_INFO(Pointer p) {
            super(p, Structure.ALIGN_DEFAULT, W32APITypeMapper.DEFAULT);
            read();
        }
    }

    /**
     * Contains the client network address of a Remote Desktop Services session.
     */
    @FieldOrder({ "AddressFamily", "Address" })
    class WTS_CLIENT_ADDRESS extends Structure {
        public int AddressFamily;
        public byte[] Address = new byte[20];

        public WTS_CLIENT_ADDRESS() {
            super();
        }

        public WTS_CLIENT_ADDRESS(Pointer p) {
            super(p);
            read();
        }
    }

    /**
     * Contains information about a Remote Desktop Services session.
     */
    @FieldOrder({ "State", "SessionId", "IncomingBytes", "OutgoingBytes", "IncomingFrames", "OutgoingFrames",
            "IncomingCompressedBytes", "OutgoingCompressedBytes", "WinStationName", "Domain", "UserName", "ConnectTime",
            "DisconnectTime", "LastInputTime", "LogonTime", "CurrentTime" })
    class WTSINFO extends Structure {
        private static final int CHAR_WIDTH = Boolean.getBoolean("w32.ascii") ? 1 : 2;

        public int State; // WTS_CONNECTSTATE_CLASS
        public int SessionId;
        public int IncomingBytes;
        public int OutgoingBytes;
        public int IncomingFrames;
        public int OutgoingFrames;
        public int IncomingCompressedBytes;
        public int OutgoingCompressedBytes;
        public final byte[] WinStationName = new byte[WINSTATIONNAME_LENGTH * CHAR_WIDTH];
        public final byte[] Domain = new byte[DOMAIN_LENGTH * CHAR_WIDTH];
        public final byte[] UserName = new byte[(USERNAME_LENGTH + 1) * CHAR_WIDTH];
        public LARGE_INTEGER ConnectTime;
        public LARGE_INTEGER DisconnectTime;
        public LARGE_INTEGER LastInputTime;
        public LARGE_INTEGER LogonTime;
        public LARGE_INTEGER CurrentTime;

        public WTSINFO() {
            super();
        }

        public WTSINFO(Pointer p) {
            super(p);
            read();
        }

        /**
         * Convenience method to return the null-terminated string in the
         * {@link #WinStationName} member, accounting for {@code CHAR} or {@code WCHAR}
         * byte width.
         *
         * @return The {@code WinStationName} as a string.
         */
        public String getWinStationName() {
            return getStringAtOffset(fieldOffset("WinStationName"));
        }

        /**
         * Convenience method to return the null-terminated string in the
         * {@link #Domain} member, accounting for {@code CHAR} or {@code WCHAR} byte
         * width.
         *
         * @return The {@code Domain} as a string.
         */
        public String getDomain() {
            return getStringAtOffset(fieldOffset("Domain"));
        }

        /**
         * Convenience method to return the null-terminated string in the
         * {@link #UserName} member, accounting for {@code CHAR} or {@code WCHAR} byte
         * width.
         *
         * @return The {@code UserName} as a string.
         */
        public String getUserName() {
            return getStringAtOffset(fieldOffset("UserName"));
        }

        private String getStringAtOffset(int offset) {
            return CHAR_WIDTH == 1 ? getPointer().getString(offset) : getPointer().getWideString(offset);
        }
    }

    /**
     * Retrieves a list of sessions on a Remote Desktop Session Host (RD Session
     * Host) server.
     *
     * @param hServer
     *            A handle to the RD Session Host server.
     *            <p>
     *            You can use the {@code WTSOpenServer} or {@code WTSOpenServerEx}
     *            functions to retrieve a handle to a specific server, or
     *            {@link #WTS_CURRENT_SERVER_HANDLE} to use the RD Session Host
     *            server that hosts your application.
     * @param Reserved
     *            This parameter is reserved. It must be zero.
     * @param Version
     *            The version of the enumeration request. This parameter must be 1.
     * @param ppSessionInfo
     *            A pointer to an array of {@link WTS_SESSION_INFO} structures that
     *            represent the retrieved sessions. To free the returned buffer,
     *            call the {@link Wtsapi32#WTSFreeMemory} function.
     * @param pCount
     *            A pointer to the number of {@code WTS_SESSION_INFO} structures
     *            returned in the {@code ppSessionInfo} parameter.
     * @return Returns {@code false} if this function fails. If this function
     *         succeeds, returns {@code true}.
     *         <p>
     *         To get extended error information, call
     *         {@link Kernel32#GetLastError()}.
     */
    boolean WTSEnumerateSessions(HANDLE hServer, int Reserved, int Version, PointerByReference ppSessionInfo,
            IntByReference pCount);

    /**
     * Retrieves session information for the specified session on the specified
     * Remote Desktop Session Host (RD Session Host) server. It can be used to query
     * session information on local and remote RD Session Host servers.
     *
     * @param hServer
     *            A handle to an RD Session Host server. Specify a handle opened by
     *            the {@code WTSOpenServer} function, or specify
     *            {@link #WTS_CURRENT_SERVER_HANDLE} to indicate the RD Session Host
     *            server on which your application is running.
     * @param SessionId
     *            A Remote Desktop Services session identifier. To indicate the
     *            session in which the calling application is running (or the
     *            current session) specify {@link #WTS_CURRENT_SESSION}. Only
     *            specify {@code WTS_CURRENT_SESSION} when obtaining session
     *            information on the local server. If {@code WTS_CURRENT_SESSION} is
     *            specified when querying session information on a remote server,
     *            the returned session information will be inconsistent. Do not use
     *            the returned data.
     *            <p>
     *            You can use the {@code WTSEnumerateSessionsEx} function to
     *            retrieve the identifiers of all sessions on a specified RD Session
     *            Host server.
     *            <p>
     *            To query information for another user's session, you must have
     *            Query Information permission.
     * @param WTSInfoClass
     *            A value of the {@link WTS_INFO_CLASS} enumeration that indicates
     *            the type of session information to retrieve in a call to the
     *            {@code WTSQuerySessionInformation} function.
     * @param ppBuffer
     *            A pointer to a variable that receives a pointer to the requested
     *            information. The format and contents of the data depend on the
     *            information class specified in the {@code WTSInfoClass} parameter.
     *            To free the returned buffer, call the {@link #WTSFreeMemory}
     *            function.
     * @param pBytesReturned
     *            A pointer to a variable that receives the size, in bytes, of the
     *            data returned in ppBuffer.
     * @return If the function succeeds, returns {@code true}.
     *         <p>
     *         If the function fails, returns {@code false}. To get extended error
     *         information, call {@link Kernel32#GetLastError()}.
     */
    boolean WTSQuerySessionInformation(HANDLE hServer, int SessionId, int WTSInfoClass, PointerByReference ppBuffer,
            IntByReference pBytesReturned);

    /**
     * Frees memory allocated by a Remote Desktop Services function.
     *
     * @param pMemory
     *            Pointer to the memory to free.
     */
    void WTSFreeMemory(Pointer pMemory);
}
