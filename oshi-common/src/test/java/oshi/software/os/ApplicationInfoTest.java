/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ApplicationInfoTest {

    @Test
    void testGetters() {
        Map<String, String> extra = new LinkedHashMap<>();
        extra.put("location", "/Applications/App.app");
        ApplicationInfo app = new ApplicationInfo("MyApp", "2.0", "Acme", 1_000_000L, extra);
        assertThat(app.getName(), is("MyApp"));
        assertThat(app.getVersion(), is("2.0"));
        assertThat(app.getVendor(), is("Acme"));
        assertThat(app.getTimestamp(), is(1_000_000L));
        assertThat(app.getAdditionalInfo().get("location"), is("/Applications/App.app"));
    }

    @Test
    void testNullAdditionalInfoBecomesEmptyMap() {
        ApplicationInfo app = new ApplicationInfo("App", "1.0", "Vendor", 0L, null);
        assertThat(app.getAdditionalInfo().isEmpty(), is(true));
    }

    @Test
    void testEqualsAndHashCode() {
        ApplicationInfo a = new ApplicationInfo("App", "1.0", "Vendor", 0L, null);
        ApplicationInfo b = new ApplicationInfo("App", "1.0", "Vendor", 0L, null);
        ApplicationInfo c = new ApplicationInfo("Other", "1.0", "Vendor", 0L, null);
        assertThat(a, is(b));
        assertThat(a.hashCode(), is(b.hashCode()));
        assertThat(a, is(not(c)));
        assertThat(a, is(a));
        assertThat(a, is(not((Object) null)));
    }

    @Test
    void testToString() {
        ApplicationInfo app = new ApplicationInfo("MyApp", "2.0", "Acme", 0L, null);
        assertThat(app.toString(), containsString("MyApp"));
        assertThat(app.toString(), containsString("Acme"));
    }
}
