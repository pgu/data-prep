//  ============================================================================
//  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
//  This source code is available under agreement available at
//  https://github.com/Talend/data-prep/blob/master/LICENSE
//
//  You should have received a copy of the agreement
//  along with this program; if not, write to Talend SA
//  9 rue Pages 92150 Suresnes, France
//
//  ============================================================================

package org.talend.dataprep.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncExecutionResult {

    /** This class' logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncExecutionResult.class);

    /**
     * Url where we can get the result of the asynchrone execution
     */
    private String downloadUrl;

    // for JSON Serialization
    public AsyncExecutionResult() {
    }

    public AsyncExecutionResult(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
