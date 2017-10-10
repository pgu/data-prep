/*
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * https://github.com/Talend/data-prep/blob/master/LICENSE
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package org.talend.dataprep.command;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.http.HttpStatus;

public class HttpCallConfiguration<T> {

    public static <T> HttpCallConfiguration<T> call() {
        return new HttpCallConfiguration<>();
    }

    private Supplier<HttpRequestBase> httpRequestBase;

    private Map<HttpStatus, BiFunction<HttpRequestBase, HttpResponse, T>> behavior = new EnumMap<>(HttpStatus.class);

    private Function<Exception, T> onError = e -> {throw Defaults.passthrough().apply(e);};

    /**
     * Supply a {@link HttpRequestBase} through a java 8 {@link Supplier}. The request creation will be done when the task is executed.
     *
     * @deprecated This allow initialization of supplier in constructors, before Spring autowiring. It is not the preferred way
     * with Hystrix use through AOP. Please use {@link #execute(HttpRequestBase)}.
     */
    // Using supplier allow the creation of the HTTP request AFTER the object containing its code has been initialized with Spring context.
    // Still it is not how it should be done and the other execute method should be privileged.
    @Deprecated
    public HttpCallConfiguration<T> execute(Supplier<HttpRequestBase> httpRequestBaseSupplier) {
        httpRequestBase = httpRequestBaseSupplier;
        return this;
    }

    /**
     * Set the request to be executed.
     */
    public HttpCallConfiguration<T> execute(HttpRequestBase httpRequestBase) {
        this.httpRequestBase = () -> httpRequestBase;
        return this;
    }

    public HttpCallConfiguration<T> onError(Function<Exception, T> onError) {
        this.onError = onError;
        return this;
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code <code>status</code>.
     *
     * @param status One of more HTTP {@link HttpStatus status(es)}.
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    public BehaviorBuilder<T> on(HttpStatus... status) {
        return new BehaviorBuilder<>(status, this);
    }

    public HttpRequestBase getHttpRequestBase() {
        return httpRequestBase.get();
    }

    public Map<HttpStatus, BiFunction<HttpRequestBase, HttpResponse, T>> getBehavior() {
        return behavior;
    }

    public Function<Exception, T> getOnError() {
        return onError;
    }

    public static final class BehaviorBuilder<T> {

        private final HttpStatus[] status;

        private final HttpCallConfiguration<T> configuration;

        public BehaviorBuilder(HttpStatus[] status, HttpCallConfiguration<T> configuration) {
            this.status = status;
            this.configuration = configuration;
        }

        /**
         * Declares what action should be performed for the given HTTP status(es).
         *
         * @param action A {@link BiFunction function} to be executed for given HTTP status(es).
         * @see Defaults
         */
        public HttpCallConfiguration<T> then(BiFunction<HttpRequestBase, HttpResponse, T> action) {
            for (HttpStatus currentStatus : status) {
                configuration.behavior.put(currentStatus, action);
            }
            return configuration;
        }
    }

}
