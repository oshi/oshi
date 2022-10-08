/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.tuples.Pair;

/**
 * Uses OSHI to attempt to identify which OSFileStore, HWDiskStore, and HWPartition a file resides on. Intended as a
 * demonstration, not intended to be used in production code.
 * <p>
 * In pariticular, this won't work in all cases, particularly with logical partitions.
 */
public class DiskStoreForPath {
    /**
     * Main method
     *
     * @param args Optional file path
     * @throws URISyntaxException on invalid path
     */
    public static void main(String[] args) throws URISyntaxException {
        // Use the arg as a file path or get this class's path
        String filePath = args.length > 0 ? args[0]
                : new File(DiskStoreForPath.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                        .getPath();
        System.out.println("Searching stores for path: " + filePath);

        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        List<HWDiskStore> diskStores = hal.getDiskStores();
        Pair<Integer, Integer> dsPartIdx = getDiskStoreAndPartitionForPath(filePath, diskStores);
        int dsIndex = dsPartIdx.getA();
        int partIndex = dsPartIdx.getB();

        System.out.println();
        System.out.println("DiskStore index " + dsIndex + " and Partition index " + partIndex);
        if (dsIndex >= 0 && partIndex >= 0) {
            System.out.println(diskStores.get(dsIndex));
            System.out.println(" |-- " + diskStores.get(dsIndex).getPartitions().get(partIndex));
        } else {
            System.out.println("Couldn't find that path on a partition.");
        }

        OperatingSystem os = si.getOperatingSystem();
        List<OSFileStore> fileStores = os.getFileSystem().getFileStores();
        int fsIndex = getFileStoreForPath(filePath, fileStores);

        System.out.println();
        System.out.println("FileStore index " + fsIndex);
        if (fsIndex >= 0) {
            System.out.println(fileStores.get(fsIndex));
        } else {
            System.out.println("Couldn't find that path on a filestore.");
        }
    }

    private static Pair<Integer, Integer> getDiskStoreAndPartitionForPath(String path, List<HWDiskStore> diskStores) {
        for (int ds = 0; ds < diskStores.size(); ds++) {
            HWDiskStore store = diskStores.get(ds);
            List<HWPartition> parts = store.getPartitions();
            for (int part = 0; part < parts.size(); part++) {
                String mount = parts.get(part).getMountPoint();
                if (!mount.isEmpty() && path.substring(0, mount.length()).equalsIgnoreCase(mount)) {
                    return new Pair<>(ds, part);
                }
            }
        }
        return new Pair<>(-1, -1);
    }

    private static int getFileStoreForPath(String path, List<OSFileStore> fileStores) {
        for (int fs = 0; fs < fileStores.size(); fs++) {
            String mount = fileStores.get(fs).getMount();
            if (!mount.isEmpty() && path.substring(0, mount.length()).equalsIgnoreCase(mount)) {
                return fs;
            }
        }
        return -1;
    }
}
