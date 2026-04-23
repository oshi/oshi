/*
 * Copyright 2017-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import oshi.annotation.PublicApi;
import oshi.annotation.concurrent.ThreadSafe;

/**
 * Provides network parameters of the running operating system, including the hostname, domain name, DNS server
 * addresses, and default gateways for IPv4 and IPv6.
 * <p>
 * The hostname ({@link #getHostName()}) is the local machine name. The domain name ({@link #getDomainName()}) is the
 * DNS domain suffix. DNS servers ({@link #getDnsServers()}) are the configured name resolution servers. The default
 * gateways ({@link #getIpv4DefaultGateway()}, {@link #getIpv6DefaultGateway()}) are the routing destinations for
 * {@code 0.0.0.0/0} and {@code ::/0} respectively, and return an empty string if not defined.
 */
@PublicApi
@ThreadSafe
public interface NetworkParams {

    /**
     * Gets the HostName of the machine executing OSHI.
     *
     * @return the hostname
     */
    String getHostName();

    /**
     * Gets the Domain Name of the machine executing OSHI.
     *
     * @return the domain name
     */
    String getDomainName();

    /**
     * Gets the DNS Servers configured for this machine.
     *
     * @return the DNS servers
     */
    String[] getDnsServers();

    /**
     * Gets the default gateway(routing destination for 0.0.0.0/0) for IPv4 connections.
     *
     * @return default gateway for IPv4, or empty string if not defined.
     */
    String getIpv4DefaultGateway();

    /**
     * Gets default gateway(routing destination for ::/0) for IPv6 connections.
     *
     * @return default gateway for IPv6, or empty string if not defined.
     */
    String getIpv6DefaultGateway();
}
