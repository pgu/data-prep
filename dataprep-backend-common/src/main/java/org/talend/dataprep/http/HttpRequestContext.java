package org.talend.dataprep.http;

import java.util.Collections;
import java.util.Enumeration;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * A helper class around {@link RequestContextHolder}: allow simple modifications on HTTP context without worrying
 * whether code is called in a web context or not.
 */
public class HttpRequestContext {

    public static Enumeration<String> parameters() {
        final RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null && attributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attributes).getRequest().getParameterNames();
        }
        return Collections.emptyEnumeration();
    }

    public static String parameter(String parameterName) {
        final RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null && attributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attributes).getRequest().getParameter(parameterName);
        }
        return StringUtils.EMPTY;
    }
}