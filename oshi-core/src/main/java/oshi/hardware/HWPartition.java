/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
package oshi.hardware;

import java.io.Serializable;

/**
 * A region on a hard disk or other secondary storage, so that an operating
 * system can manage information in each region separately. A partition appears
 * in the operating system as a distinct "logical" disk that uses part of the
 * actual disk.
 *
 * @author widdis[at]gmail[dot]com
 */
public class HWPartition implements Serializable {

    private static final long serialVersionUID = 1L;

    private String identification;
    private String name;
    private String type;
    private String uuid;
    private long size;
    private int diskId;
    private int partitionId;
    private boolean active;

    /**
     * @return Returns the identification.
     */
    public String getIdentification() {
        return identification;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Returns the type.
     */
    public String getType() {
        return type;
    }

    /**
     * @return Returns the uuid.
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * @return Returns the size in bytes.
     */
    public long getSize() {
        return size;
    }

    /**
     * @return Returns the diskId.
     */
    public int getDiskId() {
        return diskId;
    }

    /**
     * @return Returns the partitionId.
     */
    public int getPartitionId() {
        return partitionId;
    }

    /**
     * @return Returns the active.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @param identification
     *            The identification to set.
     */
    public void setIdentification(String identification) {
        this.identification = identification;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param type
     *            The type to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @param uuid
     *            The uuid to set.
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * @param size
     *            The size (in bytes) to set.
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * @param diskId
     *            The diskId to set.
     */
    public void setDiskId(int diskId) {
        this.diskId = diskId;
    }

    /**
     * @param partitionId
     *            The partitionId to set.
     */
    public void setPartitionId(int partitionId) {
        this.partitionId = partitionId;
    }

    /**
     * @param active
     *            Set whether the partition is active.
     */
    public void setActive(boolean active) {
        this.active = active;
    }

}
