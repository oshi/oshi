/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import oshi.ffm.SystemInfo;

@EnabledForJreRange(min = JRE.JAVA_25)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class VirtualMemoryFFMTest extends VirtualMemoryTest {

    @Override
    protected VirtualMemory createVirtualMemory() {
        return new SystemInfo().getHardware().getMemory().getVirtualMemory();
    }
}
