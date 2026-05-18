/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil;

import oshi.driver.common.windows.wmi.WmiQuery;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.driver.common.windows.wmi.WmiResult;

/**
 * Adapts the JNA {@link WmiQueryHandler} to the common {@link WmiQueryExecutor} interface.
 */
public class WmiQueryExecutorJNA implements WmiQueryExecutor {

    private final WmiQueryHandler handler;

    /**
     * Creates an executor wrapping the given handler.
     *
     * @param handler the JNA WMI query handler
     */
    public WmiQueryExecutorJNA(WmiQueryHandler handler) {
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    /**
     * Creates an executor using a new default handler instance.
     *
     * @return a new executor, or {@code null} if handler creation fails
     */
    public static WmiQueryExecutorJNA createInstance() {
        WmiQueryHandler h = WmiQueryHandler.createInstance();
        return h == null ? null : new WmiQueryExecutorJNA(h);
    }

    /**
     * Returns the underlying JNA handler, for COM init/uninit operations.
     *
     * @return the JNA handler
     */
    public WmiQueryHandler getHandler() {
        return handler;
    }

    @Override
    public <T extends Enum<T>> WmiResult<T> queryWMI(WmiQuery<T> query) {
        return queryWMI(query, true);
    }

    @Override
    public <T extends Enum<T>> WmiResult<T> queryWMI(WmiQuery<T> query, boolean initCom) {
        WbemcliUtil.WmiQuery<T> jnaQuery = new WbemcliUtil.WmiQuery<>(query.getNameSpace(), query.getWmiClassName(),
                query.getPropertyEnum());
        WbemcliUtil.WmiResult<T> jnaResult = handler.queryWMI(jnaQuery, initCom);
        return new JnaWmiResult<>(jnaResult);
    }

    private static final class JnaWmiResult<T extends Enum<T>> implements WmiResult<T> {
        private final WbemcliUtil.WmiResult<T> delegate;

        JnaWmiResult(WbemcliUtil.WmiResult<T> delegate) {
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
