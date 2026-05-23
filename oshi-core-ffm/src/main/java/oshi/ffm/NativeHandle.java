/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AutoCloseable} wrapper around a native handle (represented as a {@link MemorySegment}). When closed, the
 * provided closer function is invoked to release the native resource.
 * <p>
 * This class is intended for use in try-with-resources blocks to ensure native handles are properly released, even when
 * exceptions occur.
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * try (NativeHandle h = NativeHandle.of(Kernel32FFM.OpenProcess(...), Kernel32FFM::CloseHandle)) {
 *     // use h.get()
 * }
 * }</pre>
 */
public final class NativeHandle implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NativeHandle.class);

    /**
     * A consumer that accepts a {@link MemorySegment} and may throw.
     */
    @FunctionalInterface
    public interface ThrowingCloser {
        /**
         * Performs the close operation on the given handle.
         *
         * @param handle the native handle to close
         * @throws Throwable if the close operation fails
         */
        void close(MemorySegment handle) throws Throwable;
    }

    private final MemorySegment handle;
    private final ThrowingCloser closer;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private NativeHandle(MemorySegment handle, ThrowingCloser closer) {
        this.handle = handle;
        this.closer = Objects.requireNonNull(closer, "closer");
    }

    /**
     * Creates a new {@code NativeHandle} wrapping the given segment with the specified closer.
     *
     * @param handle the native handle segment; may be {@code null} or {@link MemorySegment#NULL}
     * @param closer the function to call to release the handle
     * @return a new {@code NativeHandle}
     */
    public static NativeHandle of(MemorySegment handle, ThrowingCloser closer) {
        return new NativeHandle(handle, closer);
    }

    /**
     * Returns the underlying handle segment.
     *
     * @return the handle, which may be {@code null} or {@link MemorySegment#NULL}
     */
    public MemorySegment get() {
        return handle;
    }

    /**
     * Tests whether this handle is null or points to address zero.
     *
     * @return {@code true} if the handle is {@code null} or {@link MemorySegment#NULL}
     */
    public boolean isNull() {
        return handle == null || handle.address() == 0;
    }

    /**
     * Releases the native handle by invoking the closer. Does nothing if the handle is null or zero, or if already
     * closed. Any exception thrown by the closer is logged and suppressed. This method is idempotent.
     */
    @Override
    public void close() {
        if (!isNull() && closed.compareAndSet(false, true)) {
            try {
                closer.close(handle);
            } catch (Throwable t) {
                LOG.debug("Failed to close native handle: {}", t.getMessage());
            }
        }
    }
}
