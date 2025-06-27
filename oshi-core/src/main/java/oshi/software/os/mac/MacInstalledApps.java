/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import oshi.software.os.ApplicationInfo;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

public final class MacInstalledApps {

    private static final Logger LOG = LoggerFactory.getLogger(MacInstalledApps.class);

    private static final DocumentBuilder DOCUMENT_BUILDER;

    static {
        DocumentBuilder documentoBuilderTmp = null;
        try {
            documentoBuilderTmp = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            documentoBuilderTmp.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) throws SAXException {
                    // ignore
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    // ignore
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    // ignore
                }
            });
        } catch (ParserConfigurationException e) {
            LOG.trace("Cannot instanciate DocumentBuilder: " + e.getMessage(), e);
        }
        DOCUMENT_BUILDER = documentoBuilderTmp;
    }

    private static final XPath XPATH = XPathFactory.newInstance().newXPath();

    private MacInstalledApps() {
    }

    public static List<ApplicationInfo> queryInstalledApps() {
        if (DOCUMENT_BUILDER != null) {
            List<String> output = ExecutingCommand.runNative("system_profiler -xml SPApplicationsDataType");

            try {
                List<LinkedHashMap<String, String>> plistValues = parsePlist(output);

                if (!plistValues.isEmpty()) {
                    Set<ApplicationInfo> appInfoSet = new LinkedHashSet<>();

                    for (LinkedHashMap<String, String> dictValues : plistValues) {
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
                                    Document appPlistDoc = DOCUMENT_BUILDER.parse(appPlistFile);
                                    String getInfoString = getPlistValue(appPlistDoc, "CFBundleGetInfoString");
                                    if (getInfoString != null && !getInfoString.isEmpty()) {
                                        additionalInfo.put("Get Info String", getInfoString);
                                    }
                                    if (version == null || version.isEmpty()) {
                                        version = getPlistValue(appPlistDoc, "CFBundleVersion");
                                    }
                                }
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
        }
        return Collections.emptyList();
    }

    private static List<LinkedHashMap<String, String>> parsePlist(List<String> input) throws Exception {
        Document doc = DOCUMENT_BUILDER.parse(new ByteArrayInputStream(String.join("", input).getBytes("UTF-8")));
        doc.getDocumentElement().normalize();

        List<LinkedHashMap<String, String>> result = new ArrayList<>();

        NodeList arrays = (NodeList) XPATH.evaluate("//key[text() = '_items']/following-sibling::array[1]", doc, XPathConstants.NODESET);

        if (arrays != null && arrays.getLength() > 0) {
            NodeList dicts = ((Element) arrays.item(0)).getElementsByTagName("dict");
            for (int i = 0; i < dicts.getLength(); i++) {
                Element dictElem = (Element) dicts.item(i);
                result.add(parseDictElement(dictElem));
            }
        }
        return result;
    }

    private static LinkedHashMap<String, String> parseDictElement(Element dictElem) throws Exception {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        NodeList children = dictElem.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element el = (Element) node;
            if (!"key".equals(el.getTagName())) {
                continue;
            }

            String key = el.getTextContent();
            Node sibling = el.getNextSibling();
            while (sibling != null && sibling.getNodeType() != Node.ELEMENT_NODE) {
                sibling = sibling.getNextSibling();
            }
            if (sibling == null) {
                continue;
            }

            Element valueElem = (Element) sibling;
            String value = parseValueElement(valueElem);
            map.put(key, value);
        }
        return map;
    }

    private static String parseValueElement(Element valueElem) throws Exception {
        String tag = valueElem.getTagName();
        switch (tag) {
            case "true":
                return "true";
            case "false":
                return "false";
            case "dict":
                LOG.trace("Dictionary type aren't supported.");
                return "<dict>...</dict>";
            case "array":
                NodeList items = valueElem.getChildNodes();
                for (int j = 0; j < items.getLength(); j++) {
                    Node item = items.item(j);
                    if (item.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    // We only needs first element of signed_by
                    return parseValueElement((Element) item);
                }
                return "";
            case "string":
            case "integer":
            case "real":
            case "date":
            default:
                return valueElem.getTextContent();
        }
    }

    private static String getPlistValue(Document doc, String keyName) throws Exception {
        return XPATH.evaluate("/plist/dict/key[normalize-space(.)='" + keyName + "']"
                + "/following-sibling::*[name()='string'][1]/text()", doc).trim();
    }
}
