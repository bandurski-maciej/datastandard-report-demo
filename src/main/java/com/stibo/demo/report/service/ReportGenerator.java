package com.stibo.demo.report.service;

import com.stibo.demo.report.model.Attribute;
import com.stibo.demo.report.model.Category;
import com.stibo.demo.report.model.Datastandard;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

import static com.stibo.demo.report.service.AttributeFormatterConstants.HEADERS;
import static com.stibo.demo.report.service.AttributeFormatterConstants.LINE_SEPARATOR;

/**
 * Component responsible for the structural assembly of report rows based on category hierarchies.
 * <p>
 * This class handles the iteration over categories and their linked attributes, ensuring that
 * attributes are deduplicated across the hierarchy. It utilizes {@link AttributeFormatter}
 * for string representations and {@link DataStandardLookupService} for group name resolution.
 * </p>
 */
@Component
public class ReportGenerator {

    private final AttributeFormatter attributeFormatter;
    private final DataStandardLookupService lookupService;

    /**
     * Constructs a {@code ReportGenerator} with the necessary formatting and lookup dependencies.
     *
     * @param attributeFormatter the component used to format attribute names and type descriptions
     * @param lookupService      the service used to resolve attribute group names
     */
    public ReportGenerator(AttributeFormatter attributeFormatter, DataStandardLookupService lookupService) {
        this.attributeFormatter = attributeFormatter;
        this.lookupService = lookupService;
    }

    /**
     * Generates a stream of report rows for the provided category hierarchy.
     * <p>
     * The method produces a header row followed by data rows. It implements a deduplication
     * strategy where each unique attribute ID is only included in the report once, specifically
     * at its first occurrence in the category hierarchy traversal.
     * </p>
     *
     * @param categories      the ordered list of categories to include in the report
     * @param attributeLookup a map of available attributes indexed by their ID
     * @param dataStandard    the source data standard used to resolve attribute groups
     * @return a {@link Stream} of rows, where each row is a {@link Stream} of column values
     */
    public Stream<Stream<String>> generate(List<Category> categories,
                                           Map<String, Attribute> attributeLookup,
                                           Datastandard dataStandard) {

        Map<String, String> groupLookup = lookupService.getAttributeGroupLookup(dataStandard);
        Set<String> processedAttributeIds = new HashSet<>();

        Stream<Stream<String>> headerStream = Stream.of(HEADERS.stream());
        Stream<Stream<String>> dataStream = categories.stream()
                .flatMap(category -> category.attributeLinks().stream()
                        .filter(link -> processedAttributeIds.add(link.id()))
                        .map(link -> {
                            Attribute attribute = attributeLookup.get(link.id());
                            if (attribute == null) return null;

                            return Stream.of(
                                    category.name(),
                                    attributeFormatter.formatAttributeName(link, attribute),
                                    attributeFormatter.formatAttributeDescription(attribute),
                                    attributeFormatter.formatAttributeTypeDescription(attributeLookup, attribute),
                                    String.join(LINE_SEPARATOR, lookupService.getAttributeGroupNames(groupLookup, attribute.groupIds()))
                            );
                        }))
                .filter(Objects::nonNull);

        return Stream.concat(headerStream, dataStream);
    }

}
