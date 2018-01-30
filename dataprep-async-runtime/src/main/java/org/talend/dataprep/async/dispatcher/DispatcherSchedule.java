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

import static org.talend.dataprep.command.Defaults.toJson;

import java.io.IOException;
import java.io.StringWriter;

import javax.annotation.PostConstruct;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.talend.dataprep.command.GenericCommand;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.CommonErrorCodes;

import com.fasterxml.jackson.databind.ObjectMapper;

@Scope("prototype")
@Component
class DispatcherSchedule extends GenericCommand<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DispatcherSchedule.class);

    @Value("${execution.executor.dispatcher.url}")
    private String dispatcherUrl;

    @Autowired
    private ObjectMapper mapper;

    private final Payload payload;

    protected DispatcherSchedule(Payload payload) {
        super(() -> "DISPATCHER");
        this.payload = payload;
    }

    @PostConstruct
    public void init() {
        onSuccess().then(toJson(objectMapper).andThen(n -> n.get("jobId").asText()));
        execute(() -> {
            try {
                final String postUrl = dispatcherUrl + "/api/jobs";
                LOGGER.debug("Post parameters to '{}'.", postUrl);
                final HttpPost post = new HttpPost(postUrl);
                post.setHeader(new BasicHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE));
                final StringWriter payloadAsString = new StringWriter();
                mapper.writerFor(Payload.class).writeValue(payloadAsString, payload);
                post.setEntity(new StringEntity(payloadAsString.toString()));

                return post;
            } catch (IOException e) {
                throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
            }
        });
    }
}
