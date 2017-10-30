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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.actions.date.DateParser;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.function.Predicate;

import static java.time.Month.JANUARY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public abstract class AbstractFilterServiceTest extends FilterServiceTest {

    protected final FilterService service = getFilterService();

    protected Predicate<DataSetRow> filter;

    @Before
    public void setUp() {
        filter = null;
    }

    /**
     * Return a FilterService.
     *
     * @return an instance of FilterService
     */
    protected abstract FilterService getFilterService();

    @Test
    public void testEqualsPredicateOnStringValue() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_equals_toto();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        assertThatFilterExecutionReturnsTrueForRow("0001", "toto");
        assertThatFilterExecutionReturnsFalseForRow("0001", "Toto"); // different case
        assertThatFilterExecutionReturnsFalseForRow("0001", "tatatoto"); // contains but different
        assertThatFilterExecutionReturnsFalseForRow("0001", ""); // empty
        assertThatFilterExecutionReturnsFalseForRow("0001", null); // null
    }

    protected abstract String givenFilter_0001_equals_toto();

    protected void assertThatFilterExecutionReturnsTrueForRow(String columnId, String value) {
        row.set(columnId, value);
        assertThatFilterExecutionReturnsTrue();
    }

    protected void assertThatFilterExecutionReturnsFalseForRow(String columnId, String value) {
        row.set(columnId, value);
        assertThatFilterExecutionReturnsFalse();
    }

    @Test
    public void testEqualsPredicateOnIntegerValue() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_equals_5();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        assertThatFilterExecutionReturnsTrueForRow("0001", "5.0"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "5,00"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "05.0"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "0 005"); // eq

        assertThatFilterExecutionReturnsFalseForRow("0001", "3"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", "4.5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", "4,5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", ",5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", ".5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", "1.000,5"); // gt
        assertThatFilterExecutionReturnsFalseForRow("0001", "1 000.5"); // gt
    }

    protected abstract String givenFilter_0001_equals_5();

    @Test
    public void testEqualsPredicateOnDecimalValue() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_equals_5dot35();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        assertThatFilterExecutionReturnsTrueForRow("0001", "5.35"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "5,35"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "05.35"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "5,3500"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "5,3500"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "0 005.35"); // eq

        assertThatFilterExecutionReturnsFalseForRow("0001", "4.5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", "4,5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", ",5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", ".5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", "1.000,5"); // gt
        assertThatFilterExecutionReturnsFalseForRow("0001", "1 000.5"); // gt
    }

    protected abstract String givenFilter_0001_equals_5dot35();

    @Test
    public void testNotEqualPredicateOnStringValue() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_not_equal_test();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        assertThatFilterExecutionReturnsTrueForRow("0001", "toto"); // neq
        assertThatFilterExecutionReturnsTrueForRow("0001", "Test"); // neq

        assertThatFilterExecutionReturnsFalseForRow("0001", "test"); // eq
    }

    protected abstract String givenFilter_0001_not_equal_test();

    @Test
    public void testNotEqualPredicateOnIntegerValue() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_not_equal_12();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        assertThatFilterExecutionReturnsTrueForRow("0001", "12.1"); // neq
        assertThatFilterExecutionReturnsTrueForRow("0001", "14"); // neq

        assertThatFilterExecutionReturnsFalseForRow("0001", "12"); // eq
        assertThatFilterExecutionReturnsFalseForRow("0001", "12.00"); // eq
        assertThatFilterExecutionReturnsFalseForRow("0001", "012,0"); // eq
    }

    protected abstract String givenFilter_0001_not_equal_12();

    @Test
    public void testNotEqualPredicateOnDecimalValue() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_not_equal_24dot6();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        assertThatFilterExecutionReturnsTrueForRow("0001", "24"); // neq
        assertThatFilterExecutionReturnsTrueForRow("0001", "26.6"); // neq

        assertThatFilterExecutionReturnsFalseForRow("0001", "24.60"); // eq
        assertThatFilterExecutionReturnsFalseForRow("0001", "24,6"); // eq
        assertThatFilterExecutionReturnsFalseForRow("0001", "024,60"); // eq
    }

    protected abstract String givenFilter_0001_not_equal_24dot6();

    @Test
    public void testGreaterThanPredicateOnIntegerValue() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_greater_than_5();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        assertThatFilterExecutionReturnsTrueForRow("0001", "6"); // gt
        assertThatFilterExecutionReturnsFalseForRow("0001", "5"); // eq
        assertThatFilterExecutionReturnsFalseForRow("0001", "4"); // lt

        assertThatFilterExecutionReturnsFalseForRow("0001", "toto"); // nan
        assertThatFilterExecutionReturnsFalseForRow("0001", ""); // nan
        assertThatFilterExecutionReturnsFalseForRow("0001", null); // null

        assertThatFilterExecutionReturnsFalseForRow("0001", "4.5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", "4,5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", ",5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", ".5"); // lt

        assertThatFilterExecutionReturnsFalseForRow("0001", "5.0"); // eq
        assertThatFilterExecutionReturnsFalseForRow("0001", "5,00"); // eq
        assertThatFilterExecutionReturnsFalseForRow("0001", "05.0"); // eq
        assertThatFilterExecutionReturnsFalseForRow("0001", "0 005"); // eq

        assertThatFilterExecutionReturnsTrueForRow("0001", "5.5"); // gt
        assertThatFilterExecutionReturnsTrueForRow("0001", "5,5"); // gt
        assertThatFilterExecutionReturnsTrueForRow("0001", "1.000,5"); // gt
        assertThatFilterExecutionReturnsTrueForRow("0001", "1 000.5"); // gt
    }

    protected abstract String givenFilter_0001_greater_than_5();

    @Test
    public void testGreaterThanPredicateOnNegativeDecimalValue() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_greater_than_minus0dot1();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        assertThatFilterExecutionReturnsTrueForRow("0001", "-0.05"); // gt
        assertThatFilterExecutionReturnsTrueForRow("0001", "1"); // gt
        assertThatFilterExecutionReturnsFalseForRow("0001", "-0.1"); // eq
        assertThatFilterExecutionReturnsFalseForRow("0001", "-2"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", "-10.3"); // lt

        assertThatFilterExecutionReturnsFalseForRow("0001", "toto"); // nan
        assertThatFilterExecutionReturnsFalseForRow("0001", ""); // nan
        assertThatFilterExecutionReturnsFalseForRow("0001", null); // null
    }

    protected abstract String givenFilter_0001_greater_than_minus0dot1();

    @Test
    public void testGreaterThanOrEqualPredicateOnIntegerValue() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_greater_or_equal_5();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        assertThatFilterExecutionReturnsTrueForRow("0001", "6"); // gt
        assertThatFilterExecutionReturnsTrueForRow("0001", "5"); // eq
        assertThatFilterExecutionReturnsFalseForRow("0001", "4"); // lt

        assertThatFilterExecutionReturnsFalseForRow("0001", "toto"); // nan
        assertThatFilterExecutionReturnsFalseForRow("0001", ""); // nan
        assertThatFilterExecutionReturnsFalseForRow("0001", null); // null

        assertThatFilterExecutionReturnsFalseForRow("0001", "4.5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", "4,5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", ",5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", ".5"); // lt

        assertThatFilterExecutionReturnsTrueForRow("0001", "5.0"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "5,00"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "05.0"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "0 005"); // eq

        assertThatFilterExecutionReturnsTrueForRow("0001", "5.5"); // gt
        assertThatFilterExecutionReturnsTrueForRow("0001", "5,5"); // gt
        assertThatFilterExecutionReturnsTrueForRow("0001", "1.000,5"); // gt
        assertThatFilterExecutionReturnsTrueForRow("0001", "1 000.5"); // gt
    }

    protected abstract String givenFilter_0001_greater_or_equal_5();

    @Test
    public void testLessThanPredicateOnIntegerValue() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_less_than_5();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        assertThatFilterExecutionReturnsFalseForRow("0001", "6"); // gt
        assertThatFilterExecutionReturnsFalseForRow("0001", "5"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "4"); // lt

        assertThatFilterExecutionReturnsFalseForRow("0001", "toto"); // nan
        assertThatFilterExecutionReturnsFalseForRow("0001", ""); // nan
        assertThatFilterExecutionReturnsFalseForRow("0001", null); // null

        assertThatFilterExecutionReturnsTrueForRow("0001", "4.5"); // lt
        assertThatFilterExecutionReturnsTrueForRow("0001", "4,5"); // lt
        assertThatFilterExecutionReturnsTrueForRow("0001", ",5"); // lt
        assertThatFilterExecutionReturnsTrueForRow("0001", ".5"); // lt

        assertThatFilterExecutionReturnsFalseForRow("0001", "5.0"); // eq
        assertThatFilterExecutionReturnsFalseForRow("0001", "5,00"); // eq
        assertThatFilterExecutionReturnsFalseForRow("0001", "05.0"); // eq
        assertThatFilterExecutionReturnsFalseForRow("0001", "0 005"); // eq

        assertThatFilterExecutionReturnsFalseForRow("0001", "5.5"); // gt
        assertThatFilterExecutionReturnsFalseForRow("0001", "5,5"); // gt
        assertThatFilterExecutionReturnsFalseForRow("0001", "1.000,5"); // gt
        assertThatFilterExecutionReturnsFalseForRow("0001", "1 000.5"); // gt
    }

    protected abstract String givenFilter_0001_less_than_5();

    @Test
    public void testLessThanOrEqualPredicateOnIntegerValue() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_less_or_equal_5();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        assertThatFilterExecutionReturnsFalseForRow("0001", "6"); // gt
        assertThatFilterExecutionReturnsTrueForRow("0001", "5"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "4"); // lt

        assertThatFilterExecutionReturnsFalseForRow("0001", "toto"); // nan
        assertThatFilterExecutionReturnsFalseForRow("0001", ""); // nan
        assertThatFilterExecutionReturnsFalseForRow("0001", null); // null

        assertThatFilterExecutionReturnsTrueForRow("0001", "4.5"); // lt
        assertThatFilterExecutionReturnsTrueForRow("0001", "4,5"); // lt
        assertThatFilterExecutionReturnsTrueForRow("0001", ",5"); // lt
        assertThatFilterExecutionReturnsTrueForRow("0001", ".5"); // lt

        assertThatFilterExecutionReturnsTrueForRow("0001", "5.0"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "5,00"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "05.0"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "0 005"); // eq

        assertThatFilterExecutionReturnsFalseForRow("0001", "5.5"); // gt
        assertThatFilterExecutionReturnsFalseForRow("0001", "5,5"); // gt
        assertThatFilterExecutionReturnsFalseForRow("0001", "1.000,5"); // gt
        assertThatFilterExecutionReturnsFalseForRow("0001", "1 000.5"); // gt
    }

    protected abstract String givenFilter_0001_less_or_equal_5();

    @Test
    public void testContainsPredicateOnStringValue() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_contains_toto();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        assertThatFilterExecutionReturnsTrueForRow("0001", "toto"); // equals
        assertThatFilterExecutionReturnsTrueForRow("0001", "Toto"); // different case
        assertThatFilterExecutionReturnsTrueForRow("0001", "tatatoto"); // contains but different
        assertThatFilterExecutionReturnsFalseForRow("0001", "tagada"); // not contains
    }

    protected abstract String givenFilter_0001_contains_toto();

    @Test
    public void testCompliesPredicateOnStringValue() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_complies_Aa9dash();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        assertThatFilterExecutionReturnsFalseForRow("0001", "toto"); // different pattern
        assertThatFilterExecutionReturnsTrueForRow("0001", "To5-"); // same pattern
        assertThatFilterExecutionReturnsFalseForRow("0001", "To5--"); // different length
        assertThatFilterExecutionReturnsFalseForRow("0001", ""); // empty value
    }

    protected abstract String givenFilter_0001_complies_Aa9dash();

    @Test
    public void testCompliesEmptyPatternPredicateOnStringValue() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_complies_empty();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        assertThatFilterExecutionReturnsTrueForRow("0001", ""); // empty value
        assertThatFilterExecutionReturnsFalseForRow("0001", "tagada"); // not empty value
    }

    protected abstract String givenFilter_0001_complies_empty();

    @Test
    public void testInvalidPredicate() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_is_invalid();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        row.setInvalid("0001"); // value in invalid array in column metadata
        assertThatFilterExecutionReturnsTrue();
        row.unsetInvalid("0001");
        assertThatFilterExecutionReturnsFalse();
    }

    protected abstract String givenFilter_0001_is_invalid();

    protected void assertThatFilterExecutionReturnsTrue() {
        assertThat(filter.test(row)).isTrue();
    }

    protected void assertThatFilterExecutionReturnsFalse() {
        assertThat(filter.test(row)).isFalse();
    }

    @Test
    public void testValidPredicate() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_is_valid();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        row.setInvalid("0001"); // value is marked as invalid
        assertThatFilterExecutionReturnsFalse();

        row.unsetInvalid("0001"); // value is marked as valid
        assertThatFilterExecutionReturnsFalseForRow("0001", ""); // empty

        row.unsetInvalid("0001"); // value is marked as valid
        assertThatFilterExecutionReturnsTrueForRow("0001", "toto"); // correct value
    }

    protected abstract String givenFilter_0001_is_valid();

    @Test
    public void testEmptyPredicate() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_is_empty();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        assertThatFilterExecutionReturnsTrueForRow("0001", ""); // empty
        assertThatFilterExecutionReturnsFalseForRow("0001", "toto"); // not empty value
    }

    protected abstract String givenFilter_0001_is_empty();

    @Test
    public void testBetweenPredicateOnNumberValue() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_between_5_and_10();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        row.getRowMetadata().getById("0001").setType("integer");
        assertThatFilterExecutionReturnsFalseForRow("0001", "a"); // invalid number
        assertThatFilterExecutionReturnsFalseForRow("0001", "4"); // lt min
        assertThatFilterExecutionReturnsTrueForRow("0001", "5"); // eq min
        assertThatFilterExecutionReturnsTrueForRow("0001", "8"); // in range
        assertThatFilterExecutionReturnsFalseForRow("0001", "10"); // eq max
        assertThatFilterExecutionReturnsFalseForRow("0001", "20"); // gt max

        assertThatFilterExecutionReturnsFalseForRow("0001", "toto"); // nan
        assertThatFilterExecutionReturnsFalseForRow("0001", ""); // nan
        assertThatFilterExecutionReturnsFalseForRow("0001", null); // null

        assertThatFilterExecutionReturnsFalseForRow("0001", "4.5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", "4,5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", ",5"); // lt
        assertThatFilterExecutionReturnsFalseForRow("0001", ".5"); // lt

        assertThatFilterExecutionReturnsTrueForRow("0001", "5.0"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "5,00"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "05.0"); // eq
        assertThatFilterExecutionReturnsTrueForRow("0001", "0 005"); // eq

        assertThatFilterExecutionReturnsTrueForRow("0001", "5.5"); // gt
        assertThatFilterExecutionReturnsTrueForRow("0001", "5,5"); // gt
        assertThatFilterExecutionReturnsFalseForRow("0001", "1.000,5"); // gt
        assertThatFilterExecutionReturnsFalseForRow("0001", "1 000.5"); // gt
    }

    protected abstract String givenFilter_0001_between_5_and_10();

    @Test
    public void testBetweenPredicateOnDateValue() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_between_timestampFor19700101_and_timestampFor19900101();

        final ColumnMetadata column = row.getRowMetadata().getById("0001");
        column.setType("date");
        final DateParser dateParser = Mockito.mock(DateParser.class);
        when(dateParser.parse("a", column)).thenThrow(new DateTimeException(""));
        when(dateParser.parse("1960-01-01", column)).thenReturn(LocalDateTime.of(1960, JANUARY, 1, 0, 0));
        when(dateParser.parse("1970-01-01", column)).thenReturn(LocalDateTime.of(1970, JANUARY, 1, 0, 0));
        when(dateParser.parse("1980-01-01", column)).thenReturn(LocalDateTime.of(1980, JANUARY, 1, 0, 0));
        when(dateParser.parse("1990-01-01", column)).thenReturn(LocalDateTime.of(1990, JANUARY, 1, 0, 0));
        when(dateParser.parse("2000-01-01", column)).thenReturn(LocalDateTime.of(2000, JANUARY, 1, 0, 0));

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        assertThatFilterExecutionReturnsFalseForRow("0001", "a"); // invalid number
        assertThatFilterExecutionReturnsFalseForRow("0001", "1960-01-01"); // lt min
        assertThatFilterExecutionReturnsTrueForRow("0001", "1970-01-01"); // eq min
        assertThatFilterExecutionReturnsTrueForRow("0001", "1980-01-01"); // in range
        assertThatFilterExecutionReturnsFalseForRow("0001", "1990-01-01"); // eq max
        assertThatFilterExecutionReturnsFalseForRow("0001", "2000-01-01"); // gt max

    }

    protected abstract String givenFilter_0001_between_timestampFor19700101_and_timestampFor19900101();

    @Test
    public void should_create_AND_predicate() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_is_empty_AND_0002_equals_toto();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        row.set("0001", ""); // empty
        row.set("0002", "toto"); // eq value
        assertThatFilterExecutionReturnsTrue();
        row.set("0001", "tata"); // not empty
        row.set("0002", "toto"); // eq value
        assertThatFilterExecutionReturnsFalse();
        row.set("0001", ""); // empty
        row.set("0002", "tata"); // neq value
        assertThatFilterExecutionReturnsFalse();
    }

    protected abstract String givenFilter_0001_is_empty_AND_0002_equals_toto();

    @Test
    public void should_create_OR_predicate() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_contains_data_OR_0002_equals_12dot3();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        row.set("0001", "dataprep"); // contains
        row.set("0002", "12,30"); // eq value
        assertThatFilterExecutionReturnsTrue();
        row.set("0001", "toto"); // does not contain
        row.set("0002", "012.3"); // eq value
        assertThatFilterExecutionReturnsTrue();
        row.set("0001", "great data"); // contains
        row.set("0002", "12"); // neq value
        assertThatFilterExecutionReturnsTrue();
        row.set("0001", "tata"); // does not contain
        row.set("0002", "tata"); // neq value
        assertThatFilterExecutionReturnsFalse();
    }

    protected abstract String givenFilter_0001_contains_data_OR_0002_equals_12dot3();

    @Test
    public void should_create_NOT_predicate() throws Exception {
        // given
        final String filtersDefinition = givenFilter_0001_does_not_contain_word();

        // when
        filter = service.build(filtersDefinition, rowMetadata);

        // then
        assertThatFilterExecutionReturnsTrueForRow("0001", "another sentence"); // does not contain
        assertThatFilterExecutionReturnsFalseForRow("0001", "great wording"); // contains
    }

    protected abstract String givenFilter_0001_does_not_contain_word();

}
