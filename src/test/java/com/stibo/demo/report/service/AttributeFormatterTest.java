package com.stibo.demo.report.service;

import com.stibo.demo.report.model.Attribute;
import com.stibo.demo.report.model.AttributeLink;
import com.stibo.demo.report.model.AttributeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.stibo.demo.report.service.AttributeFormatterConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttributeFormatterTest {

    @InjectMocks
    private AttributeFormatter formatter;

    @Mock
    private AttributeLink mockLink;
    @Mock
    private Attribute mockAttribute;
    @Mock
    private AttributeType mockType;

    // --- 1. Name Formatting Tests ---

    @Test
    @DisplayName("Should return plain name when attribute is optional")
    void testFormatAttributeName_Optional() {
        when(mockAttribute.name()).thenReturn("Color");
        when(mockLink.optional()).thenReturn(true);

        String result = formatter.formatAttributeName(mockLink, mockAttribute);

        assertEquals("Color", result);
    }

    @Test
    @DisplayName("Should append marker when attribute is mandatory (not optional)")
    void testFormatAttributeName_Mandatory() {
        when(mockAttribute.name()).thenReturn("Price");
        when(mockLink.optional()).thenReturn(false);

        String result = formatter.formatAttributeName(mockLink, mockAttribute);

        // Verifies "Price*"
        assertEquals("Price" + NOT_OPTIONAL_MARKER, result);
    }

    // --- 2. Description Tests ---

    @Test
    @DisplayName("Should return description when present")
    void testFormatAttributeDescription_Present() {
        when(mockAttribute.description()).thenReturn("Product Color");
        assertEquals("Product Color", formatter.formatAttributeDescription(mockAttribute));
    }

    @Test
    @DisplayName("Should return empty string when description is null")
    void testFormatAttributeDescription_Null() {
        when(mockAttribute.description()).thenReturn(null);
        assertEquals("", formatter.formatAttributeDescription(mockAttribute));
    }

    // --- 3. Simple Type Tests ---

    @Test
    @DisplayName("Should format simple single-value type")
    void testFormatAttributeTypeDescription_SimpleSingle() {
        setupType(mockAttribute, "string", false);

        String result = formatter.formatAttributeTypeDescription(Collections.emptyMap(), mockAttribute);

        assertEquals("string", result);
    }

    @Test
    @DisplayName("Should format simple multi-value type with marker")
    void testFormatAttributeTypeDescription_SimpleMulti() {
        setupType(mockAttribute, "integer", true);

        // Verifies "integer[]"
        assertEquals("integer" + MULTIVALUE_MARKER, formatter.formatAttributeTypeDescription(Collections.emptyMap(), mockAttribute));
    }

    // --- 4. Recursive Composite Tests ---

    @Test
    @DisplayName("Should recursively format a composite type with one simple child")
    void testFormatAttributeTypeDescription_CompositeWithChild() {
        // 1. Setup Parent (Composite)
        setupType(mockAttribute, COMPOSITE_TYPE_ID, false);
        when(mockAttribute.name()).thenReturn("Address");

        // 2. Setup Child (Simple String)
        Attribute childAttr = mock(Attribute.class);
        AttributeLink childLink = mock(AttributeLink.class);
        AttributeType childType = mock(AttributeType.class);

        when(childAttr.name()).thenReturn("Street");
        when(childAttr.type()).thenReturn(childType);
        when(childType.id()).thenReturn("string");
        when(childType.multiValue()).thenReturn(false);

        when(childLink.id()).thenReturn("street_id");
        when(childLink.optional()).thenReturn(true); // Optional = no "*"

        when(mockAttribute.attributeLinks()).thenReturn(List.of(childLink));

        // 3. Execution
        Map<String, Attribute> lookup = Map.of("street_id", childAttr);
        String result = formatter.formatAttributeTypeDescription(lookup, mockAttribute);

        // 4. Verification
        // Expected structure: Address{\n  Street:string\n}
        String expected = "Address" + COMPOSITE_TYPE_OPENING_BRACE + LINE_SEPARATOR +
                "  " + "Street" + TYPE_MAPPING_SEPARATOR + "string" +
                LINE_SEPARATOR + COMPOSITE_TYPE_CLOSING_BRACE;

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should format nested composites (Composite -> Composite -> Simple)")
    void testFormatAttributeTypeDescription_DeeplyNested() {
        // Root: Specification (Composite)
        Attribute root = createMockAttribute("Specification", COMPOSITE_TYPE_ID);
        AttributeLink rootLink = mock(AttributeLink.class);
        when(rootLink.id()).thenReturn("dim_id");
        when(root.attributeLinks()).thenReturn(List.of(rootLink));

        // Middle: Dimensions (Composite)
        Attribute middle = createMockAttribute("Dimensions", COMPOSITE_TYPE_ID);
        AttributeLink middleLink = createMockLink(); // Mandatory
        when(middle.attributeLinks()).thenReturn(List.of(middleLink));

        // Leaf: Width (Simple Number)
        Attribute leaf = createMockAttribute("Width", "number");

        // Lookup Map
        Map<String, Attribute> lookup = Map.of(
                "dim_id", middle,
                "width_id", leaf
        );

        String result = formatter.formatAttributeTypeDescription(lookup, root);

        /*
         * Validation: We check key fragments because line separators can vary.
         * Structure: Specification{ Dimensions{ Width*:number } }
         */
        assertContains(result, "Specification" + COMPOSITE_TYPE_OPENING_BRACE);
        assertContains(result, "Dimensions" + COMPOSITE_TYPE_OPENING_BRACE);
        assertContains(result, "Width" + NOT_OPTIONAL_MARKER); // Verify mandatory marker
        assertContains(result, TYPE_MAPPING_SEPARATOR + "number");
    }

    @Test
    @DisplayName("Should ignore links to attributes missing from lookup map")
    void testFormatAttributeTypeDescription_MissingLookup() {
        setupType(mockAttribute, COMPOSITE_TYPE_ID, false);
        when(mockAttribute.name()).thenReturn("GhostContainer");

        AttributeLink linkToNowhere = mock(AttributeLink.class);
        when(linkToNowhere.id()).thenReturn("missing_id");

        when(mockAttribute.attributeLinks()).thenReturn(List.of(linkToNowhere));

        // Empty lookup map
        String result = formatter.formatAttributeTypeDescription(Collections.emptyMap(), mockAttribute);

        // Should return container with no children
        String expected = "GhostContainer" + COMPOSITE_TYPE_OPENING_BRACE + LINE_SEPARATOR + COMPOSITE_TYPE_CLOSING_BRACE;
        assertEquals(expected, result);
    }

    // --- Helpers ---

    private void setupType(Attribute attr, String typeId, boolean multiValue) {
        when(attr.type()).thenReturn(mockType);
        when(mockType.id()).thenReturn(typeId);
        when(mockType.multiValue()).thenReturn(multiValue);
    }

    private Attribute createMockAttribute(String name, String typeId) {
        Attribute attr = mock(Attribute.class);
        AttributeType type = mock(AttributeType.class);
        when(attr.name()).thenReturn(name);
        when(attr.type()).thenReturn(type);
        when(type.id()).thenReturn(typeId);
        when(type.multiValue()).thenReturn(false);
        return attr;
    }

    private AttributeLink createMockLink() {
        AttributeLink link = mock(AttributeLink.class);
        when(link.id()).thenReturn("width_id");
        when(link.optional()).thenReturn(false);
        return link;
    }

    private void assertContains(String actual, String expectedFragment) {
        if (!actual.contains(expectedFragment)) {
            throw new AssertionError("Expected string to contain [" + expectedFragment + "] but was [" + actual + "]");
        }
    }
}
