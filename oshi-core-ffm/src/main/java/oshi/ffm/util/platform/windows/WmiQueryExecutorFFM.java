/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.windows;

import java.util.Objects;

import oshi.driver.common.windows.wmi.WmiQuery;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.driver.common.windows.wmi.WmiResult;

/**
 * Adapts the FFM {@link WmiQueryHandlerFFM} to the common {@link WmiQueryExecutor} interface.
 */
public class WmiQueryExecutorFFM implements WmiQueryExecutor {

    private final WmiQueryHandlerFFM handler;

    /**
     * Creates an executor wrapping the given handler.
     *
     * @param handler the FFM WMI query handler
     */
    public WmiQueryExecutorFFM(WmiQueryHandlerFFM handler) {
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    /**
     * Creates an executor using a new default handler instance.
     *
     * @return a new executor, or {@code null} if handler creation fails
     */
    public static WmiQueryExecutorFFM createInstance() {
        WmiQueryHandlerFFM h = WmiQueryHandlerFFM.createInstance();
        return h == null ? null : new WmiQueryExecutorFFM(h);
    }

    /**
     * Returns the underlying FFM handler, for COM init/uninit operations.
     *
     * @return the FFM handler
     */
    public WmiQueryHandlerFFM getHandler() {
        return handler;
    }

    @Override
    public <T extends Enum<T>> WmiResult<T> queryWMI(WmiQuery<T> query) {
        return queryWMI(query, true);
    }

    @Override
    public <T extends Enum<T>> WmiResult<T> queryWMI(WmiQuery<T> query, boolean initCom) {
        WbemcliUtilFFM.WmiQuery<T> ffmQuery = new WbemcliUtilFFM.WmiQuery<>(query.getNameSpace(),
                query.getWmiClassName(), query.getPropertyEnum());
        WbemcliUtilFFM.WmiResult<T> ffmResult = handler.queryWMI(ffmQuery, initCom);
        return new FfmWmiResult<>(ffmResult);
    }

    private static final class FfmWmiResult<T extends Enum<T>> implements WmiResult<T> {
        private final WbemcliUtilFFM.WmiResult<T> delegate;

        FfmWmiResult(WbemcliUtilFFM.WmiResult<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int getResultCount() {
            return delegate.getResultCount();
        }

        @Override
        public Object getValue(T property, int index) {
            return delegate.getValue(property, index);
        }

        @Override
        public int getVtType(T property) {
            return delegate.getVtType(property);
        }

        @Override
        public int getCIMType(T property) {
            return delegate.getCIMType(property);
        }
    }
}
