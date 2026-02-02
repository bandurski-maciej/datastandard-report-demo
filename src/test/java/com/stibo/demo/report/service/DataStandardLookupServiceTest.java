package com.stibo.demo.report.service;

import com.stibo.demo.report.model.Attribute;
import com.stibo.demo.report.model.AttributeGroup;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataStandardLookupServiceTest {

    @InjectMocks
    private DataStandardLookupService service;

    @Mock
    private Datastandard mockDataStandard;

    // --- 1. Category Hierarchy Tests ---

    @Test
    @DisplayName("Should return empty list if starting category is not found")
    void testGetCategoryHierarchy_NotFound() {
        // Setup empty categories
        when(mockDataStandard.categories()).thenReturn(Collections.emptyList());

        List<Category> result = service.getCategoryHierarchy(mockDataStandard, "unknown_id");

        assertTrue(result.isEmpty(), "Result should be empty when category ID is missing");
    }

    @Test
    @DisplayName("Should return single category if it has no parent (Root)")
    void testGetCategoryHierarchy_RootOnly() {
        Category root = createCategory("root", null);
        when(mockDataStandard.categories()).thenReturn(List.of(root));

        List<Category> result = service.getCategoryHierarchy(mockDataStandard, "root");

        assertEquals(1, result.size());
        assertEquals("root", result.get(0).id());
    }

    @Test
    @DisplayName("Should return correct hierarchy path from Child to Root (Parent -> Child)")
    void testGetCategoryHierarchy_DeepStructure() {
        // Structure: Root -> Level1 -> Level2 (Target)
        Category root = createCategory("root", null);
        Category level1 = createCategory("level1", "root");
        Category level2 = createCategory("level2", "level1");

        when(mockDataStandard.categories()).thenReturn(List.of(root, level2, level1)); // Random order in list

        List<Category> result = service.getCategoryHierarchy(mockDataStandard, "level2");

        // Verify size and order (Parent first)
        assertEquals(3, result.size());
        assertEquals("root", result.get(0).id());
        assertEquals("level1", result.get(1).id());
        assertEquals("level2", result.get(2).id());
    }

    @Test
    @DisplayName("Should handle broken chain gracefully (stop at missing parent)")
    void testGetCategoryHierarchy_BrokenChain() {
        // Structure: (Missing Root) -> Level1 -> Target
        Category level1 = createCategory("level1", "missing_root");
        Category target = createCategory("target", "level1");

        when(mockDataStandard.categories()).thenReturn(List.of(level1, target));

        List<Category> result = service.getCategoryHierarchy(mockDataStandard, "target");

        // Should return [Level1, Target] and stop looking for 'missing_root'
        assertEquals(2, result.size());
        assertEquals("level1", result.get(0).id());
        assertEquals("target", result.get(1).id());
    }

    // --- 2. Attribute Lookup Tests ---

    @Test
    @DisplayName("Should index attributes by ID")
    void testGetAttributeLookup() {
        Attribute a1 = createAttribute("a1");
        Attribute a2 = createAttribute("a2");

        when(mockDataStandard.attributes()).thenReturn(List.of(a1, a2));

        Map<String, Attribute> result = service.getAttributeLookup(mockDataStandard);

        assertEquals(2, result.size());
        assertSame(a1, result.get("a1"));
        assertSame(a2, result.get("a2"));
    }

    @Test
    @DisplayName("Should return empty map if no attributes exist")
    void testGetAttributeLookup_Empty() {
        when(mockDataStandard.attributes()).thenReturn(Collections.emptyList());
        assertTrue(service.getAttributeLookup(mockDataStandard).isEmpty());
    }

    // --- 3. Attribute Group Lookup Tests ---

    @Test
    @DisplayName("Should index attribute group names by ID")
    void testGetAttributeGroupLookup() {
        AttributeGroup g1 = createGroup("g1", "Technical");
        AttributeGroup g2 = createGroup("g2", "Marketing");

        when(mockDataStandard.attributeGroups()).thenReturn(List.of(g1, g2));

        Map<String, String> result = service.getAttributeGroupLookup(mockDataStandard);

        assertEquals("Technical", result.get("g1"));
        assertEquals("Marketing", result.get("g2"));
    }

    // --- 4. Group Name Resolution Tests ---

    @Test
    @DisplayName("Should resolve list of IDs to Names")
    void testGetAttributeGroupNames_Success() {
        Map<String, String> lookup = Map.of(
                "g1", "Technical",
                "g2", "Marketing"
        );
        List<String> ids = List.of("g1", "g2");

        List<String> result = service.getAttributeGroupNames(lookup, ids);

        assertEquals(2, result.size());
        assertTrue(result.contains("Technical"));
        assertTrue(result.contains("Marketing"));
    }

    @Test
    @DisplayName("Should ignore IDs that are not in the lookup map")
    void testGetAttributeGroupNames_FilterMissing() {
        Map<String, String> lookup = Map.of("g1", "Technical");
        List<String> ids = List.of("g1", "missing_id");

        List<String> result = service.getAttributeGroupNames(lookup, ids);

        assertEquals(1, result.size());
        assertEquals("Technical", result.get(0));
    }

    @Test
    @DisplayName("Should handle empty ID list")
    void testGetAttributeGroupNames_EmptyInput() {
        Map<String, String> lookup = Map.of("g1", "Technical");
        List<String> result = service.getAttributeGroupNames(lookup, Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    // --- Helpers ---

    private Category createCategory(String id, String parentId) {
        Category c = mock(Category.class);
        when(c.id()).thenReturn(id);
        when(c.parentId()).thenReturn(parentId);
        return c;
    }

    private Attribute createAttribute(String id) {
        Attribute a = mock(Attribute.class);
        when(a.id()).thenReturn(id);
        return a;
    }

    private AttributeGroup createGroup(String id, String name) {
        AttributeGroup g = mock(AttributeGroup.class);
        when(g.id()).thenReturn(id);
        when(g.name()).thenReturn(name);
        return g;
    }
}
