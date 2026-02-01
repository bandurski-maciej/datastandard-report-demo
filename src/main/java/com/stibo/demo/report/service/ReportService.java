package com.stibo.demo.report.service;

import com.stibo.demo.report.logging.LogTime;
import com.stibo.demo.report.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service responsible for generating a hierarchical report of attributes
 * within a specific data standard category.
 */
@Service
public class ReportService {

    private static final List<String> HEADERS = List.of("Category Name", "Attribute Name", "Description", "Type", "Group");
    private static final String NOT_OPTIONAL_MARKER = "*";
    private static final String MULTIVALUE_MARKER = "[]";
    private static final String LINE_SEPARATOR = ","; // expected new line, needs further investigation as System.lineSeparator() doesn't seem to work
    private static final String COMPOSITE_TYPE_ID = "composite";
    private static final String TYPE_MAPPING_SEPARATOR = ":";

    /**
     * Generates a stream of report rows for a given category, including its parent hierarchy.
     *
     * @param dataStandard The full data standard model.
     * @param categoryId   The starting category ID for the report.
     * @return A Stream of rows, where each row is a Stream of column values.
     */
    @LogTime
    public Stream<Stream<String>> report(Datastandard dataStandard, String categoryId) {
        List<Category> categoryHierarchy = getCategoryHierarchy(dataStandard, categoryId);
        Map<String, Attribute> attributeMap = getAttributeMap(dataStandard);

        return generateReportRows(categoryHierarchy, attributeMap, dataStandard);
    }

    private List<Category> getCategoryHierarchy(Datastandard dataStandard, String categoryId) {
        Map<String, Category> categoryMap = dataStandard.categories().stream()
                .collect(Collectors.toMap(Category::id, Function.identity()));

        List<Category> path = new ArrayList<>();
        Category current = categoryMap.get(categoryId);

        while (current != null) {
            path.add(current);
            current = categoryMap.get(current.parentId());
        }

        Collections.reverse(path);
        return path;
    }

    private Map<String, Attribute> getAttributeMap(Datastandard dataStandard) {
        return dataStandard.attributes().stream()
                .collect(Collectors.toMap(Attribute::id, Function.identity()));
    }

    private static List<String> getAttributeGroupNames(List<String> groupIds, Datastandard dataStandard) {
        return dataStandard.attributeGroups().stream()
                .filter(group -> groupIds.contains(group.id()))
                .map(AttributeGroup::name)
                .toList();
    }

    private static Stream<Stream<String>> generateReportRows(List<Category> categories,
                                                             Map<String, Attribute> attributeLookup,
                                                             Datastandard dataStandard) {
        List<Stream<String>> rows = new ArrayList<>();
        Set<String> processedAttributeIds = new HashSet<>();

        rows.add(HEADERS.stream());

        for (Category category : categories) {
            for (AttributeLink link : category.attributeLinks()) {

                // If an attribute was already listed in a parent category, skip it.
                if (!processedAttributeIds.add(link.id())) {
                    continue;
                }

                Attribute attribute = attributeLookup.get(link.id());
                if (attribute == null) continue;

                String groups = String.join(LINE_SEPARATOR, getAttributeGroupNames(attribute.groupIds(), dataStandard));
                String typeDescription = getTypeDescription(attributeLookup, attribute);

                rows.add(Stream.of(
                        category.name(),
                        formatAttributeName(link, attribute),
                        attribute.description(),
                        typeDescription,
                        groups));
            }
        }

        return rows.stream();
    }

    private static String getTypeDescription(Map<String, Attribute> attributeLookup, Attribute attribute) {
        if (!COMPOSITE_TYPE_ID.equals(attribute.type().id())) {
            return formatBaseType(attribute.type());
        }

        return describeTypeRecursively(attributeLookup, attribute, new StringBuilder()).toString();
    }

    /**
     * Recursively builds a string representation for composite types.
     */
    private static StringBuilder describeTypeRecursively(Map<String, Attribute> attributeLookup,
                                                         Attribute attribute,
                                                         StringBuilder typeBuilder) {
        if (!COMPOSITE_TYPE_ID.equals(attribute.type().id())) {
            return typeBuilder;
        }

        typeBuilder.append(attribute.name()).append("{  ");

        List<AttributeLink> links = attribute.attributeLinks();
        for (int i = 0; i < links.size(); i++) {
            AttributeLink link = links.get(i);
            Attribute childAttribute = attributeLookup.get(link.id());

            if (childAttribute == null) continue;

            boolean isChildComposite = COMPOSITE_TYPE_ID.equals(childAttribute.type().id());

            if (isChildComposite) {
                describeTypeRecursively(attributeLookup, childAttribute, typeBuilder);
            } else {
                typeBuilder.append(formatAttributeName(link, childAttribute))
                        .append(TYPE_MAPPING_SEPARATOR)
                        .append(formatBaseType(childAttribute.type()));
            }

            if (i < links.size() - 1) {
                typeBuilder.append(LINE_SEPARATOR);
            }
        }

        return typeBuilder.append("}");
    }

    private static String formatAttributeName(AttributeLink link, Attribute attribute) {
        String name = attribute.name();
        return Boolean.TRUE.equals(link.optional()) ? name : name + NOT_OPTIONAL_MARKER;
    }

    private static String formatBaseType(AttributeType type) {
        return Boolean.TRUE.equals(type.multiValue()) ? type.id() + MULTIVALUE_MARKER : type.id();
    }
}