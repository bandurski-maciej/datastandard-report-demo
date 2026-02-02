package com.stibo.demo.report.service;

import com.stibo.demo.report.model.Attribute;
import com.stibo.demo.report.model.AttributeLink;
import com.stibo.demo.report.model.Category;
import com.stibo.demo.report.model.Datastandard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportGeneratorTest {

    @InjectMocks
    private ReportGenerator generator;

    @Mock
    private AttributeFormatter attributeFormatter;

    @Mock
    private DataStandardLookupService lookupService;

    @Mock
    private Datastandard dataStandard;

    @Test
    @DisplayName("Should always start with the correct header row")
    void testGenerate_HeadersOnly() {
        // Headers test doesn't use group lookup, so we stub it leniently
        lenient().when(lookupService.getAttributeGroupLookup(dataStandard)).thenReturn(Collections.emptyMap());

        Stream<Stream<String>> resultStream = generator.generate(Collections.emptyList(), Collections.emptyMap(), dataStandard);
        List<List<String>> rows = collectRows(resultStream);

        assertEquals(1, rows.size());
        assertEquals(List.of("Category Name", "Attribute Name", "Description", "Type", "Group"), rows.get(0));
    }

    @Test
    @DisplayName("Should deduplicate attributes")
    void testGenerate_Deduplication() {
        Category parent = createCategory("ParentCat");
        Category child = createCategory("ChildCat");
        List<Category> hierarchy = List.of(parent, child);

        Attribute attr1 = createAttribute();
        Map<String, Attribute> attributeLookup = Map.of("attr1", attr1);

        when(lookupService.getAttributeGroupLookup(dataStandard)).thenReturn(Collections.emptyMap());
        when(lookupService.getAttributeGroupNames(any(), any())).thenReturn(Collections.emptyList());

        when(attributeFormatter.formatAttributeName(any(), eq(attr1))).thenReturn("Color");
        when(attributeFormatter.formatAttributeDescription(attr1)).thenReturn("Desc");
        when(attributeFormatter.formatAttributeTypeDescription(any(), eq(attr1))).thenReturn("String");

        List<List<String>> rows = collectRows(generator.generate(hierarchy, attributeLookup, dataStandard));

        assertEquals(2, rows.size(), "Should have Header + 1 Data row due to deduplication");
    }

    // --- Helpers with Lenient Stubbing ---

    private Category createCategory(String name) {
        Category c = mock(Category.class);
        AttributeLink link = mock(AttributeLink.class);

        // Use lenient() here because some tests might not iterate deep enough to call these
        lenient().when(c.name()).thenReturn(name);
        lenient().when(link.id()).thenReturn("attr1");
        lenient().when(c.attributeLinks()).thenReturn(List.of(link));

        return c;
    }

    private Attribute createAttribute() {
        Attribute a = mock(Attribute.class);
        lenient().when(a.id()).thenReturn("attr1");
        lenient().when(a.name()).thenReturn("Color");
        lenient().when(a.groupIds()).thenReturn(Collections.emptyList());
        return a;
    }

    private List<List<String>> collectRows(Stream<Stream<String>> stream) {
        return stream.map(s -> s.collect(Collectors.toList()))
                .collect(Collectors.toList());
    }
}
