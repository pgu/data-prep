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

import static org.talend.dataprep.async.AsyncExecution.Status.FAILED;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.talend.dataprep.async.AsyncExecution;
import org.talend.dataprep.async.AsyncExecutionWatcher;
import org.talend.dataprep.exception.error.CommonErrorCodes;

class DispatcherExecutionWatcher implements AsyncExecutionWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DispatcherExecutionWatcher.class);

    private final String authenticationToken;

    /**
     * Create a new watcher for dispatcher execution.
     *
     * @param authenticationToken The authentication token to use when invoking Dispatcher service.
     */
    DispatcherExecutionWatcher(String authenticationToken) {
        this.authenticationToken = authenticationToken;
    }

    @Override
    public AsyncExecution watch(AsyncExecution asyncExecution, ApplicationContext context) {
        final String dispatchId = asyncExecution.getDispatchId();
        final String id = asyncExecution.getId();

        LOGGER.debug("Requesting statuses of execution '{}' (dispatcher id: '{}')", id, dispatchId);
        final Set<String> statuses = context.getBean(DispatcherGetStatuses.class, dispatchId, authenticationToken).execute();
        LOGGER.debug("Dispatcher statuses for execution '{}' (dispatcher id: '{}') is '{}'", id, dispatchId,
                Arrays.toString(statuses.toArray()));
        if (isJobFailed(statuses)) {
            // JOB_FAILED is the TPSVC's Dispatcher status for a failed execution.
            LOGGER.debug("Dispatcher failed to start full run, cancelling execution '{}'", id);
            asyncExecution.setStatus(FAILED);
            asyncExecution.setException(new RuntimeException("Dispatcher execution failed with statuses '" + Arrays.toString(statuses.toArray()) + "'."));
            asyncExecution.setError(CommonErrorCodes.UNEXPECTED_SERVICE_EXCEPTION);

            return asyncExecution;
        } else {
            LOGGER.debug("No change to full run execution. Keep watcher.", id);
            return null;
        }
    }

    private boolean isJobFailed(Set<String> statuses) {
        return statuses.contains("JOB_APPLICATION_DOWN") || statuses.contains("JOB_START_FAILURE")
                || statuses.contains("JOB_FAILED") || statuses.contains("CONTAINER_FAILED");
    }
}
