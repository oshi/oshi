/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.hardware.LogicalVolumeGroup;

class AixLogicalVolumeGroupTest {

    // Captured unprivileged on cfarm119 (AIX 7.3)
    private static final String LSVG_P = """
            vgHome:
            PV_NAME           PV STATE          TOTAL PPs   FREE PPs    FREE DISTRIBUTION
            hdisk2            active            1983        302         00..00..00..00..302
            hdisk3            active            1983        303         00..00..00..00..303
            hdisk4            active            1983        303         00..00..00..00..303
            hdisk5            active            1983        303         00..00..00..00..303
            hdisk6            active            1983        303         00..00..00..00..303
            hdisk7            active            1983        303         00..00..00..00..303
            hdisk8            active            1983        303         00..00..00..00..303
            hdisk9            active            1983        303         00..00..00..00..303
            vgScratch:
            PV_NAME           PV STATE          TOTAL PPs   FREE PPs    FREE DISTRIBUTION
            hdisk1            active            1022        0           00..00..00..00..00
            rootvg:
            PV_NAME           PV STATE          TOTAL PPs   FREE PPs    FREE DISTRIBUTION
            hdisk0            active            991         629         198..35..00..198..198
            """;
    private static final String LSVG_L = """
            vgHome:
            LV NAME             TYPE       LPs     PPs     PVs  LV STATE      MOUNT POINT
            hd01                jfs2       13440   13440   8    open/syncd    /home
            log08               jfs2log    1       1       1    open/syncd    N/A
            vgScratch:
            LV NAME             TYPE       LPs     PPs     PVs  LV STATE      MOUNT POINT
            lgscratch           jfs2log    1       1       1    closed/syncd  N/A
            lvscratch           jfs2       1021    1021    1    closed/syncd  /scratch
            rootvg:
            LV NAME             TYPE       LPs     PPs     PVs  LV STATE      MOUNT POINT
            hd5                 boot       1       1       1    closed/syncd  N/A
            hd6                 paging     32      32      1    open/syncd    N/A
            hd4                 jfs2       64      64      1    open/syncd    /
            livedump            jfs2       4       4       1    open/syncd    /var/adm/ras/livedump
            """;
    private static final String LSPV_HDISK2 = """
            hdisk2:
            LV NAME               LPs     PPs     DISTRIBUTION          MOUNT POINT
            hd01                  1680    1680    397..397..396..396..94 /home
            log08                 1       1       00..00..00..00..01    N/A
            """;

    private static String lspvOther(int disk) {
        return "hdisk" + disk + ":\n" + "LV NAME               LPs     PPs     DISTRIBUTION          MOUNT POINT\n"
                + "hd01                  1680    1680    397..397..396..396..94 /home\n";
    }

    @Test
    void testQueryLogicalVolumeGroups() {
        Map<String, String> outputs = new HashMap<>();
        outputs.put("lsvg -o", "vgHome\nvgScratch\nrootvg\n");
        outputs.put("lsvg -p vgHome vgScratch rootvg", LSVG_P);
        outputs.put("lsvg -l vgHome vgScratch rootvg", LSVG_L);
        outputs.put("lspv -l hdisk2", LSPV_HDISK2);
        for (int disk = 3; disk <= 9; disk++) {
            outputs.put("lspv -l hdisk" + disk, lspvOther(disk));
        }
        List<String> commands = new ArrayList<>();
        List<LogicalVolumeGroup> groups = AixLogicalVolumeGroup.queryLogicalVolumeGroups(cmd -> {
            commands.add(cmd);
            return outputs.getOrDefault(cmd, "").lines().toList();
        });
        assertThat(groups.stream().map(LogicalVolumeGroup::getName).toList(),
                contains("vgHome", "vgScratch", "rootvg"));

        LogicalVolumeGroup home = groups.get(0);
        assertThat(home.getPhysicalVolumes(),
                containsInAnyOrder("hdisk2", "hdisk3", "hdisk4", "hdisk5", "hdisk6", "hdisk7", "hdisk8", "hdisk9"));
        assertThat(home.getLogicalVolumes().get("hd01"),
                containsInAnyOrder("hdisk2", "hdisk3", "hdisk4", "hdisk5", "hdisk6", "hdisk7", "hdisk8", "hdisk9"));
        assertThat(home.getLogicalVolumes().get("log08"), contains("hdisk2"));

        // A volume group of one physical volume needs no lspv
        LogicalVolumeGroup scratch = groups.get(1);
        assertThat(scratch.getPhysicalVolumes(), contains("hdisk1"));
        assertThat(scratch.getLogicalVolumes().get("lvscratch"), contains("hdisk1"));
        assertThat(groups.get(2).getLogicalVolumes().keySet(), containsInAnyOrder("hd5", "hd6", "hd4", "livedump"));
        assertThat(commands.stream().filter(c -> c.startsWith("lspv")).count(), is(8L));
    }

    @Test
    void testQueryLogicalVolumeGroupsNone() {
        assertThat(AixLogicalVolumeGroup.queryLogicalVolumeGroups(cmd -> Collections.emptyList()), is(empty()));
    }
}
