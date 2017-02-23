/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */

package oshi.software.os;

import oshi.software.common.AbstractProcess;

public class DefaultAbstractProcess extends AbstractProcess{

    public DefaultAbstractProcess(String name, String path, State state, int processID, int parentProcessID, int threadCount, int priority, long virtualSize, long residentSetSize, long kernelTime, long userTime, long startTime, long upTime, long bytesRead, long bytesWritten) {
        super(name, path, state, processID, parentProcessID, threadCount, priority, virtualSize, residentSetSize, kernelTime, userTime, startTime, upTime, bytesRead, bytesWritten);
    }
}
