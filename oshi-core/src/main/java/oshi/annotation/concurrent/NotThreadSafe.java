/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.annotation.concurrent;

import java.lang.annotation.Documented;

/**
 * The presence of this annotation indicates that the author believes the class is not thread-safe. The absence of this
 * annotation does not indicate that the class is thread-safe, instead this annotation is for cases where a naïve
 * assumption could be easily made that the class is thread-safe. In general, it is a bad plan to assume a class is
 * thread safe without good reason.
 * <p>
 * This annotation is intended for internal use in OSHI as a temporary workaround until it is available in
 * {@code jakarta.annotations}.
 */
@Documented
public @interface NotThreadSafe {
}
