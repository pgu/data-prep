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

import static java.util.Collections.singleton;
import static org.springframework.http.HttpStatus.OK;
import static org.talend.dataprep.command.Defaults.toJson;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.talend.dataprep.command.GenericCommand;
import org.talend.dataprep.security.Security;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Returns all unique statuses for a given dispatch execution.
 */
@Scope("prototype")
@Component
class DispatcherGetStatuses extends GenericCommand<Set<String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DispatcherGetStatuses.class);

    private final String dispatcherExecutionId;

    @Autowired
    private Security security;

    @Value("${execution.executor.dispatcher.url}")
    private String dispatcherUrl;

    private String authenticationToken;

    protected DispatcherGetStatuses(String dispatcherExecutionId, String authenticationToken) {
        super(() -> "DISPATCHER");
        if (dispatcherExecutionId == null) {
            LOGGER.error("Dispatcher execution cannot be null.");
        }
        this.authenticationToken = authenticationToken;
        this.dispatcherExecutionId = dispatcherExecutionId;
        execute(() -> new HttpGet(dispatcherUrl + "/api/jobs/" + dispatcherExecutionId + "/history"));

        // Safety for communication issues with Dispatcher -> set DP job as failed.
        onUserErrors().then((httpRequestBase, httpResponse) -> singleton("JOB_FAILED"));
        onServerErrors().then((httpRequestBase, httpResponse) -> singleton("JOB_FAILED"));
    }

    @Override
    public String getAuthenticationToken() {
        return authenticationToken;
    }

    @PostConstruct
    public void init() {
        on(OK).then(toJson(objectMapper).andThen(jsonNode -> {
            final Iterator<JsonNode> elements = jsonNode.elements();
            Set<String> statuses = new HashSet<>();
            while (elements.hasNext()) {
                final JsonNode element = elements.next();
                final Optional<JsonNode> status = Optional.ofNullable(element.get("status"));
                if (status.isPresent()) {
                    statuses.add(status.get().asText());
                } else {
                    LOGGER.error("Unable to find status in '{}'.", element);
                }
            }
            LOGGER.debug("{} status found for execution '{}'", statuses.size(), dispatcherExecutionId);
            return statuses;
        }));
    }

}
