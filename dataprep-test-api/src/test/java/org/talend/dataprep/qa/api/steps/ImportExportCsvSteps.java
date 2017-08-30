package org.talend.dataprep.qa.api.steps;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.jbehave.core.annotations.AfterScenario;
import org.jbehave.core.annotations.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

public class ImportExportCsvSteps {

//    Narrative:
//    As a user
//    I want to test calling dataprep rest API
//    I want to import a csv file as a dataset
//    I want to add a step
//    I want to  export the created preparation
//    So that I can achieve a integration test

    @Autowired
    protected Environment environment;

    /**
     * Used to store created dataset inside TDP is order to delete them at the scenario end.
     */
    protected List<String> createdDataset = new ArrayList<>();

    public RequestSpecification given() {
        return RestAssured.given();
    }

    @When("I upload the dataset $filename with name $name")
    public void uploadDataset(String filename, String name) {
        // timestamped name
        String tsName = name + System.currentTimeMillis();
        Response response = RestAssured.given().body(ImportExportCsvSteps.class.getResourceAsStream(filename)).when().post();
    }

    @AfterScenario
    public void cleanupDataset() {

    }
}
