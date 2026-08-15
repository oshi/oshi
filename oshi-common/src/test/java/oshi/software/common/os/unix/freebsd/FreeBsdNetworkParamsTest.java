/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link FreeBsdNetworkParams} honors the non-null contract {@code NetworkParams.getDomainName()} declares,
 * whatever the backend resolver reports.
 */
class FreeBsdNetworkParamsTest {

    /** A params instance whose resolver result is supplied by the test. */
    private static final class StubParams extends FreeBsdNetworkParams {
        private final @Nullable String domainName;

        StubParams(@Nullable String domainName) {
            this.domainName = domainName;
        }

        @Override
        protected @Nullable String queryDomainName() {
            return this.domainName;
        }

        @Override
        protected @Nullable String queryHostName() {
            return "stub-host";
        }
    }

    @Test
    void testDomainNameIsEmptyWhenTheResolverReportsNothing() {
        assertThat(new StubParams(null).getDomainName(), is(""));
    }

    @Test
    void testDomainNameIsEmptyWhenTheResolverReportsAnEmptyString() {
        assertThat(new StubParams("").getDomainName(), is(""));
    }

    @Test
    void testDomainNameIsPassedThroughWhenResolved() {
        assertThat(new StubParams("example.com").getDomainName(), is("example.com"));
    }

    @Test
    void testHostNameComesFromTheResolverWhenPresent() {
        assertThat(new StubParams(null).getHostName(), is("stub-host"));
    }
}
