package com.stibo.demo.report.service;

import com.stibo.demo.report.logging.LogTime;
import com.stibo.demo.report.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Stream;

/**
 * Service responsible for orchestrating the generation of data standard reports.
 * <p>
 * This service acts as the entry point for report generation. It coordinates
 * the retrieval of category hierarchies and attribute definitions using the
 * {@link DataStandardLookupService}, then delegates the structural assembly
 * of the report rows to the {@link ReportGenerator}.
 * </p>
 * @see DataStandardLookupService
 * @see ReportGenerator
 */
@Service
public class ReportService {

    private final DataStandardLookupService lookupService;
    private final ReportGenerator generator;

    /**
     * Constructs a new {@code ReportService} with required dependencies.
     *
     * @param lookupService the service used to resolve category hierarchies and attribute maps
     * @param generator     the component responsible for generating the final row-based report structure
     */
    public ReportService(DataStandardLookupService lookupService, ReportGenerator generator) {
        this.lookupService = lookupService;
        this.generator = generator;
    }

    /**
     * Generates a report for a specific category within a data standard.
     * <p>
     * This method resolves the full parent-to-child path for the given category ID
     * and maps all available attributes before generating a stream-based
     * representation of the report. The timing of this operation is recorded via
     * the {@link LogTime} annotation.
     * </p>
     *
     * @param dataStandard the complete {@link Datastandard} model containing categories and attributes
     * @param categoryId   the unique identifier of the category to serve as the report's starting point
     * @return a {@link Stream} of rows, where each row is a {@link Stream} of column values (strings)
     */
    @LogTime
    public Stream<Stream<String>> report(Datastandard dataStandard, String categoryId) {
        List<Category> hierarchy = lookupService.getCategoryHierarchy(dataStandard, categoryId);
        Map<String, Attribute> attributeMap = lookupService.getAttributeLookup(dataStandard);

        return generator.generate(hierarchy, attributeMap, dataStandard);
    }
}
