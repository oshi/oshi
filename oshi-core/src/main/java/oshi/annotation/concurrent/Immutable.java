/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.annotation.concurrent;

import java.lang.annotation.Documented;

/**
 * The presence of this annotation indicates that the author believes the class to be immutable and hence inherently
 * thread-safe. An immutable class is one where the state of an instance cannot be <i>seen</i> to change. As a result
 * <ul>
 * <li>All public fields must be {@code final}</li>
 * <li>All public final reference fields are either {@code null} or refer to other immutable objects</li>
 * <li>Constructors and methods do not publish references to any potentially mutable internal state.</li>
 * </ul>
 * Performance optimization may mean that instances of an immutable class may have mutable internal state. The critical
 * point is that callers cannot tell the difference. For example {@link String} is an immutable class, despite having an
 * internal int that is non-final but used as a cache for {@link String#hashCode()}.
 * <p>
 * Immutable objects are inherently thread-safe; they may be passed between threads or published without
 * synchronization.
 * <p>
 * This annotation is intended for internal use in OSHI as a temporary workaround until it is available in
 * {@code jakarta.annotations}.
 */
@Documented
public @interface Immutable {
}
