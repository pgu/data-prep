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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Predicate;

import org.junit.Test;
import org.talend.dataprep.api.dataset.row.DataSetRow;

public class TQLFilterServiceTest extends FilterServiceTest {

    private TQLFilterService tqlFilterService = new TQLFilterService();

    @Test
    public void testValueEquals() throws Exception {
        // String
        row.set("0001", "test");
        assertThatConditionIsTrue("0001 = 'test'");
        assertThatConditionIsFalse("0001 = 'my value'");

        // Integer
        row.set("0001", "12");
        assertThatConditionIsTrue("0001 = 12");
        assertThatConditionIsFalse("0001 = 14");

        // boolean
        row.set("0001", "true");
        assertThatConditionIsTrue("0001 = true");
        assertThatConditionIsFalse("0001 = false");
        row.set("0001", "TRUE");
        assertThatConditionIsTrue("0001 = true");
        assertThatConditionIsFalse("0001 = false");
        row.set("0001", "FALSE");
        assertThatConditionIsTrue("0001 = false");
        assertThatConditionIsFalse("0001 = true");

        // decimal
        row.set("0001", "12.3");
        assertThatConditionIsTrue("0001 = 12.3");
        assertThatConditionIsFalse("0001 = 12.4");
        row.set("0001", "12.30");
        assertThatConditionIsTrue("0001 = 12.3");
        row.set("0001", "5.0");
        assertThatConditionIsTrue("0001 = 5");
        row.set("0001", "5,0");
        assertThatConditionIsTrue("0001 = 5");
    }

    private void assertThatConditionIsTrue(String tqlCondition) {
        // When
        final Predicate<DataSetRow> predicate = tqlFilterService.build(tqlCondition, rowMetadata);

        // Then
        assertThat(predicate.test(row)).isTrue();
    }

    private void assertThatConditionIsFalse(String tqlCondition) {
        // When
        final Predicate<DataSetRow> predicate = tqlFilterService.build(tqlCondition, rowMetadata);

        // Then
        assertThat(predicate.test(row)).isFalse();
    }

    @Test
    public void testValueIsNotEqual() throws Exception {
        // String
        row.set("0001", "my value");

        assertThatConditionIsTrue("0001 != 'test'");
        assertThatConditionIsFalse("0001 != 'my value'");

        // Integer
        row.set("0001", "12");
        assertThatConditionIsTrue("0001 != 14");
        assertThatConditionIsFalse("0001 != 12");

        // boolean
        row.set("0001", "true");
        assertThatConditionIsTrue("0001 != false");
        assertThatConditionIsFalse("0001 != true");
        row.set("0001", "TRUE");
        assertThatConditionIsTrue("0001 != false");
        assertThatConditionIsFalse("0001 != true");
        row.set("0001", "FALSE");
        assertThatConditionIsTrue("0001 != true");
        assertThatConditionIsFalse("0001 != false");

        // decimal
        row.set("0001", "12.3");
        assertThatConditionIsTrue("0001 != 12.4");
        assertThatConditionIsFalse("0001 != 12.3");
        row.set("0001", "12.30");
        assertThatConditionIsTrue("0001 != 12.4");
        assertThatConditionIsFalse("0001 != 12.3");
        row.set("0001", "5.0");
        assertThatConditionIsTrue("0001 != 6");
        assertThatConditionIsFalse("0001 != 5");
        row.set("0001", "5,0");
        assertThatConditionIsTrue("0001 != 6");
        assertThatConditionIsFalse("0001 != 5");
    }

    @Test
    public void testValueIsGreaterThan() throws Exception {
        // Integer
        row.set("0001", "0");

        assertThatConditionIsTrue("0001 > -1");
        assertThatConditionIsFalse("0001 > 0");

        row.set("0001", "-3");

        assertThatConditionIsTrue("0001 > -4");
        assertThatConditionIsFalse("0001 > 3");

        // decimal
        row.set("0001", "0.1");

        assertThatConditionIsTrue("0001 > 0.0");
        assertThatConditionIsFalse("0001 > 0.1");

        row.set("0001", "-0.1");

        assertThatConditionIsTrue("0001 > -0.3");
        assertThatConditionIsFalse("0001 > -0.1");

        row.set("0001", "-0,1");

        assertThatConditionIsTrue("0001 > -0.3");
        assertThatConditionIsFalse("0001 > -0.1");
    }

