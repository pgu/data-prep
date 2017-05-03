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

package org.talend.dataprep.api.service.command.folder;

import static org.talend.daikon.hystrix.Defaults.pipeStream;

import java.io.InputStream;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.talend.dataprep.command.TDPGenericCommand;

/**
 * List all folders in a tree representation
 */
@Component
@Scope("prototype")
public class FolderTree extends TDPGenericCommand<InputStream> {

    /**
     * Get all the folders in a tree representation
     */
    public FolderTree() {
        super(TDPGenericCommand.PREPARATION_GROUP);
        execute(this::onExecute);
        on(HttpStatus.OK).then(pipeStream());
    }

    private HttpRequestBase onExecute() {
        return new HttpGet(preparationServiceUrl + "/folders/tree");
    }

}
