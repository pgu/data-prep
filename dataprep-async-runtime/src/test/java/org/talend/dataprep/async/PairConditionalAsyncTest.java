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

package org.talend.dataprep.async;

import org.springframework.stereotype.Component;
import org.talend.dataprep.async.conditional.ConditionalTest;

@Component
public class PairConditionalAsyncTest implements ConditionalTest {
    @Override
    public boolean executeAsynchronously(Object... args) {

        // check pre-condition
        assert args != null;

        assert args.length == 1;
        assert args[0] instanceof Integer;

        Integer index = (Integer) args[0];

        return (index & 1) == 0;
    }
}
