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

package org.talend.dataprep.api.service.command;

import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.talend.daikon.exception.error.ErrorCode;
import org.talend.dataprep.async.AsyncExecution;
import org.talend.dataprep.async.AsyncExecutionMessage;
import org.talend.dataprep.command.Defaults;
import org.talend.dataprep.command.GenericCommand;
import org.talend.dataprep.exception.TDPException;

import com.netflix.hystrix.HystrixCommandGroupKey;

@Component
@Scope("prototype")
public class AsyncForwardCommand extends GenericCommand<AsyncExecutionMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncForwardCommand.class);

    private final String forwardURL;

    protected AsyncForwardCommand(HystrixCommandGroupKey group, String forwardURL) {
        super(group);
        this.forwardURL = forwardURL;
        execute(() -> new HttpGet(forwardURL));
    }

    @PostConstruct
    public void init() {
        on(HttpStatus.OK).then( //
                Defaults.convertResponse(objectMapper, AsyncExecutionMessage.class).andThen(execution -> {
                    if (execution != null) {
                        Optional<ErrorCode> errorCode = Optional.ofNullable(execution.getError());
                        if (execution.getStatus() == AsyncExecution.Status.FAILED && errorCode.isPresent()) {
                            throw new TDPException(errorCode.get());
                        }
                        return execution;
                    } else {
                        LOGGER.debug("No execution status returned from '{}'.", forwardURL);
                        return new AsyncExecutionMessage();
                    }

                }) //
        );
    }
}
