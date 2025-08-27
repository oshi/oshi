/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

public interface CoreFoundationHeaders {

    int kCFNotFound = -1;

    // String encodings
    int kCFStringEncodingASCII = 0x0600;
    int kCFStringEncodingUTF8 = 0x08000100;

    // CFNumber types
    int kCFNumberSInt8Type = 1;
    int kCFNumberSInt16Type = 2;
    int kCFNumberSInt32Type = 3;
    int kCFNumberSInt64Type = 4;
    int kCFNumberFloat32Type = 5;
    int kCFNumberFloat64Type = 6;
    int kCFNumberCharType = 7;
    int kCFNumberShortType = 8;
    int kCFNumberIntType = 9;
    int kCFNumberLongType = 10;
    int kCFNumberLongLongType = 11;
    int kCFNumberFloatType = 12;
    int kCFNumberDoubleType = 13;
    int kCFNumberCFIndexType = 14;
    int kCFNumberNSIntegerType = 15;
    int kCFNumberCGFloatType = 16;

    // CFDateFormatter styles
    int kCFDateFormatterNoStyle = 0;
    int kCFDateFormatterShortStyle = 1;
    int kCFDateFormatterMediumStyle = 2;
    int kCFDateFormatterLongStyle = 3;
    int kCFDateFormatterFullStyle = 4;
}
