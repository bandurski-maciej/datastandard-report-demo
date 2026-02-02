package com.stibo.demo.report.service;

import java.util.List;

public class AttributeFormatterConstants {

    // Only static constants - prevent instantiation
    private AttributeFormatterConstants() {
    }

    public static final List<String> HEADERS = List.of("Category Name", "Attribute Name", "Description", "Type", "Group");
    public static final String LINE_SEPARATOR = System.lineSeparator();
    public static final String NOT_OPTIONAL_MARKER = "*";
    public static final String MULTIVALUE_MARKER = "[]";
    public static final String TYPE_MAPPING_SEPARATOR = ":";
    public static final String COMPOSITE_TYPE_ID = "composite";
    public static final String COMPOSITE_TYPE_INDENTATION = "  ";
    public static final String COMPOSITE_TYPE_OPENING_BRACE = "{";
    public static final String COMPOSITE_TYPE_CLOSING_BRACE = "}";

}
