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

package org.talend.dataprep.api.service.command.preparation;

import static org.talend.daikon.hystrix.Defaults.asNull;
import static org.talend.dataprep.exception.error.APIErrorCodes.UNABLE_TO_DELETE_PREPARATION_CACHE;

import org.apache.http.client.methods.HttpDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.talend.dataprep.command.TDPGenericCommand;
import org.talend.dataprep.exception.TDPException;

@Component
@Scope("request")
public class CachePreparationEviction extends TDPGenericCommand<String> {
    private CachePreparationEviction(final String preparationId) {
        super(TRANSFORM_GROUP);
        execute(() -> new HttpDelete(transformationServiceUrl + "/preparation/" + preparationId + "/cache")); //$NON-NLS-1$
        onError(e ->  new TDPException(UNABLE_TO_DELETE_PREPARATION_CACHE, e));
        on(HttpStatus.OK).then(asNull());
    }
}
