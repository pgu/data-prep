package org.talend.dataprep.qa.api.steps;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.dataprep.qa.api.features.Datasets;

import java.io.IOException;

/**
 * TODO : Comment !
 */
@Component
public class DatasetSteps extends TalendStep {

    @Autowired
    Datasets datasets;

    @When("I upload the dataset $filename with name $name")
    public void uploadDataset(@Named("filename") String filename, @Named("name") String name) {
        System.out.println(this.getClass().getName() + ".uploadDataset(" + filename + "," + name + ")");
        try {
            datasets.uploadDataset(filename, name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
