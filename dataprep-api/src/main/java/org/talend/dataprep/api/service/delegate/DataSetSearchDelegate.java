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

package org.talend.dataprep.api.service.delegate;

import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.talend.daikon.annotation.Client;
import org.talend.dataprep.dataset.service.UserDataSetMetadata;
import org.talend.services.tdp.dataset.IDataSetService;

/**
 * A {@link SearchDelegate} implementation to search in datasets.
 */
@Component
public class DataSetSearchDelegate extends AbstractSearchDelegate<UserDataSetMetadata> {

    @Client
    private IDataSetService dataSetService;

    @Override
    public String getSearchCategory() {
        return "datasets";
    }

    @Override
    public String getSearchLabel() {
        return "datasets";
    }

    @Override
    public String getInventoryType() {
        return "dataset";
    }

    @Override
    public Stream<UserDataSetMetadata> search(String query, boolean strict) {
        return dataSetService.search(query, strict);
    }
}
