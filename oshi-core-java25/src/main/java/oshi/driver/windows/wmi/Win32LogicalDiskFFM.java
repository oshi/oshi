/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.windows.com.IWbemClassObjectFFM;
import oshi.util.platform.windows.WmiQueryHandlerFFM;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;

/**
 * FFM-based utility to query WMI class {@code Win32_LogicalDisk}.
 */
@ThreadSafe
public final class Win32LogicalDiskFFM {

    private static final String WIN32_LOGICAL_DISK = "Win32_LogicalDisk";

    private Win32LogicalDiskFFM() {
    }

    /**
     * Represents a logical disk from WMI query result.
     */
    public static class LogicalDiskInfo {
        private int access;
        private String description = "";
        private int driveType;
        private String fileSystem = "";
        private long freeSpace;
        private String name = "";
        private String providerName = "";
        private long size;
        private String volumeName = "";

        public int getAccess() {
            return access;
        }

        public void setAccess(int access) {
            this.access = access;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getDriveType() {
            return driveType;
        }

        public void setDriveType(int driveType) {
            this.driveType = driveType;
        }

        public String getFileSystem() {
            return fileSystem;
        }

        public void setFileSystem(String fileSystem) {
            this.fileSystem = fileSystem;
        }

        public long getFreeSpace() {
            return freeSpace;
        }

        public void setFreeSpace(long freeSpace) {
            this.freeSpace = freeSpace;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getProviderName() {
            return providerName;
        }

        public void setProviderName(String providerName) {
            this.providerName = providerName;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getVolumeName() {
            return volumeName;
        }

        public void setVolumeName(String volumeName) {
            this.volumeName = volumeName;
        }
    }

    /**
     * Queries logical disk information.
     *
     * @param nameToMatch an optional string to filter match, null otherwise
     * @param localOnly   whether to only search local drives
     * @return list of logical disk information
     */
    public static List<LogicalDiskInfo> queryLogicalDisk(String nameToMatch, boolean localOnly) {
        StringBuilder whereClause = new StringBuilder();
        boolean hasWhere = false;

        if (localOnly) {
            whereClause.append("WHERE (DriveType = 2 OR DriveType = 3 OR DriveType = 6)");
            hasWhere = true;
        }

        if (nameToMatch != null) {
            whereClause.append(hasWhere ? " AND" : " WHERE");
            whereClause.append(" Name=\"").append(nameToMatch).append("\"");
        }

        return WmiQueryHandlerFFM.getInstance().queryWMI(WIN32_LOGICAL_DISK,
                whereClause.length() > 0 ? whereClause.toString() : null, LogicalDiskInfo::new,
                Win32LogicalDiskFFM::populateLogicalDiskInfo);
    }

    /**
     * Populates a LogicalDiskInfo object from a WMI row.
     *
     * @param pObject the IWbemClassObject pointer
     * @param arena   the arena for memory allocation
     * @param info    the LogicalDiskInfo to populate
     */
    private static void populateLogicalDiskInfo(MemorySegment pObject, Arena arena, LogicalDiskInfo info) {
        info.setAccess(IWbemClassObjectFFM.getInt(pObject, "Access", arena));
        info.setDescription(IWbemClassObjectFFM.getString(pObject, "Description", arena));
        info.setDriveType(IWbemClassObjectFFM.getInt(pObject, "DriveType", arena));
        info.setFileSystem(IWbemClassObjectFFM.getString(pObject, "FileSystem", arena));
        info.setFreeSpace(IWbemClassObjectFFM.getLong(pObject, "FreeSpace", arena));
        info.setName(IWbemClassObjectFFM.getString(pObject, "Name", arena));
        info.setProviderName(IWbemClassObjectFFM.getString(pObject, "ProviderName", arena));
        info.setSize(IWbemClassObjectFFM.getLong(pObject, "Size", arena));
        info.setVolumeName(IWbemClassObjectFFM.getString(pObject, "VolumeName", arena));
    }
}
