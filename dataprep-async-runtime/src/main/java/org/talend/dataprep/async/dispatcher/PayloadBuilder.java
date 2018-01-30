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

import java.util.Map;

public class PayloadBuilder {

    private String applicationId;

    private String applicationService;

    private String applicationVersion;

    private String jobDescription;

    private Map<String, String> environment;

    private Payload.PayloadAction startAction;

    private Payload.PayloadAction stopAction;

    private Payload.PayloadAction statusAction;

    private Payload.PayloadAction healthAction;

    private Payload.ContainerDefinition containerDefinition = new Payload.ContainerDefinition();

    public PayloadBuilder setApplicationId(String applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    public PayloadBuilder setApplicationService(String applicationService) {
        this.applicationService = applicationService;
        return this;
    }

    public PayloadBuilder setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
        return this;
    }

    public PayloadBuilder setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
        return this;
    }

    public PayloadBuilder setEnvironment(Map<String, String> environment) {
        this.environment = environment;
        return this;
    }

    public PayloadBuilder setDockerImage(String dockerImage) {
        this.containerDefinition.setImageName(dockerImage);
        return this;
    }

    public PayloadBuilder setDockerContainerHttpPort(int dockerContainerHttpPort) {
        this.containerDefinition.setHttpPort(dockerContainerHttpPort);
        return this;
    }

    public PayloadBuilder setMemorySoftLimit(int memorySoftLimit) {
        // "Memory reservation" is soft limit (minimal memory in order to get things going at a minimal level).
        this.containerDefinition.setMemoryReservation(memorySoftLimit);
        return this;
    }

    public PayloadBuilder setMemoryReservationHardLimit(int memoryReservationHardLimit) {
        // "Memory" is the hard limit (in term of AWS), the maximum memory we can use before AWS kills the container.
        this.containerDefinition.setMemory(memoryReservationHardLimit);
        return this;
    }

    public PayloadBuilder setStartAction(Payload.PayloadAction startAction) {
        this.startAction = startAction;
        return this;
    }

    public PayloadBuilder setStopAction(Payload.PayloadAction stopAction) {
        this.stopAction = stopAction;
        return this;
    }

    public PayloadBuilder setStatusAction(Payload.PayloadAction statusAction) {
        this.statusAction = statusAction;
        return this;
    }

    public PayloadBuilder setHealthAction(Payload.PayloadAction healthAction) {
        this.healthAction = healthAction;
        return this;
    }

    public Payload build() {
        return new Payload(applicationId, applicationService, applicationVersion, jobDescription, environment,
                containerDefinition, startAction, stopAction, statusAction, healthAction);
    }
}
