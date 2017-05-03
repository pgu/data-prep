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

package org.talend.dataprep.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import org.talend.daikon.hystrix.GenericCommand;
import org.talend.dataprep.api.preparation.Action;

import java.util.Collection;

/**
 * Base Hystrix command request for all DataPrep commands.
 *
 * @param <T> Command result type.
 */
public abstract class TDPGenericCommand<T> extends GenericCommand<T> {

    /** Hystrix group used for dataset related commands. */
    public static final HystrixCommandGroupKey DATASET_GROUP = HystrixCommandGroupKey.Factory.asKey("dataset");

    /** Hystrix group used for preparation related commands. */
    public static final HystrixCommandGroupKey PREPARATION_GROUP = HystrixCommandGroupKey.Factory.asKey("preparation");

    /** Hystrix group used for transformation related commands. */
    public static final HystrixCommandGroupKey TRANSFORM_GROUP = HystrixCommandGroupKey.Factory.asKey("transform");

    @Autowired
    protected ObjectMapper objectMapper;

    /** Transformation service URL. */
    @Value("${transformation.service.url:}")
    protected String transformationServiceUrl;

    /** Dataset service URL. */
    @Value("${dataset.service.url:}")
    protected String datasetServiceUrl;

    /** Preparation service URL. */
    @Value("${preparation.service.url:}")
    protected String preparationServiceUrl;

    protected TDPGenericCommand(HystrixCommandGroupKey group) {
        super(group);
    }

    /**
     * Serialize the actions to string.
     *
     * @param stepActions - map of couple (stepId, action)
     * @return the serialized actions
     */
    protected String serializeActions(final Collection<Action> stepActions) throws JsonProcessingException {
        return "{\"actions\": " + objectMapper.writeValueAsString(stepActions) + "}";
    }
}
