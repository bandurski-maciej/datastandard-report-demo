package com.stibo.demo.report.service;

import com.stibo.demo.report.model.Attribute;
import com.stibo.demo.report.model.AttributeLink;
import com.stibo.demo.report.model.AttributeType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.stibo.demo.report.service.AttributeFormatterConstants.*;

/**
 * Component responsible for formatting attribute-related data into human-readable strings.
 * <p>
 * This class handles the conversion of attribute types, names, and markers into the
 * specific string representations required for the report, including recursive
 * processing for composite attribute structures.
 * </p>
 */
@Component
public class AttributeFormatter {

    /**
     * Formats the display name of an attribute, including optionality markers.
     * <p>
     * Attributes marked as mandatory in their {@link AttributeLink} are suffixed
     * with the {@code NOT_OPTIONAL_MARKER}.
     * </p>
     *
     * @param link      the link context defining if the attribute is optional
     * @param attribute the attribute being named
     * @return the formatted display name
     */
    public String formatAttributeName(AttributeLink link, Attribute attribute) {
        String name = attribute.name();
        return Boolean.TRUE.equals(link.optional()) ? name : name + NOT_OPTIONAL_MARKER;
    }

    /**
     * Formats the description of an attribute, ensuring a non-null result.
     *
     * @param attribute the attribute to describe
     * @return the attribute's description, or an empty string if null
     */
    public String formatAttributeDescription(Attribute attribute) {
        return attribute.description() != null ? attribute.description() : "";
    }

    /**
     * Generates a string representation of an attribute's type.
     * <p>
     * For base types, returns the type ID. For composite types, initiates a
     * recursive traversal of the type hierarchy.
     * </p>
     *
     * @param attributeLookup a map of available attributes for resolving child links
     * @param attribute       the attribute whose type is being described
     * @return a formatted string representing the attribute type and its structure
     */
    public String formatAttributeTypeDescription(Map<String, Attribute> attributeLookup, Attribute attribute) {
        if (!isCompositeType(attribute)) {
            return formatBaseType(attribute.type());
        }
        return describeTypeRecursively(attributeLookup, attribute, new StringBuilder(), 0).toString();
    }

    /**
     * Recursively builds a string representation for nested composite types.
     * <p>
     * Traverses through {@link AttributeLink}s to include child attributes and their types
     * within curly braces, maintaining the hierarchy of the composite structure.
     * </p>
     *
     * @param attributeLookup a map of available attributes
     * @param attribute       the composite attribute to describe
     * @param typeBuilder     the {@link StringBuilder} accumulating the description
     * @return the same {@link StringBuilder} instance containing the recursive description
     */
    private StringBuilder describeTypeRecursively(Map<String, Attribute> attributeLookup,
                                                  Attribute attribute,
                                                  StringBuilder typeBuilder,
                                                  int recursionLevel) {

        if (!isCompositeType(attribute)) return typeBuilder;

        buildParentTypeName(recursionLevel, attribute, typeBuilder);

        List<AttributeLink> links = attribute.attributeLinks();
        recursionLevel++;
        for (AttributeLink link : links) {
            Attribute child = attributeLookup.get(link.id());

            if (child == null) continue;

            if (isCompositeType(child)) {
                describeTypeRecursively(attributeLookup, child, typeBuilder, recursionLevel);
            } else {
                buildChildTypeName(typeBuilder, recursionLevel, link, child);
            }
            typeBuilder.append(LINE_SEPARATOR);
        }

        int parentLevel = recursionLevel - 1;
        typeBuilder.append(COMPOSITE_TYPE_INDENTATION.repeat(parentLevel))
                .append(COMPOSITE_TYPE_CLOSING_BRACE);
        appendMultivalueMarker(attribute.type(), typeBuilder);
        return typeBuilder;
    }

    /**
     * Checks if a given attribute type is a composite type.
     *
     * @param type the type to check
     * @return {@code true} if the type ID matches the composite identifier
     */
    private boolean isCompositeType(Attribute type) {
        return !type.attributeLinks().isEmpty();
    }

    private void buildParentTypeName(int recursionLevel, Attribute attribute, StringBuilder typeBuilder) {
        String indentation = COMPOSITE_TYPE_INDENTATION.repeat(recursionLevel);
        String identifier = (recursionLevel == 0)
                ? attribute.type().id()
                : "%s: %s".formatted(attribute.name(), attribute.type().id());

        typeBuilder.append("%s%s%s%n".formatted(indentation, identifier, COMPOSITE_TYPE_OPENING_BRACE));
    }

    private void buildChildTypeName(StringBuilder typeBuilder, int recursionLevel, AttributeLink link, Attribute child) {
        typeBuilder.append(COMPOSITE_TYPE_INDENTATION.repeat(recursionLevel))
                .append(formatAttributeName(link, child))
                .append(TYPE_MAPPING_SEPARATOR)
                .append(formatBaseType(child.type()));
    }

    /**
     * Appends a multivalue marker to the provided builder if the type allows multiple values.
     *
     * @param type the attribute type to check
     * @param sb   the builder to append the marker to
     */
    private void appendMultivalueMarker(AttributeType type, StringBuilder sb) {
        if (Boolean.TRUE.equals(type.multiValue())) {
            sb.append(MULTIVALUE_MARKER);
        }
    }

    /**
     * Formats a non-composite attribute type ID with relevant markers.
     *
     * @param type the base attribute type
     * @return the formatted base type string
     */
    private String formatBaseType(AttributeType type) {
        StringBuilder sb = new StringBuilder(type.id());
        appendMultivalueMarker(type, sb);
        return sb.toString();
    }

}
