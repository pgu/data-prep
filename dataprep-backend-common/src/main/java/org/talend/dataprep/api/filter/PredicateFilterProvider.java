// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.date.DateManipulator;
import org.talend.dataprep.quality.AnalyzerService;
import org.talend.dataprep.transformation.actions.Providers;
import org.talend.dataprep.transformation.actions.date.DateParser;
import org.talend.dataprep.util.NumericHelper;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

import static org.talend.daikon.number.BigDecimalParser.toBigDecimal;
import static org.talend.dataprep.util.NumericHelper.isBigDecimal;

public class PredicateFilterProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredicateFilterProvider.class);

    private DateParser dateParser;

    /**
     * Create a predicate that checks if the var is equals to a value.
     *
     * It first tries String comparison, and if not 'true' uses number comparison.
     *
     * @param columnId The column id
     * @param value The compared value
     * @return The eq predicate
     */
    public Predicate<DataSetRow> createEqualsPredicate(final String columnId, final String value) {
        return r -> {
            if (StringUtils.equals(r.get(columnId), value)) {
                return true;
            } else {
                return isBigDecimal(r.get(columnId)) //
                        && isBigDecimal(value) //
                        && toBigDecimal(r.get(columnId)).compareTo(toBigDecimal(value)) == 0;
            }
        };
    }

    /**
     * Create a predicate that checks if the var is greater than a value.
     *
     * @param columnId The column id
     * @param value The compared value
     * @return The gt predicate
     */
    public Predicate<DataSetRow> createGreaterThanPredicate(final String columnId, final String value) {
        return r -> isBigDecimal(r.get(columnId)) //
                && isBigDecimal(value) //
                && toBigDecimal(r.get(columnId)).compareTo(toBigDecimal(value)) > 0;
    }

    /**
     * Create a predicate that checks if the var is lower than a value.
     *
     * @param columnId The column id
     * @param value The compared value
     * @return The lt predicate
     */
    public Predicate<DataSetRow> createLowerThanPredicate(final String columnId, final String value) {
        return r -> isBigDecimal(r.get(columnId)) //
                && isBigDecimal(value) //
                && toBigDecimal(r.get(columnId)).compareTo(toBigDecimal(value)) < 0;
    }

    /**
     * Create a predicate that checks if the var is greater than or equal to a value.
     *
     * @param columnId The column id
     * @param value The compared value
     * @return The gte predicate
     */
    public Predicate<DataSetRow> createGreaterOrEqualsPredicate(final String columnId, final String value) {
        return r -> isBigDecimal(r.get(columnId)) //
                && isBigDecimal(value) //
                && toBigDecimal(r.get(columnId)).compareTo(toBigDecimal(value)) >= 0;
    }

    /**
     * Create a predicate that checks if the var is lower than or equals to a value.
     *
     * @param columnId The column id
     * @param value The compared value
     * @return The lte predicate
     */
    public Predicate<DataSetRow> createLowerOrEqualsPredicate(final String columnId, final String value) {
        return r -> isBigDecimal(r.get(columnId)) //
                && isBigDecimal(value) //
                && toBigDecimal(r.get(columnId)).compareTo(toBigDecimal(value)) <= 0;
    }

    /**
     * Create a predicate that checks if the var contains a value.
     *
     * @param columnId The column id
     * @param value The contained value
     * @return The contains predicate
     */
    public Predicate<DataSetRow> createContainsPredicate(final String columnId, final String value) {
        return r -> StringUtils.containsIgnoreCase(r.get(columnId), value);
    }

    /**
     * Create a predicate that checks if the var comply to a value.
     *
     * @param columnId The column id
     * @param value The value to comply to
     * @return The complies predicate
     */
    public Predicate<DataSetRow> createCompliesPredicate(final String columnId, final String value) {
        return r -> complies(r.get(columnId), value);
    }

    /**
     * Test a string value against a pattern.
     *
     * @param value A string value. May be null.
     * @param pattern A pattern as returned in value analysis.
     * @return <code>true</code> if value complies, <code>false</code> otherwise.
     */
    private boolean complies(String value, String pattern) {
        if (value == null && pattern == null) {
            return true;
        }
        if (value == null) {
            return false;
        }
        // Character based patterns
        if (StringUtils.containsAny(pattern, new char[] { 'A', 'a', '9' })) {
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
     * Create a predicate that checks if the var is invalid.
     *
     * @param columnId The column id
     * @return The invalid value predicate
     */
    public Predicate<DataSetRow> createInvalidPredicate(final String columnId) {
        return r -> r.isInvalid(columnId);
    }

    /**
     * Create a predicate that checks if the var is valid (not empty and not invalid).
     *
     * @param columnId The column id
     * @return The valid value predicate
     */
    public Predicate<DataSetRow> createValidPredicate(final String columnId) {
        return r -> !r.isInvalid(columnId) && StringUtils.isNotEmpty(r.get(columnId));
    }

    /**
     * Create a predicate that checks if the var is empty.
     *
     * @param columnId The column id
     * @return The empty value predicate
     */
    public Predicate<DataSetRow> createEmptyPredicate(final String columnId) {
        return r -> StringUtils.isEmpty(r.get(columnId));
    }

    /**
     * Create a predicate that checks if the value is within a range [min, max[.
     *
     * @param columnId The column id
     * @param min The range minimum (included)
     * @param max The range maximum (excluded)
     * @param rowMetadata The row metadata
     * @return The range predicate
     */
    public Predicate<DataSetRow> createRangePredicate(final String columnId, final String min, final String max,
            final RowMetadata rowMetadata) {
        return r -> {
            final String columnType = rowMetadata.getById(columnId).getType();
            Type parsedType = Type.get(columnType);
            if (Type.DATE.isAssignableFrom(parsedType)) {
                return createDateRangePredicate(columnId, min, max, rowMetadata).test(r);
            } else {
                // Assume range can be parsed as number (may happen if column is currently marked as string, but will
                // contain some numbers).
                return createNumberRangePredicate(columnId, min, max).test(r);
            }
        };
    }

    /**
     * Create a predicate that checks if the date value is within a range [min, max[.
     *
     * @param columnId The column id
     * @param start The start value
     * @param end The end value
     * @return The date range predicate
     */
    private Predicate<DataSetRow> createDateRangePredicate(final String columnId, final String start, final String end,
            final RowMetadata rowMetadata) {
        try {
            final long minTimestamp = Long.parseLong(start);
            final long maxTimestamp = Long.parseLong(end);

            final LocalDateTime minDate = DateManipulator.fromEpochMillisecondsWithSystemOffset(minTimestamp);
            final LocalDateTime maxDate = DateManipulator.fromEpochMillisecondsWithSystemOffset(maxTimestamp);

            return safeDate(r -> {
                final ColumnMetadata columnMetadata = rowMetadata.getById(columnId);
                final LocalDateTime columnValue = getDateParser().parse(r.get(columnId), columnMetadata);
                return minDate.compareTo(columnValue) == 0 || (minDate.isBefore(columnValue) && maxDate.isAfter(columnValue));
            });
        } catch (Exception e) {
            LOGGER.debug("Unable to create date range predicate.", e);
            throw new IllegalArgumentException(
                    "Unsupported query, malformed date 'range' (expected timestamps in min and max properties).");
        }
    }

    private Predicate<DataSetRow> safeDate(Predicate<DataSetRow> inner) {
        return r -> {
            try {
                return inner.test(r);
            } catch (DateTimeException e) { // thrown by DateParser
                LOGGER.debug("Unable to parse date.", e);
                return false;
            }
        };
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
     * @param min The minimal value (included)
     * @param max The maximal value (excluded)
     * @return The number range predicate
     */
    private Predicate<DataSetRow> createNumberRangePredicate(final String columnId, final String min, final String max) {
        try {
            final BigDecimal low = toBigDecimal(min);
            final BigDecimal high = toBigDecimal(max);
            return row -> {
                final String value = row.get(columnId);
                if (!NumericHelper.isBigDecimal(value)) {
                    return false;
                }
                final BigDecimal cellValue = toBigDecimal(value);
                return cellValue.compareTo(low) >= 0 && cellValue.compareTo(high) < 0;
            };
        } catch (Exception e) {
            LOGGER.debug("Unable to create number range predicate.", e);
            throw new IllegalArgumentException("Unsupported query, malformed 'range' (expected number min and max properties).");
        }
    }

}
