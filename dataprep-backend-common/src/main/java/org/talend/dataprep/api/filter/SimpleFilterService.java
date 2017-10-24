// ============================================================================
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.api.filter;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.talend.daikon.exception.TalendRuntimeException;
import org.talend.dataprep.BaseErrorCodes;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SimpleFilterService implements FilterService {

    private static final String EQ = "eq";

    private static final String GT = "gt";

    private static final String LT = "lt";

    private static final String GTE = "gte";

    private static final String LTE = "lte";

    private static final String CONTAINS = "contains";

    private static final String MATCHES = "matches";

    private static final String INVALID = "invalid";

    private static final String VALID = "valid";

    private static final String EMPTY = "empty";

    private static final String RANGE = "range";

    private static final String AND = "and";

    private static final String OR = "or";

    private static final String NOT = "not";

    private PredicateFilterProvider predicateFilterProvider;

    public SimpleFilterService(PredicateFilterProvider predicateFilterProvider) {
        super();
        this.predicateFilterProvider = predicateFilterProvider;
    }

    @Override
    public Predicate<DataSetRow> build(String filterAsString, RowMetadata rowMetadata) {
        if (isEmpty(filterAsString)) {
            return r -> true;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            final JsonNode root = mapper.reader().readTree(filterAsString);
            final Iterator<JsonNode> elements = root.elements();
            if (!elements.hasNext()) {
                throw new IllegalArgumentException("Malformed filter: " + filterAsString);
            } else {
                return buildFilter(root, rowMetadata);
            }
        } catch (Exception e) {
            throw new TalendRuntimeException(BaseErrorCodes.UNABLE_TO_PARSE_FILTER, e);
        }
    }

    private Predicate<DataSetRow> buildFilter(JsonNode currentNode, RowMetadata rowMetadata) {
        final Iterator<JsonNode> children = currentNode.elements();
        final JsonNode operationContent = children.next();
        final String columnId = operationContent.has("field") ? operationContent.get("field").asText() : null;
        final String value = operationContent.has("value") ? operationContent.get("value").asText() : null;

        final Iterator<String> propertiesIterator = currentNode.fieldNames();
        if (!propertiesIterator.hasNext()) {
            throw new UnsupportedOperationException("Unsupported query, empty filter definition: " + currentNode.toString());
        }

        final String operation = propertiesIterator.next();
        if (columnId == null && allowFullFilter(operation)) {
            // Full data set filter (no column)
            final List<ColumnMetadata> columns = rowMetadata.getColumns();
            Predicate<DataSetRow> predicate;
            if (!columns.isEmpty()) {
                predicate = buildOperationFilter(currentNode, rowMetadata, columns.get(0).getId(), operation, value);
                for (int i = 1; i < columns.size(); i++) {
                    predicate = predicate
                            .or(buildOperationFilter(currentNode, rowMetadata, columns.get(i).getId(), operation, value));
                }
            } else {
                // We can't return a null filter, default to the neutral value
                predicate = dsr -> true;
            }
            return predicate;
        } else {
            return buildOperationFilter(currentNode, rowMetadata, columnId, operation, value);
        }
    }

    private static boolean allowFullFilter(String operation) {
        switch (operation) {
        case EQ:
        case GT:
        case LT:
        case GTE:
        case LTE:
        case CONTAINS:
        case MATCHES:
        case INVALID:
        case VALID:
        case EMPTY:
        case RANGE:
            return true;
        case AND:
        case OR:
        case NOT:
        default:
            return false;
        }
    }

    private Predicate<DataSetRow> buildOperationFilter(JsonNode currentNode, //
            RowMetadata rowMetadata, //
            String columnId, //
            String operation, //
            String value) {
        switch (operation) {
        case EQ:
            return createEqualsPredicate(currentNode, columnId, value);
        case GT:
            return createGreaterThanPredicate(currentNode, columnId, value);
        case LT:
            return createLowerThanPredicate(currentNode, columnId, value);
        case GTE:
            return createGreaterOrEqualsPredicate(currentNode, columnId, value);
        case LTE:
            return createLowerOrEqualsPredicate(currentNode, columnId, value);
        case CONTAINS:
            return createContainsPredicate(currentNode, columnId, value);
        case MATCHES:
            return createMatchesPredicate(currentNode, columnId, value);
        case INVALID:
            return predicateFilterProvider.createInvalidPredicate(columnId);
        case VALID:
            return predicateFilterProvider.createValidPredicate(columnId);
        case EMPTY:
            return predicateFilterProvider.createEmptyPredicate(columnId);
        case RANGE:
            return createRangePredicate(columnId, currentNode.elements().next(), rowMetadata);
        case AND:
            return createAndPredicate(currentNode.elements().next(), rowMetadata);
        case OR:
            return createOrPredicate(currentNode.elements().next(), rowMetadata);
        case NOT:
            return createNotPredicate(currentNode.elements().next(), rowMetadata);
        default:
            throw new UnsupportedOperationException(
                    "Unsupported query, unknown filter '" + operation + "': " + currentNode.toString());
        }
    }

    /**
     * Create a predicate that do a logical AND between 2 filters
     *
     * @param nodeContent The node content
     * @param rowMetadata Row metadata to used to obtain information (valid/invalid, types...)
     * @return the AND predicate
     */
    private Predicate<DataSetRow> createAndPredicate(final JsonNode nodeContent, RowMetadata rowMetadata) {
        checkValidMultiPredicate(nodeContent);
        final Predicate<DataSetRow> leftFilter = buildFilter(nodeContent.get(0), rowMetadata);
        final Predicate<DataSetRow> rightFilter = buildFilter(nodeContent.get(1), rowMetadata);
        return leftFilter.and(rightFilter);
    }

    /**
     * Create a predicate that do a logical OR between 2 filters
     *
     * @param nodeContent The node content
     * @param rowMetadata Row metadata to used to obtain information (valid/invalid, types...)
     * @return the OR predicate
     */
    private Predicate<DataSetRow> createOrPredicate(final JsonNode nodeContent, RowMetadata rowMetadata) {
        checkValidMultiPredicate(nodeContent);
        final Predicate<DataSetRow> leftFilter = buildFilter(nodeContent.get(0), rowMetadata);
        final Predicate<DataSetRow> rightFilter = buildFilter(nodeContent.get(1), rowMetadata);
        return leftFilter.or(rightFilter);
    }

    /**
     * Create a predicate that negates a filter
     *
     * @param nodeContent The node content
     * @param rowMetadata Row metadata to used to obtain information (valid/invalid, types...)
     * @return The NOT predicate
     */
    private Predicate<DataSetRow> createNotPredicate(final JsonNode nodeContent, RowMetadata rowMetadata) {
        if (!nodeContent.isObject()) {
            throw new IllegalArgumentException("Unsupported query, malformed 'not' (expected 1 object child).");
        }
        if (nodeContent.size() == 0) {
            throw new IllegalArgumentException("Unsupported query, malformed 'not' (object child is empty).");
        }
        return buildFilter(nodeContent, rowMetadata).negate();
    }

    /**
     * Create a predicate that checks if the var is equals to a value.
     *
     * @param node The filter node
     * @param columnId The column id
     * @param value The compare value
     * @return The eq predicate
     */
    private Predicate<DataSetRow> createEqualsPredicate(final JsonNode node, final String columnId, final String value) {
        checkValidValue(node, value);
        return predicateFilterProvider.createEqualsPredicate(columnId, value);
    }

    /**
     * Create a predicate that checks if the var is greater than a value
     *
     * @param node The filter node
     * @param columnId The column id
     * @param value The compare value
     * @return The gt predicate
     */
    private Predicate<DataSetRow> createGreaterThanPredicate(final JsonNode node, final String columnId, final String value) {
        checkValidValue(node, value);
        return predicateFilterProvider.createGreaterThanPredicate(columnId, value);
    }

    /**
     * Create a predicate that checks if the var is lower than a value
     *
     * @param node The filter node
     * @param columnId The column id
     * @param value The compare value
     * @return The lt predicate
     */
    private Predicate<DataSetRow> createLowerThanPredicate(final JsonNode node, final String columnId, final String value) {
        checkValidValue(node, value);
        return predicateFilterProvider.createLowerThanPredicate(columnId, value);
    }

    /**
     * Create a predicate that checks if the var is greater than or equals to a value
     *
     * @param node The filter node
     * @param columnId The column id
     * @param value The compare value
     * @return The gte predicate
     */
    private Predicate<DataSetRow> createGreaterOrEqualsPredicate(final JsonNode node, final String columnId, final String value) {
        checkValidValue(node, value);
        return predicateFilterProvider.createGreaterOrEqualsPredicate(columnId, value);
    }

    /**
     * Create a predicate that checks if the var is lower than or equals to a value
     *
     * @param node The filter node
     * @param columnId The column id
     * @param value The compare value
     * @return The lte predicate
     */
    private Predicate<DataSetRow> createLowerOrEqualsPredicate(final JsonNode node, final String columnId, final String value) {
        checkValidValue(node, value);
        return predicateFilterProvider.createLowerOrEqualsPredicate(columnId, value);
    }

    /**
     * Create a predicate that checks if the var contains a value
     *
     * @param node The filter node
     * @param columnId The column id
     * @param value The contained value
     * @return The contains predicate
     */
    private Predicate<DataSetRow> createContainsPredicate(final JsonNode node, final String columnId, final String value) {
        checkValidValue(node, value);
        return predicateFilterProvider.createContainsPredicate(columnId, value);
    }

    /**
     * Create a predicate that checks if the var match a value
     *
     * @param node The filter node
     * @param columnId The column id
     * @param value The value to match
     * @return The match predicate
     */
    private Predicate<DataSetRow> createMatchesPredicate(final JsonNode node, final String columnId, final String value) {
        checkValidValue(node, value);
        return predicateFilterProvider.createCompliesPredicate(columnId, value);
    }

    /**
     * Create a predicate that checks if the value is within a range [min, max[
     *
     * @param columnId The column id
     * @param nodeContent The node content that contains min/max values
     * @return The range predicate
     */
    private Predicate<DataSetRow> createRangePredicate(final String columnId, final JsonNode nodeContent,
            final RowMetadata rowMetadata) {
        final String start = nodeContent.get("start").asText();
        final String end = nodeContent.get("end").asText();
        return predicateFilterProvider.createRangePredicate(columnId, start, end, rowMetadata);
    }

    /**
     * check if the node has a non null value
     *
     * @param node The node to test
     * @param value The node 'value' property
     * @throws IllegalArgumentException If the node has not a 'value' property
     */
    private void checkValidValue(final JsonNode node, final String value) {
        if (value == null) {
            throw new UnsupportedOperationException("Unsupported query, the filter needs a value : " + node.toString());
        }
    }

    /**
     * Check if the node has exactly 2 children. Used to safe check binary operator (and, or)
     *
     * @param nodeContent The node content
     * @throws IllegalArgumentException If the node has not exactly 2 children
     */
    private void checkValidMultiPredicate(final JsonNode nodeContent) {
        if (nodeContent.size() != 2) {
            throw new IllegalArgumentException("Unsupported query, malformed 'and' (expected 2 children).");
        }
    }
}
