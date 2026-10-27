/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.dragonflybsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import oshi.software.os.OperatingSystem;

/**
 * Tests that {@link DragonFlyBsdOSProcess} asks the kernel for an executable path only for the current process.
 */
class DragonFlyBsdOSProcessTest {

    private static final int OWN_PID = 42;

    // Only getProcessId() is reached by the code under test
    private static final OperatingSystem OS = (OperatingSystem) Proxy.newProxyInstance(
            OperatingSystem.class.getClassLoader(), new Class<?>[] { OperatingSystem.class }, (proxy, method, args) -> {
                if ("getProcessId".equals(method.getName())) {
                    return OWN_PID;
                }
                throw new UnsupportedOperationException(method.getName());
            });

    private static DragonFlyBsdOSProcess stubProcess(int pid, AtomicInteger kernelQueries) {
        return new DragonFlyBsdOSProcess(pid, OS) {
            @Override
            protected String queryOwnExecutablePath() {
                kernelQueries.incrementAndGet();
                return "/usr/local/bin/java";
            }

            @Override
            protected long queryAffinityMask() {
                return 0L;
            }

            @Override
            protected List<String> queryArguments() {
                return Collections.emptyList();
            }

            @Override
            protected Map<String, String> queryEnvironmentVariables() {
                return Collections.emptyMap();
            }
        };
    }

    @Test
    void testCurrentProcessPathFromKernel() {
        AtomicInteger kernelQueries = new AtomicInteger();
        assertThat(stubProcess(OWN_PID, kernelQueries).queryExecutablePath(), is("/usr/local/bin/java"));
        assertThat(kernelQueries.get(), is(1));
    }

    @Test
    void testOtherProcessNeverQueriesKernel() {
        AtomicInteger kernelQueries = new AtomicInteger();
        assertThat(stubProcess(OWN_PID + 1, kernelQueries).queryExecutablePath(), is(""));
        assertThat(kernelQueries.get(), is(0));
    }
}
