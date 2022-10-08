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
 * @param <C> Type of the third element
 * @param <D> Type of the fourth element
 */
@ThreadSafe
public class Quartet<A, B, C, D> {

    private final A a;
    private final B b;
    private final C c;
    private final D d;

    /**
     * Create a quartet and store four objects.
     *
     * @param a the first object to store
     * @param b the second object to store
     * @param c the third object to store
     * @param d the fourth object to store
     */
    public Quartet(A a, B b, C c, D d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
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

    /**
     * Returns the third stored object.
     *
     * @return third object stored
     */
    public final C getC() {
        return c;
    }

    /**
     * Returns the fourth stored object.
     *
     * @return fourth object stored
     */
    public final D getD() {
        return d;
    }
}
