/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.mac;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.LogLevel;
import oshi.util.LogUtil;

/**
 * Reports why a connection to the SMC could not be opened, at most once per condition.
 * <p>
 * Sensor readings poll, and each one opens its own connection: a single pass over CPU temperature, fan speeds, and CPU
 * voltage opens three. When the service is permanently unavailable an unlatched message therefore repeats for the life
 * of the process, several times per sampling interval. That is the normal state on a virtual machine, which does not
 * virtualize the SMC, so callers on a VM saw the failure reported continuously.
 * <p>
 * Each condition is instead reported once at a level that reflects how much it should alarm a reader, and at
 * {@link LogLevel#DEBUG} on every later occurrence. An absent service is a degradation rather than an error: OSHI
 * reports no sensors and carries on. Failing to open a service that is present is an error, because the SMC is there
 * and should have answered.
 * <p>
 * One instance per backend, holding that backend's logger, so the message is attributed to the class that could not
 * open the connection rather than to this one.
 */
@ThreadSafe
public final class SmcOpenFailure {

    private final Logger log;

    private final AtomicBoolean serviceNotFoundLogged = new AtomicBoolean();
    private final AtomicBoolean openFailedLogged = new AtomicBoolean();
    private final AtomicBoolean nullConnectionLogged = new AtomicBoolean();

    /**
     * Creates a reporter.
     *
     * @param log the logger of the class opening the connection, under whose name the messages are reported
     */
    public SmcOpenFailure(Logger log) {
        this.log = log;
    }

    /**
     * Reports that the {@code AppleSMC} service could not be found, so no sensor can be read.
     */
    public void serviceNotFound() {
        LogUtil.logAtLevel(log, levelFor(serviceNotFoundLogged, LogLevel.WARN),
                "Unable to locate the AppleSMC service; hardware sensors are unavailable."
                        + " This is expected on a virtual machine, which does not virtualize the SMC.");
    }

    /**
     * Reports that the {@code AppleSMC} service was found but would not open.
     *
     * @param result the nonzero {@code kern_return_t} from {@code IOServiceOpen}
     */
    public void openFailed(int result) {
        LogLevel level = levelFor(openFailedLogged, LogLevel.ERROR);
        if (LogUtil.isEnabled(log, level)) {
            LogUtil.logAtLevel(log, level, "Unable to open a connection to the AppleSMC service. Error: 0x{}",
                    String.format(Locale.ROOT, "%08x", result));
        }
    }

    /**
     * Reports that {@code IOServiceOpen} succeeded but handed back no connection to use.
     */
    public void nullConnection() {
        LogUtil.logAtLevel(log, levelFor(nullConnectionLogged, LogLevel.ERROR),
                "IOServiceOpen reported success but returned a null AppleSMC connection handle.");
    }

    /**
     * Returns the level for this occurrence, at the same time latching the condition as reported.
     *
     * @param latch the condition's latch
     * @param first the level for the first occurrence
     * @return {@code first} if this is the first occurrence, otherwise {@link LogLevel#DEBUG}
     */
    private static LogLevel levelFor(AtomicBoolean latch, LogLevel first) {
        return latch.compareAndSet(false, true) ? first : LogLevel.DEBUG;
    }
}
