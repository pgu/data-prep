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

import java.net.URI;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.talend.dataprep.async.AsyncExecutionMessage;
import org.talend.dataprep.command.GenericCommand;

import com.netflix.hystrix.HystrixCommandGroupKey;

@Component
@Scope("prototype")
public class AsyncGenericCommand extends GenericCommand<AsyncExecutionMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncGenericCommand.class);

    /**
     * Protected constructor.
     *
     * @param group the command group.
     */
    protected AsyncGenericCommand(HystrixCommandGroupKey group) {
        super(group);
        on(HttpStatus.ACCEPTED).then((req, res) -> {
            final Header[] locations = res.getHeaders("Location");
            final URI uri = req.getURI();
            final String forwardURL = uri.getScheme() + "://" + uri.getAuthority() + trimLocation(locations[0]);
            LOGGER.debug("Redirecting to '{}'...", forwardURL);

            AsyncForwardCommand asyncForwardCommand = context.getBean(AsyncForwardCommand.class, group, forwardURL);
            return asyncForwardCommand.execute();
        });
        on(HttpStatus.NO_CONTENT).then((req, resp) -> {
            resp.setStatusCode(HttpStatus.NO_CONTENT.value());
            return null;
        });
    }

    // Process URL and remove extra "/"
    private static String trimLocation(Header location) {
        if (location != null && location.getValue() != null) {
            return location.getValue().replace("//", "/");
        }
        return StringUtils.EMPTY;
    }

}
