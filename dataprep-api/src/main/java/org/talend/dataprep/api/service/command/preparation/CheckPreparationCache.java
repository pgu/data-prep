// ============================================================================
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.api.service.command.preparation;

import org.apache.http.client.methods.HttpGet;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.talend.dataprep.command.GenericCommand;

/**
 * Check if the cache of a preparation is available
 */
@Component
@Scope("request")
public class CheckPreparationCache extends GenericCommand<Boolean> {

    /**
     * Default constructor.
     *
     * @param preparationId The preparation id to get cache status
     */
    private CheckPreparationCache(String preparationId) {
        super(GenericCommand.TRANSFORM_GROUP);
        execute(() -> new HttpGet(transformationServiceUrl + "/preparation/" + preparationId + "/cache"));
        on(HttpStatus.NOT_FOUND).then((httpRequestBase, httpResponse) -> false);
        on(HttpStatus.NO_CONTENT).then((httpRequestBase, httpResponse) -> true);
    }

}
