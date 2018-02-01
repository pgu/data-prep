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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.export.ExportParameters;
import org.talend.dataprep.api.preparation.Preparation;
import org.talend.dataprep.api.preparation.PreparationMessage;
import org.talend.dataprep.api.preparation.Step;
import org.talend.dataprep.cache.CacheKeyGenerator;
import org.talend.dataprep.cache.ContentCache;
import org.talend.dataprep.cache.TransformationCacheKey;
import org.talend.dataprep.command.preparation.PreparationDetailsGet;
import org.talend.dataprep.exception.TDPException;

import java.io.InputStream;

import static org.talend.daikon.exception.ExceptionContext.build;
import static org.talend.dataprep.exception.error.PreparationErrorCodes.UNABLE_TO_READ_PREPARATION;

@Component
public class PreparationCacheCondition implements ConditionalTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreparationCacheCondition.class);

    @Autowired
    private ContentCache contentCache;

    @Autowired
    private CacheKeyGenerator cacheKeyGenerator;

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected ObjectMapper mapper;

    @Override
    public boolean executeAsynchronously(Object... args) {

        // check pre-condition
        assert args != null;
        assert args.length == 1;
        assert args[0] instanceof ExportParameters;

        ExportParameters exportParameters = (ExportParameters) args[0];

        if("head".equalsIgnoreCase(exportParameters.getStepId())){
            //FIXME: dealing with "head" value should be done before this class
            Preparation preparation = getPreparation(exportParameters.getPreparationId(), exportParameters.getStepId());
            exportParameters.setStepId(getCleanStepId(preparation, exportParameters.getStepId()));
        }

        TransformationCacheKey cacheKey = cacheKeyGenerator.generateContentKey(exportParameters);

        return !contentCache.has(cacheKey);
    }

    /**
     * @param preparationId the wanted preparation id.
     * @param stepId the preparation step (might be different from head's to navigate through versions).
     * @return the preparation out of its id.
     */
    //TODO: factorize this code
    protected PreparationMessage getPreparation(String preparationId, String stepId) {
        if ("origin".equals(stepId)) {
            stepId = Step.ROOT_STEP.id();
        }
        final PreparationDetailsGet preparationDetailsGet = applicationContext.getBean(PreparationDetailsGet.class,
                preparationId, stepId);
        try (InputStream details = preparationDetailsGet.execute()) {
            return mapper.readerFor(PreparationMessage.class).readValue(details);
        } catch (Exception e) {
            LOGGER.error("Unable to read preparation {}", preparationId, e);
            throw new TDPException(UNABLE_TO_READ_PREPARATION, e, build().put("id", preparationId));
        }
    }

    /**
     * Return the real step id in case of "head" or empty
     * @param preparation The preparation
     * @param stepId The step id
     */
    //TODO: factorize this code
    protected String getCleanStepId(final Preparation preparation, final String stepId) {
        if (StringUtils.equals("head", stepId) || StringUtils.isEmpty(stepId)) {
            return preparation.getSteps().get(preparation.getSteps().size() - 1).id();
        }
        return stepId;
    }
}
