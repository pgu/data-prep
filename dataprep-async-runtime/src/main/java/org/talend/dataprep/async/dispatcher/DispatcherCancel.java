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

package org.talend.dataprep.async.dispatcher;

import static org.springframework.http.HttpStatus.OK;
import static org.talend.dataprep.command.Defaults.asNull;

import org.apache.http.client.methods.HttpDelete;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.talend.dataprep.command.GenericCommand;

@Scope("prototype")
@Component
class DispatcherCancel extends GenericCommand<Void> {

    @Value("${execution.executor.dispatcher.url}")
    private String dispatcherUrl;

    protected DispatcherCancel(String dispatcherExecutionId) {
        super(() -> "DISPATCHER");
        execute(() -> new HttpDelete(dispatcherUrl + "/api/jobs/" + dispatcherExecutionId));
        on(OK).then(asNull());
    }

}
