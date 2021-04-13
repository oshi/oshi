/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware;

import java.util.Map;
import java.util.Set;

import oshi.annotation.concurrent.Immutable;

/**
 * A logical volume group implemented as part of logical volume management,
 * combining the space on one or more storage devices such as disks or
 * partitions (physical volumes) into a storage pool, and subsequently
 * allocating that space to virtual partitions (logical volumes) as block
 * devices accessible to the file system.
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
     * Gets a map containing information about the logical volumes in the logical
     * volume group, represented to the file system as block devices. The keyset for
     * the map represents a collection of the logical volumes, while the values
     * associated with these keys represent the physical volumes mapped to each
     * logical volume (if known).
     *
     * @return A map with the logical volume names as the key, and a set of
     *         associated physical volume names as the value.
     */
    Map<String, Set<String>> getLogicalVolumes();
}
