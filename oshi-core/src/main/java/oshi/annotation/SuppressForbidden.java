/*
 * Copyright 2020-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The use of some forbidden APIs such as {@link System#out} and {@link System#err} is generally discouraged. However,
 * the {@code oshi-demo} project is intended to provide proof-of-concept examples not intended for production use, so
 * exceptions can be made in demo classes.
 * <p>
 * This annotation exists to permit suppression in the Forbidden APIs check and also indicate to consumers of these
 * classes that they may need to handle these differently in their production code.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface SuppressForbidden {
    String reason();
}
