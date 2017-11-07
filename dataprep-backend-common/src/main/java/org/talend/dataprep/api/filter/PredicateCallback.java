package org.talend.dataprep.api.filter;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.daikon.number.BigDecimalParser;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.date.DateManipulator;
import org.talend.dataprep.quality.AnalyzerService;
import org.talend.dataprep.transformation.actions.Providers;
import org.talend.dataprep.transformation.actions.date.DateParser;
import org.talend.dataprep.util.NumericHelper;

import java.text.ParseException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Predicate;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.talend.dataprep.util.NumericHelper.isBigDecimal;

/**
 * An implementation of {@link JSONFilterCallback} that builds a {@link Predicate} for {@link DataSetRow}.
 *
 * @see SimpleFilterService
 */
public class PredicateCallback implements JSONFilterCallback<Predicate<DataSetRow>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredicateCallback.class);

    private DateParser dateParser;

    private static Predicate<DataSetRow> safeDate(Predicate<DataSetRow> inner) {
        return r -> {
            try {
                return inner.test(r);
            } catch (DateTimeException e) { // thrown by DateParser
                LOGGER.debug("Unable to parse date.", e);
                return false;
            }
        };
    }

    @Override
    public Predicate<DataSetRow> and(Predicate<DataSetRow> left, Predicate<DataSetRow> right) {
        return left.and(right);
    }

    @Override
    public Predicate<DataSetRow> not(Predicate<DataSetRow> expression) {
        return expression.negate();
    }

    @Override
    public Predicate<DataSetRow> empty() {
        return r -> true;
    }

    @Override
    public Predicate<DataSetRow> or(Predicate<DataSetRow> left, Predicate<DataSetRow> right) {
        return left.or(right);
    }

    /**
     * Create a predicate that checks if the var is equals to a value.
     * <p>
     * It first tries String comparison, and if not 'true' uses number comparison.
     *
     * @param node     The filter node
     * @param columnId The column id
     * @param value    The compare value
     * @return The eq predicate
     */
    @Override
    public Predicate<DataSetRow> createEqualsPredicate(final JsonNode node, final String columnId, final String value) {
        checkValidValue(node, value);
        return r -> {
            if (StringUtils.equals(r.get(columnId), value)) {
                return true;
            } else {
                return isBigDecimal(r.get(columnId)) //
                        && isBigDecimal(value) //
                        && NumberUtils.compare(toBigDecimal(r.get(columnId)), toBigDecimal(value)) == 0;
            }
        };
    }

    /**
     * Create a predicate that checks if the var is greater than a value
     *
     * @param node     The filter node
     * @param columnId The column id
     * @param value    The compare value
     * @return The gt predicate
     */
    @Override
    public Predicate<DataSetRow> createGreaterThanPredicate(final JsonNode node, final String columnId, final String value) {
        checkValidValue(node, value);
        return r -> isBigDecimal(r.get(columnId)) //
                && isBigDecimal(value) //
                && toBigDecimal(r.get(columnId)) > toBigDecimal(value);
    }

    /**
     * Create a predicate that checks if the var is lower than a value
     *
     * @param node     The filter node
     * @param columnId The column id
     * @param value    The compare value
     * @return The lt predicate
     */
    @Override
    public Predicate<DataSetRow> createLowerThanPredicate(final JsonNode node, final String columnId, final String value) {
        checkValidValue(node, value);
        return r -> isBigDecimal(r.get(columnId)) //
                && isBigDecimal(value) //
                && toBigDecimal(r.get(columnId)) < toBigDecimal(value);
    }

    /**
     * Create a predicate that checks if the var is greater than or equals to a value
     *
     * @param node     The filter node
     * @param columnId The column id
     * @param value    The compare value
     * @return The gte predicate
     */
    @Override
    public Predicate<DataSetRow> createGreaterOrEqualsPredicate(final JsonNode node, final String columnId, final String value) {
        checkValidValue(node, value);
        return r -> isBigDecimal(r.get(columnId)) //
                && isBigDecimal(value) //
                && toBigDecimal(r.get(columnId)) >= toBigDecimal(value);
    }

    /**
     * Create a predicate that checks if the var is lower than or equals to a value
     *
     * @param node     The filter node
     * @param columnId The column id
     * @param value    The compare value
     * @return The lte predicate
     */
    @Override
    public Predicate<DataSetRow> createLowerOrEqualsPredicate(final JsonNode node, final String columnId, final String value) {
        checkValidValue(node, value);
        return r -> isBigDecimal(r.get(columnId)) //
                && isBigDecimal(value) //
                && toBigDecimal(r.get(columnId)) <= toBigDecimal(value);
    }

    /**
     * Create a predicate that checks if the var contains a value
     *
     * @param node     The filter node
     * @param columnId The column id
     * @param value    The contained value
     * @return The contains predicate
     */
    @Override
    public Predicate<DataSetRow> createContainsPredicate(final JsonNode node, final String columnId, final String value) {
        checkValidValue(node, value);
        return r -> StringUtils.containsIgnoreCase(r.get(columnId), value);
    }

    /**
     * Create a predicate that checks if the var match a value
     *
     * @param node     The filter node
     * @param columnId The column id
     * @param value    The value to match
     * @return The match predicate
     */
    @Override
    public Predicate<DataSetRow> createMatchesPredicate(final JsonNode node, final String columnId, final String value) {
        checkValidValue(node, value);
        return r -> matches(r.get(columnId), value);
    }

    /**
     * Create a predicate that checks if the value is invalid
     *
     * @param columnId The column id
     * @return The invalid value predicate
     */
    @Override
    public Predicate<DataSetRow> createInvalidPredicate(final String columnId) {
        return r -> r.isInvalid(columnId);
    }

    /**
     * Create a predicate that checks if the value is value (not empty and not invalid)
     *
     * @param columnId The column id
     * @return The valid value predicate
     */
    @Override
    public Predicate<DataSetRow> createValidPredicate(final String columnId) {
        return r -> !r.isInvalid(columnId) && !isEmpty(r.get(columnId));
    }

    /**
     * Create a predicate that checks if the value is empty
     *
     * @param columnId The column id
     * @return The empty value predicate
     */
    @Override
    public Predicate<DataSetRow> createEmptyPredicate(final String columnId) {
        return r -> isEmpty(r.get(columnId));
    }

    /**
     * Create a predicate that checks if the value is within a range [min, max[
     *
     * @param columnId    The column id
     * @param node The node content that contains min/max values
     * @return The range predicate
     */
    @Override
    public Predicate<DataSetRow> createRangePredicate(final String columnId, final JsonNode node,
                                                      final RowMetadata rowMetadata) {
        final String start = node.get("start").asText();
        final String end = node.get("end").asText();
        final boolean upperBoundOpen = Optional.ofNullable(node.get("upperOpen")).map(JsonNode::asBoolean).orElse(true);
        final boolean lowerBoundOpen = Optional.ofNullable(node.get("lowerOpen")).map(JsonNode::asBoolean).orElse(false);
        return r -> {
            final String columnType = rowMetadata.getById(columnId).getType();
            Type parsedType = Type.get(columnType);
            if (Type.DATE.isAssignableFrom(parsedType)) {
                return createDateRangePredicate(columnId, start, lowerBoundOpen, end, upperBoundOpen, rowMetadata).test(r);
            } else {
                // Assume range can be parsed as number (may happen if column is currently marked as string, but will
                // contain some numbers).
                return createNumberRangePredicate(columnId, start, lowerBoundOpen, end, upperBoundOpen).test(r);
            }
        };
    }

    /**
     * Create a predicate that checks if the date value is within a range [min, max[
     *
     * @param columnId The column id
     * @param start    The start value
     * @param end      The end value
     * @param lowerBoundOpen   <code>true</code> if start is excluded from range.
     * @param upperBoundOpen   <code>true</code> if end is excluded from range.
     * @return The date range predicate
     */
    private Predicate<DataSetRow> createDateRangePredicate(final String columnId, final String start, boolean lowerBoundOpen, final String end,
                                                           boolean upperBoundOpen, final RowMetadata rowMetadata) {
        try {
            final long minTimestamp = Long.parseLong(start);
            final long maxTimestamp = Long.parseLong(end);

            final LocalDateTime minDate = DateManipulator.fromEpochMillisecondsWithSystemOffset(minTimestamp);
            final LocalDateTime maxDate = DateManipulator.fromEpochMillisecondsWithSystemOffset(maxTimestamp);

            return safeDate(r -> {
                final ColumnMetadata columnMetadata = rowMetadata.getById(columnId);
                final LocalDateTime columnValue = getDateParser().parse(r.get(columnId), columnMetadata);

                final boolean lowerBound;
                if (lowerBoundOpen) {
                    lowerBound = minDate.compareTo(columnValue) != 0 && minDate.isBefore(columnValue);
                } else {
                    lowerBound = minDate.compareTo(columnValue) == 0 || minDate.isBefore(columnValue);
                }
                final boolean upperBound;
                if (upperBoundOpen) {
                    upperBound = maxDate.compareTo(columnValue) != 0 && maxDate.isAfter(columnValue);
                } else {
                    upperBound = maxDate.compareTo(columnValue) == 0 || maxDate.isAfter(columnValue);
                }
                return lowerBound && upperBound;
            });
        } catch (Exception e) {
            LOGGER.debug("Unable to create date range predicate.", e);
            throw new IllegalArgumentException(
                    "Unsupported query, malformed date 'range' (expected timestamps in min and max properties).");
        }
    }

    private synchronized DateParser getDateParser() {
        if (dateParser == null) {
            dateParser = new DateParser(Providers.get(AnalyzerService.class));
        }
        return dateParser;
    }


    /**
     * Create a predicate that checks if the number value is within a range [min, max[
     *
     * @param columnId The column id
     * @param start    The start value
     * @param end      The end value
     * @param lowerBoundOpen   <code>true</code> if start is excluded from range.
     * @param upperBoundOpen   <code>true</code> if end is excluded from range.
     * @return The number range predicate
     */
    private Predicate<DataSetRow> createNumberRangePredicate(final String columnId, final String start, boolean lowerBoundOpen, final String end, boolean upperBoundOpen) {
        try {
            final double min = toBigDecimal(start);
            final double max = toBigDecimal(end);
            return r -> {
                final String value = r.get(columnId);
                if (NumericHelper.isBigDecimal(value)) {
                    final Double columnValue = toBigDecimal(value);

                    final boolean lowerBound;
                    if (lowerBoundOpen) {
                        lowerBound = columnValue.compareTo(min) > 0;
                    } else {
                        lowerBound = columnValue.compareTo(min) >= 0;
                    }
                    final boolean upperBound;
                    if (upperBoundOpen) {
                        upperBound = columnValue.compareTo(max) < 0;
                    } else {
                        upperBound = columnValue.compareTo(max) <= 0;
                    }
                    return lowerBound && upperBound;
                } else {
                    return false;
                }
            };
        } catch (Exception e) {
            LOGGER.debug("Unable to create number range predicate.", e);
            throw new IllegalArgumentException("Unsupported query, malformed 'range' (expected number min and max properties).");
        }
    }

    /**
     * check if the node has a non null value
     *
     * @param node  The node to test
     * @param value The node 'value' property
     * @throws IllegalArgumentException If the node has not a 'value' property
     */
    private void checkValidValue(final JsonNode node, final String value) {
        if (value == null) {
            throw new UnsupportedOperationException("Unsupported query, the filter needs a value : " + node.toString());
        }
    }

    /**
     * Test a string value against a pattern returned during value analysis.
     *
     * @param value   A string value. May be null.
     * @param pattern A pattern as returned in value analysis.
     * @return <code>true</code> if value matches, <code>false</code> otherwise.
     */
    private boolean matches(String value, String pattern) {
        if (value == null && pattern == null) {
            return true;
        }
        if (value == null) {
            return false;
        }
        // Character based patterns
        if (StringUtils.containsAny(pattern, new char[]{'A', 'a', '9'})) {
            if (value.length() != pattern.length()) {
                return false;
            }
            final char[] valueArray = value.toCharArray();
            final char[] patternArray = pattern.toCharArray();
            for (int i = 0; i < valueArray.length; i++) {
                if (patternArray[i] == 'A') {
                    if (!Character.isUpperCase(valueArray[i])) {
                        return false;
                    }
                } else if (patternArray[i] == 'a') {
                    if (!Character.isLowerCase(valueArray[i])) {
                        return false;
                    }
                } else if (patternArray[i] == '9') {
                    if (!Character.isDigit(valueArray[i])) {
                        return false;
                    }
                } else {
                    if (valueArray[i] != patternArray[i]) {
                        return false;
                    }
                }
            }
        } else {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            try {
                formatter.toFormat().parseObject(value);
            } catch (ParseException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Simple wrapper to call BigDecimalParser to simplify code above.
     */
    private double toBigDecimal(String value) {
        return BigDecimalParser.toBigDecimal(value).doubleValue();
    }

    public void setDateParser(DateParser dateParser) {
        this.dateParser = dateParser;
    }
}
