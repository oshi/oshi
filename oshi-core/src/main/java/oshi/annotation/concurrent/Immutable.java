/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.annotation.concurrent;

import java.lang.annotation.Documented;

/**
 * The presence of this annotation indicates that the author believes the class
 * to be immutable and hence inherently thread-safe. An immutable class is one
 * where the state of an instance cannot be <i>seen</i> to change. As a result
 * <ul>
 * <li>All public fields must be {@code final}</li>
 * <li>All public final reference fields are either {@code null} or refer to
 * other immutable objects</li>
 * <li>Constructors and methods do not publish references to any potentially
 * mutable internal state.</li>
 * </ul>
 * Performance optimization may mean that instances of an immutable class may
 * have mutable internal state. The critical point is that callers cannot tell
 * the difference. For example {@link String} is an immutable class, despite
 * having an internal int that is non-final but used as a cache for
 * {@link String#hashCode()}.
 * <p>
 * Immutable objects are inherently thread-safe; they may be passed between
 * threads or published without synchronization.
 * <p>
 * This annotation is intended for internal use in OSHI as a temporary
 * workaround until it is available in {@code jakarta.annotations}.
 */
@Documented
public @interface Immutable {
}
