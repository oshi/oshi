/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.freebsd.disk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.util.Constants;
import oshi.util.tuples.Triplet;

class GeomDiskListTest {

    @Test
    void testParseGeomDiskListTypicalOutput() {
        List<String> geom = Arrays.asList("Geom name: ada0", "Providers:", "1. Name: ada0",
                "   Mediasize: 500107862016 (465G)", "   Sectorsize: 512", "   Mode: r2w2e3",
                "   descr: Samsung SSD 860 EVO 500GB", "   ident: S3Z2NB0K123456X", "   rotationrate: 0", "",
                "Geom name: da0", "Providers:", "1. Name: da0", "   Mediasize: 8053063680 (7.5G)", "   Sectorsize: 512",
                "   Mode: r0w0e0", "   descr: USB Flash Drive", "   ident: (null)", "   rotationrate: unknown");

        Map<String, Triplet<String, String, Long>> result = GeomDiskList.parseGeomDiskList(geom);

        assertThat(result.size(), is(2));

        Triplet<String, String, Long> ada0 = result.get("ada0");
        assertThat(ada0.getA(), is("Samsung SSD 860 EVO 500GB"));
        assertThat(ada0.getB(), is("S3Z2NB0K123456X"));
        assertThat(ada0.getC(), is(500107862016L));

        Triplet<String, String, Long> da0 = result.get("da0");
        assertThat(da0.getA(), is("USB Flash Drive"));
        assertThat(da0.getB(), is(""));
        assertThat(da0.getC(), is(8053063680L));
    }

    @Test
    void testParseGeomDiskListEmptyInput() {
        Map<String, Triplet<String, String, Long>> result = GeomDiskList.parseGeomDiskList(Collections.emptyList());
        assertThat(result, is(anEmptyMap()));
    }

    @Test
    void testParseGeomDiskListNoMediasizeDefaultsToZero() {
        List<String> geom = Arrays.asList("Geom name: vtbd0", "   descr: VirtIO Block Device", "   ident: serial123");

        Map<String, Triplet<String, String, Long>> result = GeomDiskList.parseGeomDiskList(geom);

        assertThat(result.size(), is(1));
        Triplet<String, String, Long> vtbd0 = result.get("vtbd0");
        assertThat(vtbd0.getA(), is("VirtIO Block Device"));
        assertThat(vtbd0.getB(), is("serial123"));
        assertThat(vtbd0.getC(), is(0L));
    }

    @Test
    void testParseGeomDiskListMissingFieldsDefaultToUnknown() {
        List<String> geom = Arrays.asList("Geom name: nvd0", "   Mediasize: 1024000000000 (953G)");

        Map<String, Triplet<String, String, Long>> result = GeomDiskList.parseGeomDiskList(geom);

        assertThat(result.size(), is(1));
        Triplet<String, String, Long> nvd0 = result.get("nvd0");
        assertThat(nvd0.getA(), is(Constants.UNKNOWN));
        assertThat(nvd0.getB(), is(Constants.UNKNOWN));
        assertThat(nvd0.getC(), is(1024000000000L));
    }
}
