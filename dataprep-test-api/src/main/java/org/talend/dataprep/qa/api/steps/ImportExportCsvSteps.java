package org.talend.dataprep.qa.api.steps;

import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Pending;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

public class ImportExportCsvSteps {

//    Given I am a pending step
//    And I am still pending step
//    When a good soul will implement me
//    Then I shall be happy

    @Given("step1")
    public void step1(){
        System.out.println("Step1");
    }

    @Given("step2")
    public void step2(){
        System.out.println("Step2");
    }

    @When("when1")
    public void when1(){
        System.out.println("When1");
    }

    @Then("then1")
    public void then1(){
        System.out.println("Then1");
    }

    @Then("then")
    public void then(){
        System.out.println("story2.then");
    }
}
