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

package org.talend.dataprep.api.service.command.dataset;

import java.io.InputStream;

import org.apache.http.client.methods.HttpGet;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.talend.dataprep.command.TDPGenericCommand;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.DataSetErrorCodes;

import static org.talend.daikon.hystrix.Defaults.emptyStream;
import static org.talend.daikon.hystrix.Defaults.pipeStream;

/**
 * Command to list dataset supported encodings.
 */
@Component
@Scope("request")
public class DataSetGetEncodings extends TDPGenericCommand<InputStream> {

    /**
     * Constructor.
     */
    public DataSetGetEncodings() {
        super(TDPGenericCommand.DATASET_GROUP);
        execute(() -> new HttpGet(datasetServiceUrl + "/datasets/encodings"));
        onError(e -> new TDPException(DataSetErrorCodes.UNABLE_TO_LIST_SUPPORTED_ENCODINGS, e));
        on(HttpStatus.NO_CONTENT).then(emptyStream());
        on(HttpStatus.OK).then(pipeStream());
    }

}
