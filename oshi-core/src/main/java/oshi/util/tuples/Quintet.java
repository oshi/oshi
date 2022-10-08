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
 * @param <E> Type of the fifth element
 */
@ThreadSafe
public class Quintet<A, B, C, D, E> {

    private final A a;
    private final B b;
    private final C c;
    private final D d;
    private final E e;

    /**
     * Create a quintet and store five objects.
     *
     * @param a the first object to store
     * @param b the second object to store
     * @param c the third object to store
     * @param d the fourth object to store
     * @param e the fifth object to store
     */
    public Quintet(A a, B b, C c, D d, E e) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
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

    /**
     * Returns the fifth stored object.
     *
     * @return fifth object stored
     */
    public final E getE() {
        return e;
    }
}
