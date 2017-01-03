package oshi.software.os.unix.freebsd;

import oshi.software.os.NetworkParams;

public class FreeBsdNetworkParams implements NetworkParams{
    /**
     * {@inheritDoc}
     */
    @Override
    public String getHostName() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDomainName() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDnsServers() {
        return new String[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv4DefaultGateway() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv6DefaultGateway() {
        return "";
    }
}
