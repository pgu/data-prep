package org.talend.dataprep.qa.api.step;

import io.restassured.http.Header;
import io.restassured.response.Response;
import org.jbehave.core.annotations.When;
import org.jbehave.core.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class DataSetStep extends TalendStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSetStep.class);

//    protected void deleteDataSet(String dataSetId) {
//        given().when().
//                delete(environment.getProperty("run.environment.url") + "/api/datasets/" + dataSetId).
//                then().
//                statusCode(200);
//    }


    @When("I upload the dataset $filename with name $name")
//    @ToContext(value = "dataSetMetaDataId", retentionLevel = ToContext.RetentionLevel.STORY)
    public void uploadDataset(String filename, String name) throws Exception {
        String datasetName = name + System.currentTimeMillis();

        System.out.println("***********filename=" + filename);
        System.out.println("***********run.environment.url=" + environment.getProperty("run.environment.url"));

//        InputStream toto = DataSetStep.class.getResourceAsStream(filename);
//        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(toto))) {
//            System.out.println(buffer.lines().collect(Collectors.joining("\n")));
//        }catch (Exception exp){
//            exp.printStackTrace();
//        }
        Response response =
                given().header(new Header("Content-Type", "text/plain")).
                        body(DataSetStep.class.getResourceAsStream(filename)).
                        when().
                        post(environment.getProperty("run.environment.url") + "/api/datasets?name=" + datasetName);

        response.then().
                statusCode(200);

        String id = IOUtils.toString(response.getBody().asInputStream(), true);
        System.out.println(id);
//        dataSetMetaDataIds.add(id);
//        return id;
    }

//    @Then("I can get the dataset metadata")
//    public void getDataSetMetadata(@FromContext("dataSetMetaDataId") String dataSetMetaDataId) throws Exception {
//        // @formatter:off
//        given().
//                when().
//                get(environment.getProperty("run.environment.url") + "/api/datasets/" + dataSetMetaDataId).
//                then().
//                statusCode(200).
//                body("metadata.name", equalTo(name));
//        // @formatter:on
//    }
//
//    @Then("I can get the dataset metadata copy")
//    public void getDataSetMetadataCopy(@FromContext("dataSetMetaDataCopyId") String dataSetMetaDataId) throws Exception {
//        // @formatter:off
//        given().
//                when().
//                get(environment.getProperty("run.environment.url") + "/api/datasets/" + dataSetMetaDataId).
//                then().
//                statusCode(200).
//                body("metadata.name", equalTo(name));
//        // @formatter:on
//    }
//
//    @When("I update the dataset with $filename and a new name $name")
//    public void updateDataSet(@FromContext("dataSetMetaDataId") String dataSetMetaDataId, String filename, String name)
//            throws Exception {
//        this.name = name + System.currentTimeMillis();
//        // @formatter:off
//        Response response =
//                given().
//                        header(new Header("Content-Type", "text/plain")).
//                        body(DataSetStep.class.getResourceAsStream(filename)).
//                        when().
//                        put(environment.getProperty("run.environment.url") + "/api/datasets/" + dataSetMetaDataId + "?name=" + this.name);
//
//        response.
//                then().
//                statusCode(200).
//                body(Matchers.equalTo(""));
//        // @formatter:on
//    }
//
//    @When("I copy the dataset with the new $name")
//    @ToContext(value = "dataSetMetaDataCopyId", retentionLevel = ToContext.RetentionLevel.STORY)
//    public String copyDataSet(@FromContext("dataSetMetaDataId") String dataSetMetaDataId, String name) throws Exception {
//        this.name = name + System.currentTimeMillis();
//        // @formatter:off
//        Response response =
//                given().
//                        header(new Header("Content-Type", "text/plain")).
//                        when().
//                        post(environment.getProperty("run.environment.url") + "/api/datasets/" + dataSetMetaDataId + "/copy?name=" + this.name);
//        response.
//                then().
//                statusCode(200);
//        // @formatter:on
//        String id = IOUtils.toString(response.getBody().asInputStream(), true);
//        dataSetMetaDataIds.add(id);
//        return id;
//    }
//
//    @Then("The dataset is present in datasets list")
//    public void getDataSets(@FromContext("dataSetMetaDataId") String dataSetMetaDataId) {
//        // @formatter:off
//        given().
//                when().
//                get(environment.getProperty("run.environment.url") + "/api/datasets/").
//                then().
//                statusCode(200).
//                body("id", hasItems(dataSetMetaDataId));
//        // @formatter:on
//    }
}
