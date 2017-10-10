/*
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * https://github.com/Talend/data-prep/blob/master/LICENSE
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package org.talend.dataprep.api.service.command.preparation;

import java.util.Collection;

import org.talend.dataprep.api.preparation.Action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class ActionsSerializer {

    private ActionsSerializer() {}

    /**
     * Serialize the actions to string.
     *
     * @param stepActions - map of couple (stepId, action)
     * @return the serialized actions
     */
    static String serializeActions(ObjectMapper mapper, final Collection<Action> stepActions) throws JsonProcessingException {
        return "{\"actions\": " + mapper.writeValueAsString(stepActions) + "}";
    }

}
