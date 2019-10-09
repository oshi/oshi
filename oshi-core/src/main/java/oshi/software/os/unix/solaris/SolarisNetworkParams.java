/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.os.unix.solaris;

import com.sun.jna.Native; // NOSONAR

import oshi.jna.platform.unix.solaris.SolarisLibc;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;

public class SolarisNetworkParams extends AbstractNetworkParams {

    @Override
    public String getHostName() {
        byte[] hostnameBuffer = new byte[SolarisLibc.HOST_NAME_MAX + 1];
        int result = SolarisLibc.INSTANCE.gethostname(hostnameBuffer, hostnameBuffer.length);
        if (result != 0) {
            return super.getHostName();
        }
        return Native.toString(hostnameBuffer);
    }

    @Override
    public String getIpv4DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route get -inet default"));
    }

    @Override
    public String getIpv6DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route get -inet6 default"));
    }
}
