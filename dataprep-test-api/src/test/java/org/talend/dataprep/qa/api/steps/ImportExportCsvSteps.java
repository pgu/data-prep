package org.talend.dataprep.qa.api.steps;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.jbehave.core.annotations.AfterScenario;
import org.jbehave.core.annotations.BeforeScenario;
import org.jbehave.core.annotations.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.talend.dataprep.qa.api.util.StepsDefinition;

import java.util.ArrayList;
import java.util.List;

@StepsDefinition
public class ImportExportCsvSteps {

    @Autowired
    protected Environment environment;

    /**
     * Used to store created dataset inside TDP is order to delete them at the scenario end.
     */
    protected List<String> createdDataset = new ArrayList<>();

    @BeforeScenario
    public void initializeScenario() {
        System.out.println("### - @BeforeScenario");
    }

    @AfterScenario
    public void disposeScenario() {
        System.out.println("### - @AfterScenario");
    }

    public RequestSpecification given() {
        return RestAssured.given();
    }

    @When("I upload the dataset $filename with name $name")
    public void uploadDataset(String filename, String name) {
        // timestamped name

        System.out.println("### - ### - ### - ### - ### - ### - ### - ###");
        System.out.println("### - " + environment.toString());
        String tsName = name + System.currentTimeMillis();
//        Response response = RestAssured.given().body(ImportExportCsvSteps.class.getResourceAsStream(filename)).when().post();
//        response.then().statusCode(200);
    }


}
