/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
class LinuxNetworkParamsTest {

    /** Concrete subclass; every method under test is inherited. */
    private static final class StubLinuxNetworkParams extends LinuxNetworkParams {
    }

    /**
     * The host name must be the kernel's, read here independently of {@code FileUtil} and {@code ProcPath}. A name
     * resolution based implementation fails this whenever the kernel host name is a FQDN, because it truncates at the
     * first dot, or whenever the host name does not resolve, because it reports {@code localhost}.
     */
    @Test
    void testHostNameIsTheKernelHostName() throws IOException {
        byte[] raw = Files.readAllBytes(Paths.get("/proc/sys/kernel/hostname"));
        String kernelHostName = new String(raw, StandardCharsets.UTF_8).trim();
        assertThat("The kernel always has a host name", kernelHostName, is(not(emptyString())));
        assertThat(new StubLinuxNetworkParams().getHostName(), is(kernelHostName));
    }
}
