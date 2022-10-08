/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import java.util.Map;
import java.util.Set;

import oshi.annotation.concurrent.Immutable;

/**
 * A logical volume group implemented as part of logical volume management, combining the space on one or more storage
 * devices such as disks or partitions (physical volumes) into a storage pool, and subsequently allocating that space to
 * virtual partitions (logical volumes) as block devices accessible to the file system.
 */
@Immutable
public interface LogicalVolumeGroup {
    /**
     * Gets the logical volume group name.
     *
     * @return The name of the logical volume group.
     */
    String getName();

    /**
     * Gets a set of all physical volumes in this volume group.
     *
     * @return A set with the names of the physical volumes.
     */
    Set<String> getPhysicalVolumes();

    /**
     * Gets a map containing information about the logical volumes in the logical volume group, represented to the file
     * system as block devices. The keyset for the map represents a collection of the logical volumes, while the values
     * associated with these keys represent the physical volumes mapped to each logical volume (if known).
     *
     * @return A map with the logical volume names as the key, and a set of associated physical volume names as the
     *         value.
     */
    Map<String, Set<String>> getLogicalVolumes();
}
