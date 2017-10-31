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

import static org.assertj.core.api.Assertions.assertThat;
import static org.talend.dataprep.transformation.actions.common.ImplicitParameters.FILTER;

import java.util.Arrays;

import org.junit.Test;
import org.talend.dataprep.api.preparation.Action;
import org.talend.dataprep.api.preparation.PreparationMessage;

public class FilterTranslatorTest {

    private final FilterTranslator filterTranslator = new FilterTranslator();

    @Test
    public void testTranslateFiltersToTQL_DoNothing_WhenPreparationContainsNoAction() {
        // given
        final PreparationMessage preparation = makePreparationWithActions(null);

        // when
        filterTranslator.translateFiltersToTQL(preparation);

        // then
        assertThat(preparation.getActions()).isNull();
    }

    private PreparationMessage makePreparationWithActions(Action... actions) {
        final PreparationMessage preparation = new PreparationMessage();
        if (actions == null || actions[0] == null) {
            preparation.setActions(null);
            return preparation;
        }
        preparation.setActions(Arrays.asList(actions));
        return preparation;
    }

    @Test
    public void testTranslateFiltersToTQL_DoNothing_WhenPreparationContainsNoFilter() {
        // given
        final Action firstAction = new Action();
        final Action secondAction = new Action();
        final PreparationMessage preparation = makePreparationWithActions(firstAction, secondAction);

        // when
        filterTranslator.translateFiltersToTQL(preparation);

        // then
        assertThat(getFilterForActionAt(0, preparation)).isNull();
        assertThat(getFilterForActionAt(1, preparation)).isNull();
    }

    private String getFilterForActionAt(final int index, final PreparationMessage preparation) {
        return preparation.getActions().get(index).getParameters().get(FILTER.getKey());
    }

    @Test
    public void testTranslateFiltersToTQL_LeaveFilterUnchanged_WhenAlreadyTQL() {
        // given
        final String filter = "0001 = 'toto'";
        final Action action = makeActionWithFilter(filter);
        final PreparationMessage preparation = makePreparationWithActions(action);

        // when
        filterTranslator.translateFiltersToTQL(preparation);

        // then
        assertThat(getFilterForActionAt(0, preparation)).isEqualTo(filter);
    }

    private Action makeActionWithFilter(final String filter) {
        final Action action = new Action();
        action.getParameters().put(FILTER.getKey(), filter);
        return action;
    }

    @Test
    public void testTranslateFiltersToTQL_TranslateColumnIdEqualsNumericValue() {
        // given
        final String filter = //
                "{" + //
                        "   \"eq\": {" + //
                        "       \"field\":\"0001\", " + //
                        "       \"value\": \"12\"" + //
                        "   }" + //
                        "}";
        final Action action = makeActionWithFilter(filter);
        final PreparationMessage preparation = makePreparationWithActions(action);
        final String expectedTQLFilter = "0001 = 12";

        // when
        filterTranslator.translateFiltersToTQL(preparation);

        // then
        assertThat(getFilterForActionAt(0, preparation)).isEqualTo(expectedTQLFilter);
    }

    @Test
    public void testTranslateFiltersToTQL_TranslateAndExpression() {
        // given
        final String filter = //
                "{" + //
                        "   \"and\": [" + //
                        "       {" + //
                        "           \"empty\": {" + //
                        "               \"field\": \"0001\"" + //
                        "           }" + //
                        "       }," + //
                        "       {" + //
                        "           \"eq\": {" + //
                        "               \"field\": \"0002\"," + //
                        "               \"value\": \"toto\"" + //
                        "           }" + //
                        "       }" + //
                        "   ]" + //
                        "}";
        // TODO to be continued...

    }

}
