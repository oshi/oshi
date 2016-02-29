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
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.mac;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.ptr.IntByReference;

import oshi.hardware.Sensors;
import oshi.jna.platform.mac.IOKit;
import oshi.jna.platform.mac.IOKit.IOConnect;
import oshi.jna.platform.mac.IOKit.MachPort;
import oshi.jna.platform.mac.IOKit.SMCKeyData;
import oshi.jna.platform.mac.IOKit.SMCKeyDataKeyInfo;
import oshi.jna.platform.mac.IOKit.SMCVal;
import oshi.jna.platform.mac.SystemB;
import oshi.util.Util;

public class MacSensors implements Sensors {
    private static final Logger LOG = LoggerFactory.getLogger(MacSensors.class);

    private IOConnect conn = new IOConnect();

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    // Cache the first key query
    private Map<Integer, SMCKeyDataKeyInfo> keyInfoCache = new HashMap<Integer, SMCKeyDataKeyInfo>();

    // Store some things to throttle SMC queries
    private double lastTemp = 0d;

    private long lastTempTime;

    private int numFans = 0;

    private int[] lastFanSpeeds = new int[0];

    private long lastFanSpeedsTime;

    private double lastVolts = 0d;

    private long lastVoltsTime;

    public MacSensors() {
        smcOpen();
        // Do an initial read of temperature and fan speeds. This caches initial
        // dataInfo and improves success of future queries
        this.lastTemp = getCpuTemperature();
        this.lastFanSpeeds = getFanSpeeds();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                smcClose();
            }
        });
    }

    /*
     * Open connection to SMC
     */
    private int smcOpen() {
        int service = 0;
        MachPort masterPort = new MachPort();

        int result = IOKit.INSTANCE.IOMasterPort(0, masterPort);
        if (result != 0) {
            LOG.error(String.format("Error: IOMasterPort() = %08x", result));
            return result;
        }

        service = IOKit.INSTANCE.IOServiceGetMatchingService(masterPort.getValue(),
                IOKit.INSTANCE.IOServiceMatching("AppleSMC"));
        if (service == 0) {
            LOG.error("Error: no SMC found\n");
            return result;
        }

        result = IOKit.INSTANCE.IOServiceOpen(service, SystemB.INSTANCE.mach_task_self(), 0, this.conn);
        IOKit.INSTANCE.IOObjectRelease(service);
        if (result != 0) {
            LOG.error(String.format("Error: IOServiceOpen() = 0x%08x", result));
            return result;
        }
        // Delay to improve success of next query
        Util.sleep(5);
        return 0;
    }

    private int smcClose() {
        return IOKit.INSTANCE.IOServiceClose(this.conn.getValue());
    }

    /*
     * Get Temperature from SMC
     */
    private double smcGetTemperature(String key, int retries) {
        SMCVal val = new SMCVal();
        int result = smcReadKey(key, val, retries);
        if (result == 0) {
            int temp = 0;
            if (val.dataSize > 0 && Arrays.equals(val.dataType, IOKit.DATATYPE_SP78)) {
                temp = (val.bytes[0] * 256 + val.bytes[1]) >> 2;
            }
            return temp / 64.0;
        }
        // Read failed
        return 0d;
    }

    /*
     * Get number of fans
     */
    private int smcGetFanNumber(int retries) {
        SMCVal val = new SMCVal();
        int result = smcReadKey(IOKit.SMC_KEY_FAN_NUM, val, retries);
        if (result == 0) {
            return strtoul10(val.bytes, val.dataSize);
        }
        // Read failed
        return 0;
    }

    /*
     * Get fan speed
     */
    private float smcGetFanSpeed(int fanNum, int retries) {
        SMCVal val = new SMCVal();
        String key = String.format(IOKit.SMC_KEY_FAN_SPEED, fanNum);
        int result = smcReadKey(key, val, retries);
        if (result == 0) {
            return strtof(val.bytes, val.dataSize, 2);
        }
        // Read failed
        return 0f;
    }

    /*
     * Get voltage
     */
    private float smcGetVoltage(int retries) {
        SMCVal val = new SMCVal();
        int result = smcReadKey(IOKit.SMC_KEY_CPU_VOLTAGE, val, retries);
        if (result == 0) {
            return strtof(val.bytes, val.dataSize, 2) / 1000f;
        }
        // Read failed
        return 0f;
    }

    /*
     * Get cached keyInfo if it exists, or generate new
     */
    private int smcGetKeyInfo(SMCKeyData inputStructure, SMCKeyData outputStructure) {
        if (keyInfoCache.containsKey(inputStructure.key)) {
            SMCKeyDataKeyInfo keyInfo = keyInfoCache.get(inputStructure.key);
            outputStructure.keyInfo.dataSize = keyInfo.dataSize;
            outputStructure.keyInfo.dataType = keyInfo.dataType;
            outputStructure.keyInfo.dataAttributes = keyInfo.dataAttributes;
        } else {
            int result = 0;
            inputStructure.data8 = IOKit.SMC_CMD_READ_KEYINFO;
            Util.sleep(4);
            result = smcCall(IOKit.KERNEL_INDEX_SMC, inputStructure, outputStructure);
            if (result != 0) {
                return result;
            }
            SMCKeyDataKeyInfo keyInfo = new SMCKeyDataKeyInfo();
            keyInfo.dataSize = outputStructure.keyInfo.dataSize;
            keyInfo.dataType = outputStructure.keyInfo.dataType;
            keyInfo.dataAttributes = outputStructure.keyInfo.dataAttributes;
            keyInfoCache.put(inputStructure.key, keyInfo);
        }
        return 0;
    }

    /*
     * Read a key from SMC
     */
    private int smcReadKey(String key, SMCVal val, int retries) {
        SMCKeyData inputStructure = new SMCKeyData();
        SMCKeyData outputStructure = new SMCKeyData();

        inputStructure.key = strtoul16(key, 4);
        int result;
        do {
            result = smcGetKeyInfo(inputStructure, outputStructure);
            if (result != 0) {
                continue;
            }

            val.dataSize = outputStructure.keyInfo.dataSize;
            val.dataType = new byte[5];
            for (int i = 3; i >= 0; i--) {
                val.dataType[i] = (byte) (outputStructure.keyInfo.dataType >> (8 * (3 - i)));
            }
            inputStructure.keyInfo.dataSize = val.dataSize;
            inputStructure.data8 = IOKit.SMC_CMD_READ_BYTES;

            Util.sleep(4);
            result = smcCall(IOKit.KERNEL_INDEX_SMC, inputStructure, outputStructure);
            if (result != 0) {
                continue;
            }
            break;
        } while (--retries > 0);
        // If we errored out return code
        if (result != 0) {
            return result;
        }

        System.arraycopy(outputStructure.bytes, 0, val.bytes, 0, val.bytes.length);
        // Success
        return 0;
    }

    /*
     * Call SMC
     */
    private int smcCall(int index, SMCKeyData inputStructure, SMCKeyData outputStructure) {
        int structureInputSize = inputStructure.size();
        IntByReference structureOutputSizePtr = new IntByReference(outputStructure.size());

        int result = IOKit.INSTANCE.IOConnectCallStructMethod(this.conn.getValue(), index, inputStructure,
                structureInputSize, outputStructure, structureOutputSizePtr);
        if (result != 0) {
            // This seems to be a common error, suppressing output
            // LOG.error(String.format("Error: IOConnectCallStructMethod() =
            // 0x%08x", result));
            return result;
        }
        // Success
        return result;
    }

    private int strtoul16(String str, int size) {
        int total = 0;
        int i;

        for (i = 0; i < size; i++) {
            total += str.charAt(i) << (size - 1 - i) * 8;
        }
        return total;
    }

    private int strtoul10(byte[] bytes, int size) {
        int total = 0;
        int i;

        for (i = 0; i < size; i++) {
            total += (byte) (bytes[i] << (size - 1 - i) * 8);
        }
        return total;
    }

    float strtof(byte[] bytes, int size, int e) {
        float total = 0;
        for (int i = 0; i < size; i++) {
            if (i == (size - 1))
                total += (bytes[i] & 0xff) >> e;
            else
                total += bytes[i] << (size - 1 - i) * (8 - e);
        }
        total += (bytes[size - 1] & 0x03) * 0.25;
        return total;
    }

    @Override
    public double getCpuTemperature() {
        // Only update every second
        if (System.currentTimeMillis() - this.lastTempTime > 900) {
            double temp = smcGetTemperature(IOKit.SMC_KEY_CPU_TEMP, 50);
            if (temp > 0d) {
                this.lastTemp = temp;
                lastTempTime = System.currentTimeMillis();
            }
        }
        return this.lastTemp;
    }

    @Override
    public int[] getFanSpeeds() {
        // Only update every second
        if (System.currentTimeMillis() - this.lastFanSpeedsTime > 900) {
            // If we don't have fan # try to get it
            if (this.numFans == 0) {
                this.numFans = smcGetFanNumber(50);
                this.lastFanSpeeds = new int[this.numFans];
            }
            for (int i = 0; i < this.numFans; i++) {
                int speed = (int) smcGetFanSpeed(i, 50);
                if (speed > 0) {
                    this.lastFanSpeeds[i] = speed;
                    lastFanSpeedsTime = System.currentTimeMillis();
                }
            }
        }
        return this.lastFanSpeeds;
    }

    @Override
    public double getCpuVoltage() {
        // Only update every second
        if (System.currentTimeMillis() - this.lastVoltsTime > 900) {
            double volts = smcGetVoltage(50);
            if (volts > 0d) {
                this.lastVolts = volts;
                lastVoltsTime = System.currentTimeMillis();
            }
        }
        return this.lastVolts;
    }

    @Override
    public JsonObject toJSON() {
        JsonArrayBuilder fanSpeedsArrayBuilder = jsonFactory.createArrayBuilder();
        for (int speed : getFanSpeeds()) {
            fanSpeedsArrayBuilder.add(speed);
        }
        return jsonFactory.createObjectBuilder().add("cpuTemperature", getCpuTemperature())
                .add("fanSpeeds", fanSpeedsArrayBuilder.build()).add("cpuVoltage", getCpuVoltage()).build();
    }
}
