/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

/**
 * Common interface for executing WMI queries, abstracting JNA and FFM implementations.
 * <p>
 * JNA users who wish to customize COM initialization (e.g., to avoid per-query COM init/uninit overhead) should extend
 * {@code oshi.util.platform.windows.WmiQueryHandler} in the {@code oshi-core} module. See the {@code oshi-demo} module
 * for examples.
 */
public interface WmiQueryExecutor {

    /**
     * Query WMI for values. Initializes and uninitializes COM for each query.
     *
     * @param <T>   the enum type for the query properties
     * @param query the WMI query
     * @return the query results
     */
    <T extends Enum<T>> WmiResult<T> queryWMI(WmiQuery<T> query);

    /**
     * Query WMI for values.
     *
     * @param <T>     the enum type for the query properties
     * @param query   the WMI query
     * @param initCom whether to initialize COM for this query. If {@code false}, assumes COM is already initialized.
     * @return the query results
     */
    <T extends Enum<T>> WmiResult<T> queryWMI(WmiQuery<T> query, boolean initCom);

    /**
     * Creates a new instance of the platform-appropriate WMI query executor.
     * <p>
     * On JNA, this returns an instance of {@code WmiQueryHandler} (or a user-configured subclass). On FFM, this returns
     * an instance of {@code WmiQueryHandlerFFM}.
     *
     * @return a new executor instance, or {@code null} if creation fails
     */
    static WmiQueryExecutor createInstance() {
        throw new UnsupportedOperationException("Use platform-specific createInstance()");
    }
}
