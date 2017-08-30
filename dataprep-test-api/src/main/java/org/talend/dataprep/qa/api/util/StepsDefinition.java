package org.talend.dataprep.qa.api.util;

import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.*;

//@Documented
//@Component
@ContextConfiguration(classes = AcceptanceTestsConfiguration.class)
@ActiveProfiles("tests")
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StepsDefinition {
}
