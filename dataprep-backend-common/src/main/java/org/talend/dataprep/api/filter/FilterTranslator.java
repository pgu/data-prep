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

import static org.talend.dataprep.api.filter.SimpleFilterService.EQ;
import static org.talend.dataprep.transformation.actions.common.ImplicitParameters.FILTER;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.talend.daikon.exception.TalendRuntimeException;
import org.talend.dataprep.BaseErrorCodes;
import org.talend.dataprep.api.preparation.Action;
import org.talend.dataprep.api.preparation.PreparationMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Translate legacy JSON filters to TQL filters. */
public class FilterTranslator {

    private final ObjectMapper mapper = new ObjectMapper();;

    public void translateFiltersToTQL(final PreparationMessage preparation) {
        final List<Action> actions = preparation.getActions();
        if (CollectionUtils.isEmpty(actions)) {
            return;
        }
        for (Action action : actions) {
            final String filter = action.getParameters().get(FILTER.getKey());
            action.getParameters().put(FILTER.getKey(), translateToTQL(filter));
        }
    }

    private String translateToTQL(final String filter) {
        if (StringUtils.isBlank(filter) || !filter.startsWith("{")) {
            return filter;
        }
        try {
            final JsonNode root = mapper.reader().readTree(filter);
            final Iterator<JsonNode> children = root.elements();
            final JsonNode filterContent = children.next();
            final String columnId = extractColumnId(filterContent);
            final String value = extractRawValue(filterContent);

            final String operation = extractOperator(root);
            final String tqlOperation;
            switch (operation) {
            case EQ:
                tqlOperation = " = ";
                break;
            default:
                return null;
            }
            return columnId + tqlOperation + value;
        } catch (Exception e) {
            throw new TalendRuntimeException(BaseErrorCodes.UNABLE_TO_PARSE_FILTER, e);
        }
    }

    private String extractColumnId(JsonNode filterContent) {
        return filterContent.has("field") ? filterContent.get("field").asText() : null;
    }

    private String extractRawValue(JsonNode filterContent) {
        return filterContent.has("value") ? filterContent.get("value").asText() : null;
    }

    private String extractOperator(JsonNode root) {
        final Iterator<String> propertiesIterator = root.fieldNames();
        if (!propertiesIterator.hasNext()) {
            throw new UnsupportedOperationException("Unsupported query, empty filter definition: " + root.toString());
        }

        return propertiesIterator.next();
    }

}
