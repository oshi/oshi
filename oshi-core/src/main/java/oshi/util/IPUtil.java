/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.util;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressNetwork;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.ipv4.IPv4Address;
import inet.ipaddr.ipv6.IPv6Address;
import oshi.annotation.concurrent.ThreadSafe;

import java.math.BigInteger;

/**
 * Formatting utility for appending units or converting IP address types.
 */
@SuppressWarnings({"AlibabaClassNamingShouldBeCamel", "unused"})
@ThreadSafe
public final class IPUtil {
    public static final long IP_VERSION_4 = 4;
    public static final long IP_VERSION_6 = 6;
    public static final String IP_SEPERATOR = ";";

    /**
     * Produce short strings for the address in the usual address format.
     **
     * @param ipAddressStr
     *        IP address string
     * @return A compressed IP address string
     */
    public static String toCompressedFormatByString(String ipAddressStr) {
        IPAddress ipAddress = toIpAddressByString(ipAddressStr);
        if (ipAddress == null) {
            return null;
        }

        return ipAddress.toCompressedString();
    }

    /**
     * This produces a canonical string.
     * <p>
     * RFC 5952 describes canonical representations for Ipv6
     * http://en.wikipedia.org/wiki/IPv6_address#Recommended_representation_as_text
     * http://tools.ietf.org/html/rfc5952
     * <p>
     * Each address has a unique canonical string, not counting the prefix.
     * The prefix can cause two equal addresses to have different strings.
     * @param ipAddressStr
     *        IP address string
     * @return A canonical IP address string
     */
    public static String toCanonicalFormatByString(String ipAddressStr) {
        IPAddress ipAddress = toIpAddressByString(ipAddressStr);
        if (ipAddress == null) {
            return null;
        }

        return ipAddress.toCanonicalString();
    }

    /**
     * This produces a IPAddress object.
     * @param ipAddressStr
     *          IP address string
     */
    public static IPAddress toIpAddressByString(String ipAddressStr) {
        if (ipAddressStr == null) {
            return null;
        }

        return new IPAddressString(ipAddressStr).getAddress();
    }

    /**
     * This produces a IPAddress object by byte array.
     *
     * @param ipAddressByteArray:IP address byte array
     * @return IPAddress address
     */
    public static IPAddress toIpAddressByByteArray(byte[] ipAddressByteArray) {
        if (ipAddressByteArray == null) {
            return null;
        }

        IPAddressNetwork.IPAddressGenerator generator = new IPAddressNetwork.IPAddressGenerator();

        return generator.from(ipAddressByteArray);
    }

    /**
     * This produces a canonical string by byte array.
     *
     * @param ipAddressByteArray:IP address byte array
     * @return IPAddress address string
     */
    public static String toCanonicalIPAddressStrByByteArray(byte[] ipAddressByteArray) {
        IPAddress ipAddress = toIpAddressByByteArray(ipAddressByteArray);
        if (ipAddress == null) {
            return null;
        }

        return ipAddress.toCanonicalString();
    }

    /**
     * This produces a compressed string by byte array.
     *
     * @param ipAddressByteArray:IP address byte array
     * @return IPAddress address
     */
    public static String toCompressedIPAddressStrByByteArray(byte[] ipAddressByteArray) {
        IPAddress ipAddress = toIpAddressByByteArray(ipAddressByteArray);
        if (ipAddress == null) {
            return null;
        }

        return ipAddress.toCompressedString();
    }

    /**
     * This produces a byte array by IP address string.
     *
     * @param ipAddressStr:IP address string
     * @return IPAddress address byte array
     */
    public static byte[] toByteArrayByIPAddressStr(String ipAddressStr) {
        IPAddress ipAddress = toIpAddressByString(ipAddressStr);
        if (ipAddress == null) {
            return null;
        }

        return ipAddress.getBytes();
    }

    /**
     * This produces a BigInteger object by IP address string.
     *
     * @param ipAddressStr:IP address
     * @return Address BigInteger
     */
    public static BigInteger toIpAddrBigIntegerByString(String ipAddressStr) {
        IPAddress ipAddress = toIpAddressByString(ipAddressStr);
        if (ipAddress == null) {
            return BigInteger.ZERO;
        }

        return ((ipAddress.getValue() == null) ? BigInteger.ZERO : ipAddress.getValue());
    }

    /**
     * This produces a IPAddress object by IP version and BigInteger.
     *
     * @param ipAddressBigInteger:IP address
     * @param ipVersion:IP           version
     * @return IPAddress object
     */
    public static IPAddress toIpAddressByBigInteger(BigInteger ipAddressBigInteger, Integer ipVersion) {
        if ((ipAddressBigInteger == null) || (ipVersion == null)) {
            return null;
        }

        IPAddress ipAddress;
        if (ipVersion == IP_VERSION_4) {
            ipAddress = new IPv4Address(ipAddressBigInteger.intValue());
        } else if (ipVersion == IP_VERSION_6){
            ipAddress = new IPv6Address(ipAddressBigInteger);
        } else {
            ipAddress = null;
        }

        return ipAddress;
    }

    /**
     * This produces an IP address string by IP version and BigInteger.
     *
     * @param ipAddressBigInteger:IP address
     * @param ipVersion:IP           version
     * @return Address string
     */
    public static String toStringByBigInteger(BigInteger ipAddressBigInteger, Integer ipVersion) {
        IPAddress ipAddress = toIpAddressByBigInteger(ipAddressBigInteger, ipVersion);
        if (ipAddress == null) {
            return null;
        }

        return ipAddress.toCanonicalString();
    }

    /** Get compressed IP address string with seperator(see IP_SEPERATOR) by array
     *
     * @param ipAddressStringArray
     *            IP address string array
     * @return Single string with seperator(IP_SEPERATOR)
     */
    public static String[] toCompressedIPAddressArray(String[] ipAddressStringArray) {
        if ((ipAddressStringArray == null) || (ipAddressStringArray.length == 0)) {
            return null;
        }

        String[] compressedIPAddressStringArray = new String[ipAddressStringArray.length];
        for (int i = 0; i < ipAddressStringArray.length; i++) {
            compressedIPAddressStringArray[i] =  toCompressedFormatByString(ipAddressStringArray[i]);
        }

        return compressedIPAddressStringArray;
    }

    /** Get IP address string with seperator(see IP_SEPERATOR) by array
     *
     * @param ipAddressStringArray
     *            IP address string array
     * @return Single string with seperator(IP_SEPERATOR)
     */
    public static String toIPAddressStringByArray(String[] ipAddressStringArray) {
        return FormatUtil.getStringWithSepByArray(ipAddressStringArray, IP_SEPERATOR);
    }

    /** Get compressed IP address string with seperator(see IP_SEPERATOR) by array
     *
     * @param ipAddressStringArray
     *            IP address string array
     * @return Single string with seperator(IP_SEPERATOR)
     */
    public static String toCompressedIPAddressStringByArray(String[] ipAddressStringArray) {
        if ((ipAddressStringArray == null) || (ipAddressStringArray.length == 0)) {
            return null;
        }

        String[] compressedIPAddressStringArray = toCompressedIPAddressArray(ipAddressStringArray);
        if (compressedIPAddressStringArray == null) {
            return null;
        }

        return FormatUtil.getStringWithSepByArray(compressedIPAddressStringArray, IP_SEPERATOR);
    }
}
