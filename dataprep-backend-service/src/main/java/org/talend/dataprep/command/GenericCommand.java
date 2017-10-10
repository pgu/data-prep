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

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.talend.dataprep.exception.TDPException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

/**
 * Base Hystrix command request for all DataPrep commands.
 *
 * @param <T> Command result type.
 */
public class GenericCommand<T> extends HystrixCommand<T> {

    protected static final String DATASET_GROUP_KEY = "dataset";

    /** Hystrix group used for dataset related commands. */
    protected static final HystrixCommandGroupKey DATASET_GROUP = HystrixCommandGroupKey.Factory.asKey(DATASET_GROUP_KEY);

    protected static final String PREPARATION_GROUP_KEY = "preparation";

    /** Hystrix group used for preparation related commands. */
    protected static final HystrixCommandGroupKey PREPARATION_GROUP = HystrixCommandGroupKey.Factory.asKey(PREPARATION_GROUP_KEY);

    protected static final String TRANSFORM_GROUP_KEY = "transform";

    /** Hystrix group used for transformation related commands. */
    protected static final HystrixCommandGroupKey TRANSFORM_GROUP = HystrixCommandGroupKey.Factory.asKey(TRANSFORM_GROUP_KEY);

    protected static final String FULLRUN_GROUP_KEY = "fullrun";

    /** Hystrix group used for transformation related commands. */
    protected static final HystrixCommandGroupKey FULLRUN_GROUP = HystrixCommandGroupKey.Factory.asKey(FULLRUN_GROUP_KEY);

    public static final String VERSION_GROUP_KEY = "version";

    protected static final String ERRORS_GROUP_KEY = "errors";

    private static final HystrixCommandGroupKey DEFAULT_GROUP = HystrixCommandGroupKey.Factory.asKey("default");

    private static final HttpStatus[] SUCCESS_STATUS = Stream.of(HttpStatus.values()) //
            .filter(HttpStatus::is2xxSuccessful) //
            .collect(Collectors.toList()) //
            .toArray(new HttpStatus[0]);

    private static final HttpStatus[] REDIRECT_STATUS = Stream.of(HttpStatus.values()) //
            .filter(HttpStatus::is3xxRedirection) //
            .collect(Collectors.toList()) //
            .toArray(new HttpStatus[0]);

    private static final HttpStatus[] INFO_STATUS = Stream.of(HttpStatus.values()) //
            .filter(HttpStatus::is1xxInformational) //
            .collect(Collectors.toList()) //
            .toArray(new HttpStatus[0]);

    private static final HttpStatus[] USER_ERROR_STATUS = Stream.of(HttpStatus.values()) //
            .filter(HttpStatus::is4xxClientError) //
            .collect(Collectors.toList()) //
            .toArray(new HttpStatus[0]);

    private static final HttpStatus[] SERVER_ERROR_STATUS = Stream.of(HttpStatus.values()) //
            .filter(HttpStatus::is5xxServerError) //
            .collect(Collectors.toList()) //
            .toArray(new HttpStatus[0]);

    /** Jackson object mapper to handle json. */
    @Autowired
    protected ObjectMapper objectMapper;

    /** Spring application context. */
    @Autowired
    protected ApplicationContext context;

    /** Transformation service URL. */
    @Value("${transformation.service.url:}")
    protected String transformationServiceUrl;

    /** Full run service URL. */
    @Value("${fullrun.service.url:}")
    protected String fullRunServiceUrl;

    /** Dataset service URL. */
    @Value("${dataset.service.url:}")
    protected String datasetServiceUrl;

    /** Preparation service URL. */
    @Value("${preparation.service.url:}")
    protected String preparationServiceUrl;

    @Autowired
    protected DataprepHttpClientDelegate dataprepHttpClientDelegate;

    // config render the class stateful but it can be easily refactored with IDE tools (#inline)
    private final HttpCallConfiguration<T> configuration = new HttpCallConfiguration<>();

    // this (and config) is what render commands stateful. If we could stop using it it would be so great!
    private DataprepHttpClientDelegate.HttpCallResult<T> callResult;

    /** For commands migrated to AOP that do not need group in constructor. */
    protected GenericCommand() {
        super(DEFAULT_GROUP);
    }

    /**
     * Protected constructor.
     *
     * @param group the command group.
     */
    protected GenericCommand(final HystrixCommandGroupKey group) {
        super(group);
    }

