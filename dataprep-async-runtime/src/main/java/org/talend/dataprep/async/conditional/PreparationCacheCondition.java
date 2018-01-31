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

package org.talend.dataprep.async.conditional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.export.ExportParameters;
import org.talend.dataprep.cache.CacheKeyGenerator;
import org.talend.dataprep.cache.ContentCache;
import org.talend.dataprep.cache.TransformationCacheKey;

@Component
public class PreparationCacheCondition implements ConditionalTest {


    @Autowired
    private ContentCache contentCache;

    @Autowired
    private CacheKeyGenerator cacheKeyGenerator;

    @Override
    public boolean executeAsynchronously(Object... args) {

        // check pre-condition
        assert args != null;
        assert args.length == 1;
        assert args[0] instanceof ExportParameters;

        ExportParameters exportParameters = (ExportParameters) args[0];

        TransformationCacheKey cacheKey = cacheKeyGenerator.generateContentKey(exportParameters);

        return contentCache.has(cacheKey);
    }
}
