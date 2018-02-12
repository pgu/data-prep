@OnPremise
Feature: Dataset cache features

  @CleanAfter
  Scenario: Clean preparation dataset when updating a dataset used by a preparation
    Given I upload the dataset "/data/8L3C.csv" with name "8L3C_dataset"
    And I create a preparation with name "8L3C_preparation", based on "8L3C_dataset" dataset
    And I add a "uppercase" step on the preparation "8L3C_preparation" with parameters :
      | column_name      | firstname        |
      | column_id        | 0001             |
    When I update the dataset named "8L3C_dataset" with data "/data/10L3C.csv"
    When I export the preparation with parameters :
      | exportType           | CSV              |
      | preparationName      | 8L3C_preparation |
      | dataSetName          | 8L3C_dataset     |
      | fileName             | 8L3C_result.csv  |
      | csv_escape_character | "                |
      | csv_enclosure_char   | "                |
    Then I check that "8L3C_result.csv" temporary file equals "/data/10L3C_firstname_uppercase.csv" file
