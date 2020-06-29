/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.unix.aix;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractBaseboard;
import oshi.util.Constants;

/**
 * Baseboard data obtained by smbios
 */
@Immutable
final class AixBaseboard extends AbstractBaseboard {

    private final String manufacturer;
    private final String model;
    private final String serialNumber;
    private final String version;

    /*-
     ~/git/oshi$ lscfg -vp
    PLATFORM SPECIFIC
    Name:  IBM,9114-275
    Model:  IBM,9114-275
    Node:  /
    Device Type:  chrp
      Platform Firmware:
        ROM Level.(alterable).......3F080425
        Version.....................RS6K
        Hardware Location Code......U0.1-P1-X1/Y1
      Physical Location: U0.1-P1-X1/Y1
      System Firmware:
        ROM Level.(alterable).......RG080425_d79e22_regatta
        Version.....................RS6K
        Hardware Location Code......U0.1-P1-X1/Y2
      Physical Location: U0.1-P1-X1/Y2
      System VPD:
        Machine/Cabinet Serial No...10ACFDE
        Machine Type and Model......9114-275
        Manufacture ID..............IBM980
        Version.....................RS6K
        Op Panel Installed..........Y
        Brand.......................I0
        Hardware Location Code......U0.1
      Physical Location: U0.1
      PS CEC OP PANEL :
        Serial Number...............YL1124335115
        EC Level....................H64013
        Customer Card ID Number.....28D3
        FRU Number.................. 97P3352
        Action Code, Timestamp......BD 200210290851
        Version.....................RS6K
        Hardware Location Code......U0.1-L1
      Physical Location: U0.1-L1
      1 WAY BACKPLANE :
        Serial Number...............YL10243490FB
        Part Number.................80P4315
        Customer Card ID Number.....26F4
        CCIN Extender...............1
        FRU Number.................. 80P4315
        Version.....................RS6K
        Hardware Location Code......U0.1-P1
      Physical Location: U0.1-P1
      CSP             :
        Serial Number...............YL1024350048
        Part Number.................80P5573
        Customer Card ID Number.....28D0
        CCIN Extender...............1
        FRU Number.................. 80P5573
        ROM Level.(alterable).......3F080425
        Version.....................RS6K
        Hardware Location Code......U0.1-P1-X1
      Physical Location: U0.1-P1-X1
     */

    AixBaseboard() {
        this.manufacturer = Constants.UNKNOWN;
        this.model = Constants.UNKNOWN;
        this.serialNumber = Constants.UNKNOWN;
        this.version = Constants.UNKNOWN;
    }

    @Override
    public String getManufacturer() {
        return this.manufacturer;
    }

    @Override
    public String getModel() {
        return this.model;
    }

    @Override
    public String getSerialNumber() {
        return this.serialNumber;
    }

    @Override
    public String getVersion() {
        return this.version;
    }
}
