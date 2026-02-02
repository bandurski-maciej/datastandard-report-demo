package com.stibo.demo.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stibo.demo.report.model.Datastandard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.stibo.demo.report.service.AttributeFormatterConstants.*;
import static java.util.stream.Collectors.toList;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        ReportService.class,
        ObjectMapper.class,
        ReportGenerator.class,
        DataStandardLookupService.class,
        AttributeFormatter.class})
public class ReportServiceTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReportService reportService;

    private Datastandard datastandard;

    @BeforeEach
    public void before() throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("datastandard.json")) {
            this.datastandard = objectMapper.readValue(stream, Datastandard.class);
        }

    }

    @Test
    @DisplayName("Should generate the full report for the leaf category")
    public void testLeafCategoryReport() {
        List<List<String>> report = generateReport("leaf");

        assertThat(report.get(0)).containsExactly("Category Name", "Attribute Name", "Description", "Type", "Group");

        assertThat(report.get(1)).containsExactly(
                "Root",
                "String Value*",
                "",
                "string",
                "All"
        );

        assertThat(report.get(2)).containsExactly(
                "Leaf",
                "Composite Value",
                "Composite Value Description",
                "Composite Value" + COMPOSITE_TYPE_OPENING_BRACE + LINE_SEPARATOR + "  Nested Value*:integer" + LINE_SEPARATOR + COMPOSITE_TYPE_CLOSING_BRACE + "[]",
                "All" + LINE_SEPARATOR + "Complex"
        );
    }

    @Test
    @DisplayName("Should only show root attributes when reporting from root")
    public void testRootCategoryReport() {
        List<List<String>> report = generateReport("root");

        assertThat(report).hasSize(2); // Headers + 1 attribute
        assertThat(report.get(1).get(0)).isEqualTo("Root");
    }

    private List<List<String>> generateReport(String categoryId) {
        return reportService.report(datastandard, categoryId)
                .map(rowStream -> rowStream.collect(toList()))
                .collect(toList());
    }
}

