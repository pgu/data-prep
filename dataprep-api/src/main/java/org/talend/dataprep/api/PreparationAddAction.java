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

package org.talend.dataprep.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHeader;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.preparation.AppendStep;
import org.talend.dataprep.command.TDPGenericCommand;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.CommonErrorCodes;

import static org.talend.daikon.hystrix.Defaults.asNull;

@Component("PreparationAddActionv2")
@Scope("request")
public class PreparationAddAction extends TDPGenericCommand<Void> {

    /**
     * Default constructor.
     *
     * @param preparationId the preparation id.
     * @param steps         the step to append.
     */
    // private constructor to ensure the use of IoC
    private PreparationAddAction(final String preparationId, final List<AppendStep> steps) {
        super(PREPARATION_GROUP);
        execute(() -> onExecute(preparationId, steps));
        on(HttpStatus.OK).then(asNull());
    }

    private HttpRequestBase onExecute(String preparationId, List<AppendStep> steps) {
        try {
            final String stepAsString = objectMapper.writeValueAsString(steps);
            final InputStream stepInputStream = new ByteArrayInputStream(stepAsString.getBytes(StandardCharsets.UTF_8));

            final HttpPost actionAppend = new HttpPost(preparationServiceUrl + "/preparations/" + preparationId + "/actions");
            actionAppend.setHeader(new BasicHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE));
            actionAppend.setEntity(new InputStreamEntity(stepInputStream));

            return actionAppend;
        } catch (IOException e) {
            throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        }
    }
}
