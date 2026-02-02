package com.stibo.demo.report.service;

import com.stibo.demo.report.model.Attribute;
import com.stibo.demo.report.model.AttributeGroup;
import com.stibo.demo.report.model.Category;
import com.stibo.demo.report.model.Datastandard;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provides lookup and transformation utilities for the {@link Datastandard} model.
 * <p>
 * This service extracts and indexes data from the flat lists within the data standard
 * to facilitate efficient attribute mapping and category tree traversal.
 * </p>
 */
@Service
public class DataStandardLookupService {

    /**
     * Traverses the category tree upwards from the specified category ID to the root.
     * <p>
     * The resulting list is ordered from the top-most parent down to the target category.
     * If the starting {@code categoryId} is not found, an empty list is returned.
     * </p>
     *
     * @param dataStandard the data standard containing the category definitions
     * @param categoryId   the ID of the category to start the traversal from
     * @return an ordered {@link List} of categories representing the hierarchy path
     */
    public List<Category> getCategoryHierarchy(Datastandard dataStandard, String categoryId) {
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

    /**
     * Indexes all attributes in the data standard by their unique identifier.
     *
     * @param dataStandard the data standard containing the attribute definitions
     * @return a {@link Map} where keys are attribute IDs and values are the corresponding {@link Attribute} objects
     */
    public Map<String, Attribute> getAttributeLookup(Datastandard dataStandard) {
        return dataStandard.attributes().stream()
                .collect(Collectors.toMap(Attribute::id, Function.identity()));
    }

    /**
     * Creates a lookup map for attribute group names indexed by their group ID.
     *
     * @param dataStandard the data standard containing the attribute group definitions
     * @return a {@link Map} where keys are group IDs and values are group names
     */
    public Map<String, String> getAttributeGroupLookup(Datastandard dataStandard) {
        return dataStandard.attributeGroups().stream()
                .collect(Collectors.toMap(AttributeGroup::id, AttributeGroup::name));
    }

    /**
     * Resolves a list of group IDs into their corresponding human-readable names.
     * <p>
     * Any group IDs that do not exist in the provided lookup map are filtered out of the result.
     * </p>
     *
     * @param groupLookup a map of group IDs to group names
     * @param groupIds    the list of group IDs to resolve
     * @return a {@link List} of resolved group names
     */
    public List<String> getAttributeGroupNames(Map<String, String> groupLookup, List<String> groupIds) {
        return groupIds.stream()
                .map(groupLookup::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
