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

package org.talend.dataprep.async.conditional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.dataprep.cache.ContentCache;
import org.talend.dataprep.cache.ContentCacheKey;

/**
 * return TRUE if content is in the cache
 */
@Component
public class CacheCondition implements ConditionalTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheCondition.class);

    @Autowired
    private ContentCache contentCache;

    @Override
    public boolean apply(Object... args) {

        // check pre-condition
        assert args != null;
        assert args.length == 1;
        assert args[0] instanceof ContentCacheKey;

        ContentCacheKey cacheKey = (ContentCacheKey) args[0];

        return contentCache.has(cacheKey);
    }

}