    @Test
    public void testValueIsLessThan() throws Exception {
        // Integer
        row.set("0001", "0");

        assertThatConditionIsTrue("0001 < 1");
        assertThatConditionIsFalse("0001 < 0");

        row.set("0001", "-3");

        assertThatConditionIsTrue("0001 < 1");
        assertThatConditionIsFalse("0001 < -5");

        // decimal
        row.set("0001", "0.1");

        assertThatConditionIsTrue("0001 < 0.5");
        assertThatConditionIsFalse("0001 < 0.0");

        row.set("0001", "-0.1");

        assertThatConditionIsTrue("0001 < -0.05");
        assertThatConditionIsFalse("0001 < -0.1");

        row.set("0001", "-0,1");

        assertThatConditionIsTrue("0001 < -0.05");
        assertThatConditionIsFalse("0001 < -0.1");
    }

    @Test
    public void testValueIsGreaterOrEqualThan() throws Exception {
        row.set("0001", "1234");

        assertThatConditionIsTrue("0001 >= 1111");
        assertThatConditionIsTrue("0001 >= 1234");
        assertThatConditionIsFalse("0001 >= 2223");
    }

    @Test
    public void testValueIsLessOrEqualThan() throws Exception {
        row.set("0001", "10");

        assertThatConditionIsTrue("0001 <= 99");
        assertThatConditionIsTrue("0001 <= 10");
        assertThatConditionIsFalse("0001 <= 2");
    }

    @Test
    public void testValueIsEmpty() throws Exception {
        row.set("0001", "");
        assertThatConditionIsTrue("0001 is empty");

        row.set("0001", "not empty");
        assertThatConditionIsFalse("0001 is empty");
    }

    @Test
    public void testValueIsValid() throws Exception {
        row.set("0001", "valid");
        assertThatConditionIsTrue("0001 is valid");

        row.setInvalid("0001");
        assertThatConditionIsFalse("0001 is valid");

        row.unsetInvalid("0001");
        assertThatConditionIsTrue("0001 is valid");
    }

    @Test
    public void testValueIsInvalid() throws Exception {
        row.set("0001", "valid");
        assertThatConditionIsFalse("0001 is invalid");

        row.setInvalid("0001");
        assertThatConditionIsTrue("0001 is invalid");

        row.unsetInvalid("0001");
        assertThatConditionIsFalse("0001 is invalid");
    }

    @Test
    public void testValueContains() throws Exception {
        row.set("0001", "skermabon@dataprep.com");
        assertThatConditionIsTrue("0001 contains 'dataprep'");
        assertThatConditionIsFalse("0001 contains 'talend'");
    }

    @Test
    public void testValueMatch() throws Exception {
        row.set("0001", "skermabon@dataprep.com");
        assertThatConditionIsTrue("0001 ~ '[a-z]+@dataprep.[a-z]+'");
        assertThatConditionIsFalse("0001 ~ '[a-z]+@talend.[a-z]+'");
    }

    @Test
    public void testValueIn() throws Exception {
        row.set("0001", "Vincent");
        assertThatConditionIsTrue("0001 in ['Vincent', 'François', 'Paul']");
        row.set("0001", "Stéphane");
        assertThatConditionIsFalse("0001 in ['Vincent', 'François', 'Paul']");
    }

    @Test
    public void testAndBooleanOperator() throws Exception {
        row.set("0001", "Vincent");
        assertThatConditionIsTrue("0001 in ['Vincent', 'François', 'Paul'] and 0001 contains 'Vinc'");
        row.set("0001", "Stéphane");
        assertThatConditionIsFalse("0001 in ['Vincent', 'François', 'Paul'] and 0001 contains 'phane'");
    }

    @Test
    public void testOrBooleanOperator() throws Exception {
        row.set("0001", "Vincent");
        assertThatConditionIsTrue("0001 in ['Vincent', 'François', 'Paul'] or 0001 contains 'Ted'");
        assertThatConditionIsTrue("0001 in ['Stéphane', 'François', 'Paul'] or 0001 contains 'Vinc'");
        row.set("0001", "Stéphane");
        assertThatConditionIsFalse("0001 in ['Vincent', 'François', 'Paul'] or 0001 contains 'Vinc'");
    }

    @Test
    public void testNotBooleanOperator() throws Exception {
        row.set("0001", "Stéphane");
        assertThatConditionIsTrue("not (0001 in ['Vincent', 'François', 'Paul'])");
        row.set("0001", "Vincent");
        assertThatConditionIsFalse("not (0001 in ['Vincent', 'François', 'Paul'])");
    }
}
