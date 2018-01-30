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

import static org.talend.dataprep.async.AsyncExecution.Status.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.fullrun.AsyncExecutionResult;
//import org.talend.dataprep.api.fullrun.FullRunProgress;
//import org.talend.dataprep.api.fullrun.FullRunResultFactory;
import org.talend.dataprep.api.service.info.VersionService;
import org.talend.dataprep.async.AsyncExecution;
import org.talend.dataprep.async.ManagedTaskExecutor;
import org.talend.dataprep.async.repository.AsyncExecutionUpdaterRegistry;
import org.talend.dataprep.async.repository.ManagedTaskRepository;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.TransformationErrorCodes;
import org.talend.dataprep.security.Security;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A implementation of {@link ManagedTaskExecutor} dedicated to run of a full run using TPSVC's Dispatcher.
 *
 * @param <T> the type of managed tasks.
 */
@Component
@ConditionalOnProperty(name = "execution.executor.dispatcher")
public class RemoteManagedTaskExecutor<T extends AsyncExecutionResult> implements ManagedTaskExecutor<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteManagedTaskExecutor.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private VersionService versionService;

    @Autowired
    private ManagedTaskRepository repository;

    @Autowired
    private Security security;

    @Value("${preparation.service.url}")
    private String preparationServiceUrl;

    @Value("${dataset.service.url}")
    private String datasetServiceUrl;

    @Value("${async_store.service.url}")
    private String executionStoreRemoteUrl;

    private String dockerImage;

    @Value("${execution.executor.dispatcher.memorySoftLimit:512}")
    private int memorySoftLimit;

    @Value("${execution.executor.dispatcher.memoryHardLimit:1024}")
    private int memoryHardLimit;

    @Autowired
    private AsyncExecutionUpdaterRegistry asyncExecutionUpdaterRegistry;

    @Value("${execution.executor.dispatcher.dockerImage}")
    void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }

    @Override
    public AsyncExecution resume(ManagedTaskCallable<T> task, String executionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncExecution queue(ManagedTaskCallable<T> task, String groupId) {
        LOGGER.debug("Queuing task '{}' (group id: {}) using dispatcher...", task, groupId);

        // Create async execution
        final Optional<String> optional = Optional.ofNullable(groupId);
        AsyncExecution execution = optional.map(s -> new AsyncExecution(groupId)).orElseGet(AsyncExecution::new);
        execution.setUserId(security.getUserId());
        execution.setTenantId(security.getTenantId());
        execution.setStatus(RUNNING);
//        execution.setProgress();
        execution.getTime().setCreationDate(System.currentTimeMillis());
        execution.getTime().setStartDate(System.currentTimeMillis());
//        execution.setResult(FullRunResultFactory.createDataPrepFullRunResult());
        repository.save(execution);
        LOGGER.debug("Created execution '{}' for user id '{}' (tenant id: '{}')", execution.getId(), security.getUserId(),
                security.getTenantId());

        final Map<String, String> environment = buildEnvironment();

        if (LOGGER.isDebugEnabled()) {
            final StringBuilder environmentDump = new StringBuilder();
            for (Map.Entry<String, String> environmentEntry : environment.entrySet()) {
                environmentDump.append(environmentEntry.getKey()).append(": ").append(environmentEntry.getValue()).append('\n');
            }
            LOGGER.debug("Environment for execution: {}", environmentDump.toString());
        }

        Payload payload = buildPayload(task, execution, environment, groupId);

        if (LOGGER.isDebugEnabled()) {
            try {
                final StringWriter writer = new StringWriter();
                objectMapper.writerFor(Payload.class).writeValue(writer, payload);
                LOGGER.debug("Payload sent to dispatcher: {}", writer.toString());
            } catch (IOException e) {
                LOGGER.debug("Unable to show payload sent to dispatcher.", e);
            }
        }

        LOGGER.debug("Invoking dispatcher...");
        final String dispatchId = applicationContext.getBean(DispatcherSchedule.class, payload).execute();
        LOGGER.debug("Dispatcher invoked (dispatch id: '{}')", dispatchId);
        execution.setDispatchId(dispatchId);
        repository.save(execution); // And don't forget to save dispatch information

        // Watch for dispatch failures (e.g. unable to start full run instance)
        final String authenticationToken = security.getAuthenticationToken();
        asyncExecutionUpdaterRegistry.register(execution, new DispatcherExecutionWatcher(authenticationToken));

        return execution;
    }

    Map<String, String> buildEnvironment() {

        final Map<String, String> environment = new HashMap<>();
        environment.put("EXECUTION_STORE_URL", executionStoreRemoteUrl);
        copyProperty(environment, "iam.accounts.url", "ACCOUNT_URL");
        copyProperty(environment, "configuration.service.url", "CONFIG_URL");
        copyProperty(environment, "dataset.service.url", "DATASET_URL");
        copyProperty(environment, "preparation.service.url", "PREPARATION_URL");
        copyProperty(environment, "tcomp.server.url", "TCOMP_URL");
        copyProperty(environment, "security.oidc.client.expectedIssuer", "OIDC_URL");
        copyProperty(environment, "security.token.invalid-after", "SECURITY_TOKEN_INVALID_AFTER");
        copyProperty(environment, "security.oidc.client.keyUri", "OIDC_KEY_URI");
        copyProperty(environment, "security.oauth2.client.clientId", "SECURITY_CLIENT_ID");
        copyProperty(environment, "security.oauth2.client.clientSecret", "SECURITY_CLIENT_SECRET");
        copyProperty(environment, "security.oauth2.resource.tokenInfoUri", "OIDC_INFO_URI");
        copyProperty(environment, "iam.scim.url", "SCIM_URL");
        return environment;
    }

    Payload buildPayload(ManagedTaskCallable<T> task, AsyncExecution execution, Map<String, String> environment, String groupId) {
        final String selectedDockerImage = getDockerImage();
        final String dockerImageTag = getDockerImageTag();
        LOGGER.debug("Docker image selected for run is '{}'", selectedDockerImage);

        return new PayloadBuilder().setApplicationId("TDP") //
                .setApplicationService("fullrun") //
                .setApplicationVersion(dockerImageTag) //
                .setJobDescription("Full run of preparation #" + groupId) //
                .setEnvironment(environment) //
                .setDockerImage(selectedDockerImage) //
                .setDockerContainerHttpPort(3333) //
                .setMemoryReservationHardLimit(memoryHardLimit) //
                .setMemorySoftLimit(memorySoftLimit) //
                .setStartAction(new Payload.PayloadAction(HttpMethod.POST, "/apply/preparation/fullruns/" + execution.getId(),
                        toJobPayload(task))) //
                .setStopAction(new Payload.PayloadAction(HttpMethod.DELETE, "/preparation/fullruns/" + execution.getId(), null)) //
                .setStatusAction(new Payload.PayloadAction(HttpMethod.GET, "/status", null)) //
                .setHealthAction(new Payload.PayloadAction(HttpMethod.GET, "/health", null)) //
                .build();
    }


    String getDockerImageTag() {
        return StringUtils.substringAfterLast(dockerImage, ":");
    }

    // Extract only tag from injected value.
    private String getDockerImage() {
        final String tag = getDockerImageTag();
        final String fullImageName = "localhost:5000/talend/dataprep-ee-fullrun-dispatcher:" + tag;
        LOGGER.debug("Replacing '{}' with '{}' as Docker image name.", dockerImage, fullImageName);
        return fullImageName;
    }

    // Copy a property from environment to given map.
    private void copyProperty(Map<String, String> environment, String key, String dest) {
        environment.put(dest, applicationContext.getEnvironment().getProperty(key));
    }

    private String toJobPayload(ManagedTaskCallable<T> task) {
        try {
            final Object o = task.getArguments()[0];
            final StringWriter output = new StringWriter();
            objectMapper.writerFor(o.getClass()).writeValue(output, o);
            return output.toString();
        } catch (IOException e) {
            throw new TDPException(TransformationErrorCodes.UNABLE_TO_PERFORM_EXPORT, e);
        }
    }

    @Override
    public AsyncExecution cancel(String id) throws CancellationException {
        return interrupt(id, CANCELLED);
    }

    @Override
    public AsyncExecution stop(String id) {
        return interrupt(id, DONE);
    }

    private AsyncExecution interrupt(String id, AsyncExecution.Status newStatus) {
        LOGGER.debug("Cancel execution #{}", id);
        final AsyncExecution execution = repository.get(id);
        if (execution != null) {
            if (execution.getStatus() == DONE && newStatus == CANCELLED) {
                throw new CancellationException();
            }
            try {
                final String dispatchId = execution.getDispatchId();
                applicationContext.getBean(DispatcherCancel.class, dispatchId).execute();
            } catch (Exception e) {
                LOGGER.error("Cancel task {} exception.", id, e);
            } finally {
                execution.updateExecutionState(newStatus);
                repository.save(execution);
            }
        }
        return execution;
    }

}
