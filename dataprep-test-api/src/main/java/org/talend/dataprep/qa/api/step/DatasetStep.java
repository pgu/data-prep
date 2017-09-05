package org.talend.dataprep.qa.api.step;

import io.restassured.http.Header;
import io.restassured.response.Response;
import org.jbehave.core.annotations.AfterStory;
import org.jbehave.core.annotations.BeforeStory;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.jbehave.core.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.dataprep.qa.api.feature.Dataset;

import java.time.LocalDateTime;

import static org.hamcrest.CoreMatchers.hasItems;


@Component
public class DatasetStep extends TalendStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetStep.class);
    /**
     * TODO : choose if we separate Step declaration and Step implementation like in TDS (in this case we should use the {@link Dataset} feature.
     */
    @Autowired
    private Dataset dataset;

    @BeforeStory
    public void initializeStory() {
        initialize();
    }

    @AfterStory
    public void disposeStory() {
        context().getDatasetById().forEach((key, value) -> deleteDataSet(value));
        dispose();
    }

    @When("I upload the dataset $filename with name $name")
    public void uploadDataset(String filename, String name) throws Exception {
        dataset.uploadDataset(filename, name);
        String datasetName = name + "_" + LocalDateTime.now();

        Response response =
                given().header(new Header("Content-Type", "text/plain")).
                        body(IOUtils.toString(DatasetStep.class.getResourceAsStream(filename), true)).
                        when().
                        post(environment.getProperty("run.environment.url") + "/api/datasets?name=" + datasetName);

        response.then().
                statusCode(200);

        String id = IOUtils.toString(response.getBody().asInputStream(), true);
        context().getDatasetById().put(STORY_DATASET_UPLOADED_ID, id);
        context().getDatasetById().put(STORY_DATASET_UPLOADED_NAME, datasetName);
    }

    @Then("The uploaded dataset is present in datasets list")
    public void getDataSets() {
        given().
                when().
                get(environment.getProperty("run.environment.url") + "/api/datasets/").
                then().
                statusCode(200).
                body("id", hasItems(context().getDatasetById().get(STORY_DATASET_UPLOADED_ID)));
    }

    /**
     * Delete a given dataset.
     *
     * @param dataSetId the dataset to delete.
     */
    protected void deleteDataSet(String dataSetId) {
        given().
                when().
                delete(environment.getProperty("run.environment.url") + "/api/datasets/" + dataSetId).
                then().
                statusCode(200);
    }
}
