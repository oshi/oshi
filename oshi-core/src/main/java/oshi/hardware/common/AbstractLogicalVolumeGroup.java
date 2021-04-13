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
package oshi.hardware.common;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import oshi.hardware.LogicalVolumeGroup;

public class AbstractLogicalVolumeGroup implements LogicalVolumeGroup {

    private final String name;
    private final Map<String, Set<String>> lvMap;
    private final Set<String> pvSet;

    /**
     * @param name
     *            Name of the volume group
     * @param lvMap
     *            Logical volumes derived from this volume group and the physical
     *            volumes its mapped to.
     * @param pvSet
     *            Set of physical volumes this volume group consists of.
     */
    protected AbstractLogicalVolumeGroup(String name, Map<String, Set<String>> lvMap, Set<String> pvSet) {
        this.name = name;
        for (Entry<String, Set<String>> entry : lvMap.entrySet()) {
            lvMap.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
        }
        this.lvMap = Collections.unmodifiableMap(lvMap);
        this.pvSet = Collections.unmodifiableSet(pvSet);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Set<String>> getLogicalVolumes() {
        return lvMap;
    }

    @Override
    public Set<String> getPhysicalVolumes() {
        return pvSet;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Logical Volume Group: ");
        sb.append(name).append("\n |-- PVs: ");
        sb.append(pvSet.toString());
        for (Entry<String, Set<String>> entry : lvMap.entrySet()) {
            sb.append("\n |-- LV: ").append(entry.getKey());
            Set<String> mappedPVs = entry.getValue();
            if (!mappedPVs.isEmpty()) {
                sb.append(" --> ").append(mappedPVs);
            }
        }
        return sb.toString();
    }
}
