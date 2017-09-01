package org.talend.dataprep.qa.api.features;

import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * TODO comment !!
 */
public class TalendFeature {

    @Autowired
    ApplicationContext applicationContext;

    public RequestSpecification given() {
        return io.restassured.RestAssured.given();
    }

}
