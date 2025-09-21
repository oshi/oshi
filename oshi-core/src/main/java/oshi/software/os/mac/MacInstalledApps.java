/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.software.os.ApplicationInfo;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

public final class MacInstalledApps {

    private static final Logger LOG = LoggerFactory.getLogger(MacInstalledApps.class);

    private MacInstalledApps() {
    }
    
    public static List<ApplicationInfo> queryInstalledApps() {
        List<String> output = ExecutingCommand.runNative("system_profiler -xml SPApplicationsDataType");

        try {
            List<Map<String, String>> plistValues = parseItems(String.join("", output));

            if (!plistValues.isEmpty()) {
                Set<ApplicationInfo> appInfoSet = new LinkedHashSet<>();

                for (Map<String, String> dictValues : plistValues) {
                    try {
                        String obtainedFrom = ParseUtil.getStringValueOrUnknown(dictValues.get("obtained_from"));
                        if ("apple".equals(obtainedFrom)) {
                            obtainedFrom = "Apple";
                        } else if ("mac_app_store".equals(obtainedFrom)) {
                            obtainedFrom = "App Store";
                        }
                        String signedBy = ParseUtil.getStringValueOrUnknown(dictValues.get("signed_by"));
                        String vendor;
                        if ("identified_developer".equals(obtainedFrom)) {
                            if (signedBy.startsWith("Developer ID Application: ")) {
                                vendor = signedBy.substring(26);
                            } else {
                                vendor = signedBy;
                            }
                        } else if (Constants.UNKNOWN.equals(obtainedFrom) && !Constants.UNKNOWN.equals(signedBy)) {
                            vendor = signedBy;
                        } else {
                            vendor = obtainedFrom;
                        }

                        String version = dictValues.get("version");

                        String lastModified = dictValues.get("lastModified");
                        long lastModifiedEpoch = ParseUtil.parseDateToEpoch(lastModified, "yyyy-MM-dd'T'HH:mm:ss'Z'");

                        Map<String, String> additionalInfo = new LinkedHashMap<>();
                        additionalInfo.put("Kind", ParseUtil.getStringValueOrUnknown(dictValues.get("arch_kind")));
                        String location = ParseUtil.getStringValueOrUnknown(dictValues.get("path"));
                        additionalInfo.put("Location", location);
                        if (!Constants.UNKNOWN.equals(location)) {
                            File appPlistFile = new File(location, "/Contents/Info.plist");
                            if (appPlistFile.exists()) {
                                if ("bplist00".equals(new String(readFirstBytes(appPlistFile), StandardCharsets.UTF_8))) {
                                    // convert binary plist to xml
                                    output = ExecutingCommand.runNative(new String[]{"plutil", "-convert", "xml1", "-o", "-", appPlistFile.getAbsolutePath()});
                                } else {
                                    output = FileUtil.readFile(appPlistFile.getAbsolutePath());
                                }
                                String xml = String.join("", output);
                                String getInfoString = readStringValue(xml, "CFBundleGetInfoString");
                                if (getInfoString != null && !getInfoString.isEmpty()) {
                                    additionalInfo.put("Get Info String", getInfoString);
                                }
                                if (version == null || version.isEmpty()) {
                                    version = readStringValue(xml, "CFBundleVersion");
                                }
                            }
                        }

                        if (version == null || version.isEmpty()) {
                            version = Constants.UNKNOWN;
                        }

                        appInfoSet.add(new ApplicationInfo(dictValues.get("_name"), version, vendor, lastModifiedEpoch, additionalInfo));
                    } catch (Exception e) {
                        LOG.trace("Unable to parse dict values: " + e.getMessage() + " - " + dictValues, e);
                    }
                }

                return new ArrayList<>(appInfoSet);
            }
        } catch (Exception e) {
            LOG.trace("Unable to read installed apps: " + e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    private static byte[] readFirstBytes(File file) throws IOException {
        byte[] buffer = new byte[8];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(buffer);
            return buffer;
        }
    }

    private static List<Map<String, String>> parseItems(String xml) {
        if (xml == null) {
            return Collections.emptyList();
        }

        Matcher m = Pattern.compile("<key>\\s*_items\\s*</key>").matcher(xml);
        if (!m.find()) {
            return Collections.emptyList();
        }

        int arrayOpen = xml.indexOf("<array>", m.end());
        if (arrayOpen < 0) {
            return Collections.emptyList();
        }
        
        String arrayBody = extractBalancedInner(xml, arrayOpen, "<array>", "</array>");
        if (arrayBody == null) {
            return Collections.emptyList();
        }

        List<String> dictBlocks = extractTopLevelBlocks(arrayBody, "<dict>", "</dict>");
        List<Map<String, String>> out = new ArrayList<>();
        for (String dictInner : dictBlocks) {
            out.add(parseDict(dictInner));
        }
        return out;
    }


    private static Map<String, String> parseDict(String dictInner) {
        Map<String, String> map = new LinkedHashMap<>();
        int pos = 0;
        while (true) {
            int kStart = dictInner.indexOf("<key>", pos);
            if (kStart < 0) {
                break;
            }
            int kEnd = dictInner.indexOf("</key>", kStart + 5);
            if (kEnd < 0) {
                break;
            }

            String key = unescape(dictInner.substring(kStart + 5, kEnd).trim());
            int vOpen = dictInner.indexOf('<', kEnd + 6);
            if (vOpen < 0) {
                break;
            }

            String val;
            if (startsWith(dictInner, vOpen, "<string>")) {
                String inner = extractSimpleInner(dictInner, vOpen, "<string>", "</string>");
                val = unescape(inner);
                pos = dictInner.indexOf("</string>", vOpen) + "</string>".length();
            } else if (startsWith(dictInner, vOpen, "<date>")) {
                String inner = extractSimpleInner(dictInner, vOpen, "<date>", "</date>");
                val = inner.trim();
                pos = dictInner.indexOf("</date>", vOpen) + "</date>".length();
            } else if (startsWith(dictInner, vOpen, "<array>")) {
                String inner = extractBalancedInner(dictInner, vOpen, "<array>", "</array>");
                val = parseStringArray(inner);
                pos = dictInner.indexOf("</array>", vOpen) + "</array>".length();
            } else {
                // other irrelevant tags: go to next tag
                pos = vOpen + 1;
                continue;
            }

            map.put(key, val);
        }
        return map;
    }

    private static String parseStringArray(String arrayInner) {
        int lt = arrayInner.indexOf('<');
        if (lt >= 0) {
            if (startsWith(arrayInner, lt, "<string>")) {
                String inner = extractSimpleInner(arrayInner, lt, "<string>", "</string>");
                return unescape(inner);
            }
        }
        return null;
    }

    private static boolean startsWith(String s, int pos, String tag) {
        int end = pos + tag.length();
        return end <= s.length() && s.regionMatches(false, pos, tag, 0, tag.length());
    }

    private static String extractSimpleInner(String s, int openPos, String openTag, String closeTag) {
        int start = s.indexOf(openTag, openPos);
        if (start < 0) {
            return "";
        }
        int end = s.indexOf(closeTag, start + openTag.length());
        if (end < 0) {
            return "";
        }
        return s.substring(start + openTag.length(), end);
    }

    private static String extractBalancedInner(String s, int openPos, String openTag, String closeTag) {
        int pos = openPos;
        if (!startsWith(s, pos, openTag)) {
            return null;
        }
        pos += openTag.length();
        int depth = 1;
        while (pos < s.length()) {
            int nextOpen = s.indexOf(openTag, pos);
            int nextClose = s.indexOf(closeTag, pos);
            if (nextClose == -1) {
                return null;
            }
            if (nextOpen != -1 && nextOpen < nextClose) {
                depth++;
                pos = nextOpen + openTag.length();
            } else {
                depth--;
                if (depth == 0) {
                    return s.substring(openPos + openTag.length(), nextClose);
                }
                pos = nextClose + closeTag.length();
            }
        }
        return null;
    }

    private static List<String> extractTopLevelBlocks(String containerInner, String openTag, String closeTag) {
        List<String> blocks = new ArrayList<>();
        int pos = 0;
        while (true) {
            int open = containerInner.indexOf(openTag, pos);
            if (open < 0) {
                break;
            }
            String inner = extractBalancedInner(containerInner, open, openTag, closeTag);
            if (inner == null) {
                break;
            }
            blocks.add(inner);
            int closeEnd = containerInner.indexOf(closeTag, open) + closeTag.length();
            pos = closeEnd;
        }
        return blocks;
    }

    private static String unescape(String s) {
        return s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'");
    }

    private static String readStringValue(String xml, String keyName) throws Exception {
        int i = xml.indexOf("<key>" + keyName + "</key>");
        if (i > 0) {
            i = xml.indexOf("<string>", i);
            if (i > 0) {
                i += 8; // lengh of "<string>"
                int e = xml.indexOf("</string>", i);
                if (e > 0) {
                    return xml.substring(i, e);
                }
            }
        }
        return null;
    }
}
