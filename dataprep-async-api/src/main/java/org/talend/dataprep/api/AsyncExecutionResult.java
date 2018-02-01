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

package org.talend.dataprep.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Model the result of an asynchrone execution
 */
public class AsyncExecutionResult {

    /** When the asynchrone execution was finished. */
    private final long endDate = System.currentTimeMillis();

    /**
     * Result of the asynchrone execution
     */
    private Object result;

//    /** The full run content type. */
//    private String type;

    public AsyncExecutionResult() {
    }

    public AsyncExecutionResult(Object result) {
        this.result = result;
    }

//    public AsyncExecutionResult(String key, String value) {
//        addProperty(key, value);
//    }
//
//    public AsyncExecutionResult(final String type) {
//        this.type = type;
//    }
//
    public long getEndDate() {
        return endDate;
    }

    public Object getResult() {
        return result;
    }

    //
//    public String getType() {
//        return type;
//    }
//
//    public void setType(String type) {
//        this.type = type;
//    }
//
//    public void addProperty(String key, String value) {
//        properties.put(key, value);
//    }
//
//    public void removeProperty(String key) {
//        properties.remove(key);
//    }
//
//    public String getProperty(String key) {
//        return properties.get(key);
//    }
}
