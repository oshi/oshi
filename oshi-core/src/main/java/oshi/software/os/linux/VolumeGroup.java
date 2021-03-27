/**
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
package oshi.software.os.linux;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class VolumeGroup {
    private final String name;
    private final Map<String, List<String>> lvMap;
    private final Set<String> pvSet;

    /**
     * @param name Name of the volume group
     * @param lvMap Logical volumes derived from this volume group and the physical volumes its mapped to.
     * @param pvSet Set of physical volumes this volume group consists of.
     */
    public VolumeGroup(String name, Map<String, List<String>> lvMap, Set<String> pvSet) {
        this.name = name;
        this.lvMap = lvMap;
        this.pvSet = pvSet;
    }

    public String getName() {
        return name;
    }

    public Map<String, List<String>> getLogicalVolumes() {
        return lvMap;
    }

    public Set<String> getPhysicalVolumes() {
        return pvSet;
    }
}
