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

package org.talend.dataprep.async.dispatcher;

import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.talend.dataprep.async.ManagedTaskExecutor;

/**
 * <p>
 * When {@link ConfigurationTaskExecutor} does <b>not</b> match (no multiple instance of {@link ManagedTaskExecutor
 * executors} in context), autowiring of executors fails.
 * </p>
 * <p>
 * This class ensures autowiring is correctly performed when {@link ConfigurationTaskExecutor} is not enabled.
 * </p>
 */
@Component
public class DefaultExecutorConfiguration implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        /*
         * This bean being @Primary all wrapped executors are *not* autowired by Spring. Code below ensures proper dependency
         * injection is done.
         */
        final ConfigurableApplicationContext applicationContext = event.getApplicationContext();
        final ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        final Map<String, ManagedTaskExecutor> beans = beanFactory.getBeansOfType(ManagedTaskExecutor.class);
        beans.replaceAll((s, managedTaskExecutor) -> {
            beanFactory.autowireBean(managedTaskExecutor);
            return managedTaskExecutor;
        });
    }

}
