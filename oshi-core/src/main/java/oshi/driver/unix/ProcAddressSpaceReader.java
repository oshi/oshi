/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.platform.unix.LibCAPI.size_t;
import com.sun.jna.platform.unix.LibCAPI.ssize_t;

import oshi.annotation.concurrent.NotThreadSafe;
import oshi.jna.platform.unix.CLibrary;

/**
 * Reads pointer-sized values and NUL-terminated strings out of a process's {@code /proc/<pid>/as} address space via
 * libc {@code pread}, buffering one page at a time. Shared by the Solaris and AIX JNA {@code PsInfo} drivers, which
 * read a process's argument and environment vectors this way.
 * <p>
 * Not thread-safe: each instance owns a single file descriptor and a reusable page buffer, so confine it to one thread
 * and {@link #close()} it when done (it is {@link AutoCloseable}).
 */
@NotThreadSafe
public final class ProcAddressSpaceReader implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ProcAddressSpaceReader.class);

    private final CLibrary libc;
    private final int fd;
    private final long pageSize;
    private final Memory buffer;
    private final size_t bufSize;
    private long bufStart;
    private boolean bufValid;

    private ProcAddressSpaceReader(CLibrary libc, int fd, long pageSize) {
        this.libc = libc;
        this.fd = fd;
        this.pageSize = pageSize;
        this.buffer = new Memory(pageSize * 2);
        this.bufSize = new size_t(this.buffer.size());
        this.bufStart = 0L;
        this.bufValid = false;
    }

    /**
     * Opens {@code /proc/<pid>/as} for reading.
     *
     * @param libc     the platform C library providing {@code open}/{@code pread}/{@code close}
     * @param pid      the process ID
     * @param pageSize the memory page size in bytes
     * @return a reader, or {@code null} if the address space could not be opened (e.g. insufficient permission)
     */
    public static ProcAddressSpaceReader open(CLibrary libc, int pid, long pageSize) {
        String procas = "/proc/" + pid + "/as";
        int fd = libc.open(procas, 0);
        if (fd < 0) {
            LOG.trace("No permission to read file: {} ", procas);
            return null;
        }
        return new ProcAddressSpaceReader(libc, fd, pageSize);
    }

    /**
     * Reads a pointer-sized value at {@code addr}.
     *
     * @param addr      the address to read
     * @param increment the pointer size in bytes ({@code 8} for a 64-bit process, otherwise a 32-bit value)
     * @return the pointer value, or {@code 0} if the page could not be read
     */
    public long readPointer(long addr, long increment) {
        if (!ensurePage(addr)) {
            return 0L;
        }
        return decodePointer(this.buffer, addr - this.bufStart, increment);
    }

    /**
     * Reads a NUL-terminated string at {@code addr}.
     *
     * @param addr the address to read
     * @return the string, or an empty string if the page could not be read
     */
    public String readString(long addr) {
        if (!ensurePage(addr)) {
            return "";
        }
        return this.buffer.getString(addr - this.bufStart);
    }

    /**
     * Ensures the page containing {@code addr} is loaded in the buffer, reading it via {@code pread} if it is not
     * already present.
     *
     * @param addr the address whose page must be buffered
     * @return {@code true} if the page is available, {@code false} on read failure
     */
    private boolean ensurePage(long addr) {
        // Reuse the buffer only if it holds a valid page covering addr
        if (this.bufValid && addr >= this.bufStart && addr - this.bufStart <= this.pageSize) {
            return true;
        }
        // pread overwrites the buffer, so invalidate the cached page before reading: a short read would otherwise
        // leave bufStart pointing at a page the buffer no longer fully holds.
        this.bufValid = false;
        long newStart = Math.floorDiv(addr, this.pageSize) * this.pageSize;
        ssize_t result = this.libc.pread(this.fd, this.buffer, this.bufSize, new NativeLong(newStart));
        // May return less than asked but should be at least a full page
        if (result.longValue() < this.pageSize) {
            LOG.debug("Failed to read page from address space: {} bytes read", result.longValue());
            return false;
        }
        this.bufStart = newStart;
        this.bufValid = true;
        return true;
    }

    /**
     * Decodes a pointer-sized value from a buffer. For 32-bit pointers the value is read unsigned so high addresses
     * ({@code >= 0x80000000}) are not sign-extended.
     *
     * @param buffer    the buffer to read from
     * @param offset    the offset within the buffer
     * @param increment the pointer size in bytes ({@code 8} for 64-bit, otherwise 32-bit)
     * @return the decoded value
     */
    static long decodePointer(Memory buffer, long offset, long increment) {
        return increment == 8 ? buffer.getLong(offset) : Integer.toUnsignedLong(buffer.getInt(offset));
    }

    @Override
    public void close() {
        this.libc.close(this.fd);
        this.buffer.close();
    }
}
