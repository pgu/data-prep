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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpHeaders.AUTHORIZATION;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.talend.daikon.exception.error.ErrorCode;
import org.talend.daikon.exception.json.JsonErrorCode;
import org.talend.dataprep.conversions.BeanConversionService;
import org.talend.dataprep.exception.ErrorCodeDto;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.TdpExceptionDto;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.security.Security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service to facilitate HTTP call between data prep components. It help read answers and fallback on HTTP status code
 * or exceptions.
 */
@Service
public class DataprepHttpClientDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataprepHttpClientDelegate.class);

    @Autowired
    private HttpClient client;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private BeanConversionService conversionService;

    @Autowired
    private Security security;

    /**
     * Runs a data prep command with the following steps:
     *
     * @return A instance of <code>T</code>.
     */
    public <T> HttpCallResult<T> execute(HttpCallConfiguration<T> configuration) {
        final HttpRequestBase request = configuration.getHttpRequestBase();
        // update request header with security token if needed
        addSecurityToken(request);

        final HttpResponse response;
        try {
            LOGGER.trace("Requesting {} {}", request.getMethod(), request.getURI());
            response = client.execute(request);
        } catch (Exception e) {
            return handleUnexpectedError(configuration, null, null, e);
        }

        HttpStatus status = HttpStatus.valueOf(response.getStatusLine().getStatusCode());

        BiFunction<HttpRequestBase, HttpResponse, T> handler = //
                getResponseHandlingFunction(configuration, request, response, status);

        Header[] commandResponseHeaders = response.getAllHeaders();

        // application of handler must be able to throw exception on purpose without being bothered by onError wrapping
        T result;
        result = handler.apply(request, response);
        return new HttpCallResult<>(result, status, commandResponseHeaders);
    }

    private <T> void addSecurityToken(HttpRequest request) {
        String authenticationToken = security.getAuthenticationToken();
        if (request.getHeaders(AUTHORIZATION).length == 0) {
            if (StringUtils.isNotBlank(authenticationToken)) {
                request.addHeader(AUTHORIZATION, authenticationToken);
            } else {
                // Intentionally left as debug to prevent log flood in open source edition.
                LOGGER.debug("No current authentication token for {}.", request);
            }
        } else {
            // Intentionally left as debug to prevent log flood in open source edition.
            LOGGER.debug("Authentication token already present for {}.", request);
        }
    }

    private <T> BiFunction<HttpRequestBase, HttpResponse, T> getResponseHandlingFunction(HttpCallConfiguration<T> configuration,
                                                                                         HttpUriRequest request,
                                                                                         HttpResponse response, HttpStatus status) {
        // do we have a behavior for this status code (even an error) ?
        BiFunction<HttpRequestBase, HttpResponse, T> function = configuration.getBehaviorForStatus(status);
        if (function == null) {
            // handle response's HTTP status
            if (status.is4xxClientError() || status.is5xxServerError()) {
                LOGGER.trace("request {} {} : response on error {}", request.getMethod(), request.getURI(),
                        response.getStatusLine());
                // Http status >= 400 so apply onError behavior

                function = handleRemoteServerHttpError(configuration.getOnError());
            } else {
                // Http status is not error so apply onError behavior
                function = missingBehavior();
            }
        }
        return function;
    }

    /**
     * @return A {@link BiFunction} to handle missing behavior definition for HTTP response's code.
     */
    private static <T> BiFunction<HttpRequestBase, HttpResponse, T> missingBehavior() {
        return (req, res) -> {
            LOGGER.error("Unable to process message for request {} (response code: {}).", req,
                    res.getStatusLine().getStatusCode());
            return null;
        };
    }

    private <T> HttpCallResult<T> handleUnexpectedError(HttpCallConfiguration<T> configuration, HttpStatus status,
                                                        Header[] commandResponseHeaders, Exception e) {
        Function<Exception, T> onError = configuration.getOnError();
        if (onError != null) {
            return new HttpCallResult<>(onError.apply(e), status, commandResponseHeaders);
        } else {
            throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        }
    }

    private <T> BiFunction<HttpRequestBase, HttpResponse, T> handleRemoteServerHttpError(Function<Exception, T> onError) {
        return (httpRequestBase, httpResponse) -> handleRemoteServerHttpError(onError, httpRequestBase, httpResponse);
    }

    private <T> T handleRemoteServerHttpError(Function<Exception, T> onError, HttpUriRequest request, HttpResponse response) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("request on error {} -> {}", request.toString(), response.getStatusLine());
        }
        final int statusCode = response.getStatusLine().getStatusCode();
        String content = StringUtils.EMPTY;
        TDPException exception;
        try {
            if (response.getEntity() != null) {
                content = IOUtils.toString(response.getEntity().getContent(), UTF_8);
                LOGGER.trace("Error received {}", content);
                TdpExceptionDto exceptionDto = objectMapper.readValue(content, TdpExceptionDto.class);
                exception = conversionService.convert(exceptionDto, TDPException.class);
                ErrorCode code = exception.getCode();
                if (code instanceof ErrorCodeDto) {
                    ((ErrorCodeDto) code).setHttpStatus(statusCode);
                }
            } else {
                LOGGER.trace("Error received with no payload.");
                exception = new TDPException(new UnexpectedErrorCode(statusCode));
            }
        } catch (JsonProcessingException e) {
            LOGGER.debug("Cannot parse response content as JSON with content '" + content + "'", e);
            // Failed to parse JSON error, returns an unexpected code with returned HTTP code
            exception = new TDPException(new UnexpectedErrorCode(statusCode));
        } catch (IOException e) {
            LOGGER.error("Unexpected error message: {}", buildRequestReport(request, response));
            throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        }
        return onError.apply(exception);
    }

    private static String buildRequestReport(HttpUriRequest req, HttpResponse res) {
        StringBuilder builder = new StringBuilder("{request:{\n");
        builder.append("uri:").append(req.getURI()).append(",\n");
        builder.append("request:").append(req.getRequestLine()).append(",\n");
        builder.append("method:").append(req.getMethod()).append(",\n");
        if (req instanceof HttpEntityEnclosingRequestBase) {
            try {
                builder.append("load:")
                        .append(IOUtils.toString(((HttpEntityEnclosingRequestBase) req).getEntity().getContent(),
                                UTF_8))
                        .append(",\n");
            } catch (IOException e) {
                // We ignore the field
            }
        }
        builder.append("}, response:{\n");
        try {
            builder.append(IOUtils.toString(res.getEntity().getContent(), UTF_8));
        } catch (IOException e) {
            // We ignore the field
        }
        builder.append("}\n}");
        return builder.toString();
    }

    public static class HttpCallResult<T> {

        private final T result;

        private final HttpStatus httpStatus;

        /** Headers of the response received by the command. Set in the run command. */
        private final Header[] commandResponseHeaders;

        public HttpCallResult(T result, HttpStatus httpStatus, Header[] commandResponseHeaders) {
            this.result = result;
            this.httpStatus = httpStatus;
            this.commandResponseHeaders = commandResponseHeaders;
        }

        public T getResult() {
            return result;
        }

        public HttpStatus getHttpStatus() {
            return httpStatus;
        }

        public Header[] getCommandResponseHeaders() {
            return commandResponseHeaders;
        }
    }

    private static class UnexpectedErrorCode extends JsonErrorCode {

        private final int statusCode;

        UnexpectedErrorCode(int statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public String getProduct() {
            return CommonErrorCodes.UNEXPECTED_EXCEPTION.getProduct();
        }

        @Override
        public String getCode() {
            return CommonErrorCodes.UNEXPECTED_EXCEPTION.getCode();
        }

        @Override
        public int getHttpStatus() {
            return statusCode;
        }
    }
}
