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

import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Command designed to be used as delegate.
 */
@Service
public class CommandSupport extends GenericCommand {

    public <T> DataprepHttpClientDelegate.HttpCallResult<T> run(HttpCallConfiguration<T> configuration) {
        try{
            return dataprepHttpClientDelegate.run(configuration);
        } catch (Exception e) {
            throw decomposeException(e);
        }
    }

    @Override
    @Deprecated
    public HttpStatus getStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Header[] getCommandResponseHeaders() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    protected void onError(Function onError) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    protected void onErrorReturn(Function onError) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    protected void execute(Supplier call) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    protected void execute(HttpRequestBase call) {
        throw new UnsupportedOperationException();
    }

}
