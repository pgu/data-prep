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

package org.talend.dataprep.async.result;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.stereotype.Component;
import org.talend.dataprep.async.AsyncExecutionResult;

@Component
public class PrepMetadataGetContentUrlGenerator implements ResultUrlGenerator {

    @Override
    public AsyncExecutionResult generateResultUrl(Object... args) {

        // check pre-condition
        assert args != null;
        assert args.length == 2;
        assert args[0] instanceof String;
        assert args[1] instanceof String;

        String preparationId = (String) args[0];
        String headId = (String) args[1];

        URIBuilder builder = new URIBuilder();
        builder.setPath("/api/preparations/" + preparationId + "/metadata");

        if (StringUtils.isNotEmpty(headId)) {
            builder.setParameter("version", headId);
        }

        return new AsyncExecutionResult(builder.toString());
    }
}
