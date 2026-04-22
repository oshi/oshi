/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated type is part of the OSHI public API. Public API types follow
 * <a href="https://semver.org/">Semantic Versioning</a>: they are guaranteed to remain binary-compatible within the
 * same major version.
 * <p>
 * Types <em>not</em> annotated with {@code @PublicApi} (such as platform-specific implementations, drivers, and utility
 * classes) may change between minor releases.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PublicApi {
}