    @Override
    protected RuntimeException decomposeException(Exception e) {
        Throwable current = e;
        while (current.getCause() != null) {
            if (current instanceof TDPException) {
                break;
            }
            current = current.getCause();
        }
        if (current instanceof TDPException) {
            return (TDPException) current;
        } else {
            return super.decomposeException(e);
        }
    }

    /**
     * Runs a data prep command with the following steps:
     * <ul>
     * <li>Gets the HTTP command to execute (see {@link #execute(Supplier)}.</li>
     * <li>Gets the behavior to adopt based on returned HTTP code (see {@link #on(HttpStatus...)}).</li>
     * <li>If no behavior was defined for returned code, returns an error as defined in {@link #onErrorThrow(Function)}</li>
     * <li>If a behavior was defined, invokes defined behavior.</li>
     * </ul>
     *
     * @return A instance of <code>T</code>.
     * @throws Exception If command execution fails.
     */
    @Override
    protected T run() {
        try{
            callResult = dataprepHttpClientDelegate.run(configuration);
            return callResult.getResult();
        } catch (Exception e) {
            throw decomposeException(e);
        }
    }

    /**
     * Headers of the response received by the command. Set in the run command.
     *
     * @return the CommandResponseHeader
     */
    public Header[] getCommandResponseHeaders() {
        return callResult.getCommandResponseHeaders();
    }

    /**
     * The HTTP status of the executed request.
     */
    public HttpStatus getStatus() {
        return callResult.getHttpStatus();
    }

    /**
     * Declares what exception should be thrown in case of error. Will replace any {@link #onError(Function)} set.
     *
     * @param onError A {@link Function function} that returns a {@link RuntimeException}.
     * @see TDPException
     */
    protected void onErrorThrow(Function<Exception, RuntimeException> onError) {
        configuration.onError(e -> {
            throw onError.apply(e);
        });
    }

    /**
     * Declares what value should be returned in case of error. Will replace any {@link #onErrorThrow(Function)} set.
     *
     * @param onError A {@link Function function} that returns the type searched.
     * @see #onErrorThrow(Function)
     */
    protected void onError(Function<Exception, T> onError) {
        configuration.onError(onError);
    }

    /**
     * Declares which {@link HttpRequestBase http request} to execute in command.
     *
     * @param call The {@link Supplier} to provide the {@link HttpRequestBase} to execute.
     * @deprecated use {@link #execute(HttpRequestBase)}
     */
    @Deprecated
    protected void execute(Supplier<HttpRequestBase> call) {
        configuration.execute(call);
    }

    /**
     * Declares which {@link HttpRequestBase http request} to execute in command.
     *
     * @param call The {@link HttpRequestBase} to execute.
     */
    protected void execute(HttpRequestBase call) {
        configuration.execute(call);
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code <code>status</code>.
     *
     * @param status One of more HTTP {@link HttpStatus status(es)}.
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder on(HttpStatus... status) {
        return new BehaviorBuilder(status);
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code of 1xx.
     *
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder onInfo() {
        return on(INFO_STATUS);
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code of 2xx.
     *
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder onSuccess() {
        return on(SUCCESS_STATUS);
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code of 4xx.
     *
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder onUserErrors() {
        return on(USER_ERROR_STATUS);
    }

    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code of 5xx.
     *
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder onServerErrors() {
        return on(SERVER_ERROR_STATUS);
    }


    /**
     * Starts declaration of behavior(s) to adopt when HTTP response has status code of 3xx.
     *
     * @return A {@link BehaviorBuilder builder} to continue behavior declaration for the HTTP status(es).
     * @see BehaviorBuilder#then(BiFunction)
     */
    protected BehaviorBuilder onRedirect() {
        return on(REDIRECT_STATUS);
    }

    // A intermediate builder for behavior definition.
    protected class BehaviorBuilder {

        private final HttpStatus[] status;

        public BehaviorBuilder(HttpStatus[] status) {
            this.status = status;
        }

        /**
         * Declares what action should be performed for the given HTTP status(es).
         *
         * @param action A {@link BiFunction function} to be executed for given HTTP status(es).
         * @see Defaults
         */
        public void then(BiFunction<HttpRequestBase, HttpResponse, T> action) {
            configuration.on(status).then(action);
        }
    }

}
