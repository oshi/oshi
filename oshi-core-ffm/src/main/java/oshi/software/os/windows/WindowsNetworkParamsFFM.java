/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import oshi.ffm.util.platform.windows.IPHlpAPIUtilFFM;
import oshi.ffm.util.platform.windows.Kernel32UtilFFM;
import oshi.software.common.os.windows.WindowsNetworkParams;

public final class WindowsNetworkParamsFFM extends WindowsNetworkParams {

    @Override
    public String[] getDnsServers() {
        return IPHlpAPIUtilFFM.getDnsServers();
    }

    @Override
    public String getDomainName() {
        return Kernel32UtilFFM.getComputerNameEx();
    }

    @Override
    public String getHostName() {
        String name = Kernel32UtilFFM.getComputerName();
        return name.isEmpty() ? super.getHostName() : name;
    }
}
