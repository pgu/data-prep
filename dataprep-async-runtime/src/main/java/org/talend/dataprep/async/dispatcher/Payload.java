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

import org.springframework.http.HttpMethod;

import com.fasterxml.jackson.annotation.JsonProperty;

class Payload {

    @JsonProperty("applicationId")
    private String applicationId;

    @JsonProperty("applicationService")
    private String applicationService;

    @JsonProperty("applicationVersion")
    private String applicationVersion;

    @JsonProperty("jobDescription")
    private String jobDescription;

    @JsonProperty("environment")
    private Map<String, String> environment;

    @JsonProperty("containerDefinition")
    private ContainerDefinition containerDefinition;

    @JsonProperty("jobPayload")
    private String jobPayLoad;

    @JsonProperty("startAction")
    private PayloadAction startAction;

    @JsonProperty("stopAction")
    private PayloadAction stopAction;

    @JsonProperty("statusAction")
    private PayloadAction statusAction;

    @JsonProperty("healthAction")
    private PayloadAction healthAction;

    // For deserialization purposes
    public Payload() {
    }

    Payload(String applicationId, //
            String applicationService, //
            String applicationVersion, //
            String jobDescription, //
            Map<String, String> environment, //
            ContainerDefinition containerDefinition, //
            PayloadAction startAction, //
            PayloadAction stopAction, //
            PayloadAction statusAction, //
            PayloadAction healthAction) {
        this.applicationId = applicationId;
        this.applicationService = applicationService;
        this.applicationVersion = applicationVersion;
        this.jobDescription = jobDescription;
        this.environment = environment;
        this.containerDefinition = containerDefinition;
        this.startAction = startAction;
        this.stopAction = stopAction;
        this.statusAction = statusAction;
        this.healthAction = healthAction;

        // This is temporary, dispatcher would get jobPayload from 'startAction'
        if (startAction != null) {
            this.jobPayLoad = startAction.jobPayload;
        }
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationService() {
        return applicationService;
    }

    public void setApplicationService(String applicationService) {
        this.applicationService = applicationService;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public PayloadAction getStartAction() {
        return startAction;
    }

    public void setStartAction(PayloadAction startAction) {
        this.startAction = startAction;
    }

    public PayloadAction getStopAction() {
        return stopAction;
    }

    public void setStopAction(PayloadAction stopAction) {
        this.stopAction = stopAction;
    }

    public PayloadAction getStatusAction() {
        return statusAction;
    }

    public void setStatusAction(PayloadAction statusAction) {
        this.statusAction = statusAction;
    }

    public PayloadAction getHealthAction() {
        return healthAction;
    }

    public void setHealthAction(PayloadAction healthAction) {
        this.healthAction = healthAction;
    }

    public ContainerDefinition getContainerDefinition() {
        return containerDefinition;
    }

    public void setContainerDefinition(ContainerDefinition containerDefinition) {
        this.containerDefinition = containerDefinition;
    }

    public static class PayloadAction {

        @JsonProperty("httpMethod")
        private HttpMethod httpMethod;

        @JsonProperty("url")
        private String url;

        @JsonProperty("jobPayload")
        private String jobPayload;

        // For deserialization purposes
        public PayloadAction() {
        }

        public PayloadAction(HttpMethod httpMethod, String url, String jobPayload) {
            this.httpMethod = httpMethod;
            this.url = url;
            this.jobPayload = jobPayload;
        }

        public HttpMethod getHttpMethod() {
            return httpMethod;
        }

        public void setHttpMethod(HttpMethod httpMethod) {
            this.httpMethod = httpMethod;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getJobPayload() {
            return jobPayload;
        }

        public void setJobPayload(String jobPayload) {
            this.jobPayload = jobPayload;
        }
    }

    public static class ContainerDefinition {

        @JsonProperty("imageName")
        private String imageName;

        @JsonProperty("httpPort")
        private int httpPort;

        @JsonProperty("memory")
        private int memory;

        @JsonProperty("memoryReservation")
        private int memoryReservation;

        // For deserialization purposes
        public ContainerDefinition() {
        }

        public String getImageName() {
            return imageName;
        }

        public void setImageName(String imageName) {
            this.imageName = imageName;
        }

        public int getHttpPort() {
            return httpPort;
        }

        public void setHttpPort(int httpPort) {
            this.httpPort = httpPort;
        }

        public int getMemory() {
            return memory;
        }

        public void setMemory(int memory) {
            this.memory = memory;
        }

        public int getMemoryReservation() {
            return memoryReservation;
        }

        public void setMemoryReservation(int memoryReservation) {
            this.memoryReservation = memoryReservation;
        }
    }
}
