/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.tuples;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Convenience class for returning multiple objects from methods.
 *
 * @param <A> Type of the first element
 * @param <B> Type of the second element
 */
@ThreadSafe
public class Pair<A, B> {

    private final A a;
    private final B b;

    /**
     * Create a pair and store two objects.
     *
     * @param a the first object to store
     * @param b the second object to store
     */
    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    /**
     * Returns the first stored object.
     *
     * @return first object stored
     */
    public final A getA() {
        return a;
    }

    /**
     * Returns the second stored object.
     *
     * @return second object stored
     */
    public final B getB() {
        return b;
    }
}
