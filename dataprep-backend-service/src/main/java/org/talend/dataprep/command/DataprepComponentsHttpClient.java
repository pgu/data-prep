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
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.talend.daikon.exception.error.ErrorCode;
import org.talend.daikon.exception.json.JsonErrorCode;
import org.talend.dataprep.exception.ErrorCodeDto;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.TdpExceptionDto;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.security.Security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DataprepComponentsHttpClient {

    private static final Logger LOGGER = getLogger(DataprepComponentsHttpClient.class);

    @Autowired
    private Security security;

    @Autowired
    protected HttpClient client;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected ConversionService conversionService;

    public <T> T execute(HttpRequestBase request) {

        Map<HttpStatus, BiFunction<HttpRequestBase, HttpResponse, T>> behavior = new EnumMap<>(HttpStatus.class);
        Function<Exception, RuntimeException> onError = Defaults.passthrough();

        // update request header with security token
        String authenticationToken = security.getAuthenticationToken();
        if (StringUtils.isNotBlank(authenticationToken)) {
            request.addHeader(AUTHORIZATION, authenticationToken);
        }

        final HttpResponse response;
        try {
            LOGGER.trace("Requesting {} {}", request.getMethod(), request.getURI());
            response = client.execute(request);
        } catch (Exception e) {
            throw onError.apply(e);
        }
        Header[] commandResponseHeaders = response.getAllHeaders();

        HttpStatus status = HttpStatus.valueOf(response.getStatusLine().getStatusCode());

        // do we have a behavior for this status code (even an error) ?
        // if yes use it
        BiFunction<HttpRequestBase, HttpResponse, T> function = behavior.get(status);
        if (function != null) {
            try {
                return function.apply(request, response);
            } catch (Exception e) {
                throw onError.apply(e);
            }
        }

        // handle response's HTTP status
        if (status.is4xxClientError() || status.is5xxServerError()) {
            LOGGER.trace("request {} {} : response on error {}", request.getMethod(), request.getURI(), response.getStatusLine());
            // Http status >= 400 so apply onError behavior
            return callOnError(request, response, onError);
        } else {
            // Http status is not error so apply onError behavior
            return behavior.getOrDefault(status, missingBehavior()).apply(request, response);
        }
    }

    private <T> BiFunction<HttpRequestBase, HttpResponse, T> missingBehavior() {
        return (req, res) -> {
            LOGGER.error("Unable to process message for request {} (response code: {}).", req,
                    res.getStatusLine().getStatusCode());
            req.releaseConnection();
            return Defaults.<T> asNull().apply(req, res);
        };
    }

    private <T> T callOnError(HttpRequestBase req, HttpResponse res, Function<Exception, RuntimeException> onError) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("request on error {} -> {}", req.toString(), res.getStatusLine());
        }
        final int statusCode = res.getStatusLine().getStatusCode();
        String content = StringUtils.EMPTY;
        try {
            if (res.getEntity() != null) {
                content = IOUtils.toString(res.getEntity().getContent(), UTF_8);
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Error received {}", content);
            }
            TdpExceptionDto exceptionDto = objectMapper.readValue(content, TdpExceptionDto.class);
            TDPException cause = conversionService.convert(exceptionDto, TDPException.class);
            ErrorCode code = cause.getCode();
            if (code instanceof ErrorCodeDto) {
                ((ErrorCodeDto) code).setHttpStatus(statusCode);
            }
            throw onError.apply(cause);
        } catch (JsonProcessingException e) {
            LOGGER.debug("Cannot parse response content as JSON with content '" + content + "'", e);
            // Failed to parse JSON error, returns an unexpected code with returned HTTP code
            final TDPException exception = new TDPException(new JsonErrorCode() {

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
            });
            throw onError.apply(exception);
        } catch (IOException e) {
            LOGGER.error("Unexpected error message: {}", buildRequestReport(req, res));
            throw new TDPException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e);
        } finally {
            req.releaseConnection();
        }
    }

    public String buildRequestReport(HttpRequestBase req, HttpResponse res) {
        StringBuilder builder = new StringBuilder("{request:{\n");
        builder.append("uri:").append(req.getURI()).append(",\n");
        builder.append("request:").append(req.getRequestLine()).append(",\n");
        builder.append("method:").append(req.getMethod()).append(",\n");
        if (req instanceof HttpEntityEnclosingRequestBase) {
            try {
                builder.append("load:")
                        .append(IOUtils.toString(((HttpEntityEnclosingRequestBase) req).getEntity().getContent(), UTF_8))
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

}
