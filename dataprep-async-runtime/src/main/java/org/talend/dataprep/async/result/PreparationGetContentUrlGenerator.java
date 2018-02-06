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

import com.sun.jndi.toolkit.url.Uri;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.export.ExportParameters;
import org.talend.dataprep.async.AsyncExecutionResult;

import java.net.URI;

@Component
public class PreparationGetContentUrlGenerator implements ResultUrlGenerator {

    @Override
    public AsyncExecutionResult generateResultUrl(Object... args) {

        // check pre-condition
        assert args != null;
        assert args.length == 1;
        assert args[0] instanceof ExportParameters;

        ExportParameters param = (ExportParameters) args[0];

        URIBuilder builder = new URIBuilder();
        builder.setPath("/api/preparations/" + param.getPreparationId() + "/content");

        if(StringUtils.isNotEmpty(param.getStepId())){
            builder.setParameter("version", param.getStepId());
        }

        if(param.getFrom() != null){
            builder.setParameter("from", param.getFrom().name());
        }

        return new AsyncExecutionResult(builder.toString());
    }
}
