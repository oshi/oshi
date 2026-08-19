/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

import org.junit.jupiter.api.Test;

class WindowsCentralProcessorTest {

    @Test
    void testParseIdentifier() {
        String id = "Intel64 Family 6 Model 142 Stepping 12";
        assertThat(WindowsCentralProcessor.parseIdentifier(id, "Family"), is("6"));
        assertThat(WindowsCentralProcessor.parseIdentifier(id, "Model"), is("142"));
        assertThat(WindowsCentralProcessor.parseIdentifier(id, "Stepping"), is("12"));
    }

    @Test
    void testParseIdentifierNotFound() {
        assertThat(WindowsCentralProcessor.parseIdentifier("Intel64 Family 6", "Model"), is(""));
    }

    @Test
    void testParseIdentifierKeyAtEnd() {
        // Key is the last token — no value follows
        assertThat(WindowsCentralProcessor.parseIdentifier("Family 6 Model", "Model"), is(""));
    }

    @Test
    void testNoFeaturesPresent() {
        assertTrue(WindowsCentralProcessor.queryFeatureFlags(f -> false).isEmpty());
    }

    @Test
    void testAllFeaturesPresent() {
        List<String> all = WindowsCentralProcessor.queryFeatureFlags(f -> true);
        assertEquals(90, all.size());
        assertEquals(new HashSet<>(all).size(), all.size(), "feature names should be unique");
        for (String name : all) {
            assertTrue(name.startsWith("PF_"), name);
        }
    }

    @Test
    void testOnlyRequestedFeaturesReported() {
        // 3 = PF_MMX_INSTRUCTIONS_AVAILABLE, 39 = PF_AVX_INSTRUCTIONS_AVAILABLE
        Set<Integer> present = new HashSet<>(Arrays.asList(3, 39));
        IntPredicate predicate = present::contains;
        assertEquals(Arrays.asList("PF_MMX_INSTRUCTIONS_AVAILABLE", "PF_AVX_INSTRUCTIONS_AVAILABLE"),
                WindowsCentralProcessor.queryFeatureFlags(predicate));
    }

    @Test
    void testFeatureValuesAreContiguous() {
        // The enum mirrors winnt.h, whose PF_ defines run 0 through 89 with no gaps
        Set<Integer> queried = new HashSet<>();
        WindowsCentralProcessor.queryFeatureFlags(f -> {
            queried.add(f);
            return false;
        });
        Set<Integer> expected = new HashSet<>();
        for (int i = 0; i <= 89; i++) {
            expected.add(i);
        }
        assertEquals(expected, queried);
    }
}
