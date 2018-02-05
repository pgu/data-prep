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

package org.talend.dataprep.api.service;

import static com.jayway.restassured.RestAssured.given;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.talend.ServiceBaseTest;
import org.talend.dataprep.api.folder.Folder;
import org.talend.dataprep.api.service.test.APIClientTest;
import org.talend.dataprep.async.AsyncExecution;
import org.talend.dataprep.async.AsyncExecutionMessage;
import org.talend.dataprep.cache.ContentCache;
import org.talend.dataprep.dataset.store.content.DataSetContentStore;
import org.talend.dataprep.dataset.store.metadata.DataSetMetadataRepository;
import org.talend.dataprep.folder.store.FolderRepository;
import org.talend.dataprep.format.export.ExportFormat;
import org.talend.dataprep.preparation.store.PreparationRepository;
import org.talend.dataprep.transformation.aggregation.api.AggregationParameters;
import org.talend.dataprep.transformation.format.CSVFormat;
import org.talend.dataprep.url.UrlRuntimeUpdater;

import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

/**
 * Base test for all API service unit.
 */
public abstract class ApiServiceTestBase extends ServiceBaseTest {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ApiServiceTestBase.class);

    @Autowired
    protected DataSetMetadataRepository dataSetMetadataRepository;

    @Autowired
    @Qualifier("ContentStore#local")
    protected DataSetContentStore contentStore;

    @Autowired
    protected PreparationRepository preparationRepository;

    @Autowired
    protected ContentCache cache;

    @Autowired
    protected FolderRepository folderRepository;

    @Autowired
    protected APIClientTest testClient;

    protected Folder home;

    @Autowired
    private UrlRuntimeUpdater[] urlUpdaters;

    @Before
    public void setUp() {
        super.setUp();
        for (UrlRuntimeUpdater urlUpdater : urlUpdaters) {
            urlUpdater.setUp();
        }
        home = folderRepository.getHome();
    }

    @After
    public void tearDown() {
        dataSetMetadataRepository.clear();
        contentStore.clear();
        preparationRepository.clear();
        cache.clear();
        folderRepository.clear();
    }

    protected AggregationParameters getAggregationParameters(String input) throws IOException {
        InputStream parametersInput = this.getClass().getResourceAsStream(input);
        return mapper.readValue(parametersInput, AggregationParameters.class);
    }

    protected Response getPreparation(String preparationId) throws IOException, InterruptedException {
        return getPreparation(preparationId, "head", "HEAD");
    }

    protected Response getPreparation(String preparationId, String versionId) throws IOException, InterruptedException {
        return getPreparation(preparationId, versionId, "HEAD");
    }

    /**
     * Method handling 202/200 status to get the transformation content
     *
     * @param preparationId prepartionId
     * @return the content of a preparation
     * @throws IOException
     * @throws InterruptedException
     */
    protected Response getPreparation(String preparationId, String version, String stepId)
            throws IOException, InterruptedException {
        // when
        Response transformedResponse = given()
                .when() //
                .get("/api/preparations/{prepId}/content?version={version}&from={stepId}", preparationId, version, stepId);

        if (HttpStatus.ACCEPTED.value() == transformedResponse.getStatusCode()) {
            // first time we have a 202 with a Location to see asynchronous method status
            final String asyncMethodStatusUrl = transformedResponse.getHeader("Location");

            waitForAsynchronousMethodTofinish(asyncMethodStatusUrl);

            transformedResponse = given()
                    .when() //
                    .expect()
                    .statusCode(200)
                    .log()
                    .ifError() //
                    .get("/api/preparations/{id}/content?version=head", preparationId);
        }

        return transformedResponse;
    }

    /**
     * Ping (100 times max) async method status url in order to wait the end of the execution
     *
     * @param asyncMethodStatusUrl
     * @throws IOException
     * @throws InterruptedException
     */
    private void waitForAsynchronousMethodTofinish(String asyncMethodStatusUrl) throws IOException, InterruptedException {
        boolean isAsyncMethodRunning = true;
        int nbLoop = 0;

        while (isAsyncMethodRunning && nbLoop < 100) {

            String statusAsyncMethod = given()
                    .when() //
                    .expect()
                    .statusCode(200)
                    .log()
                    .ifError() //
                    .get(asyncMethodStatusUrl)
                    .asString();

            AsyncExecutionMessage asyncExecutionMessage =
                    mapper.readerFor(AsyncExecutionMessage.class).readValue(statusAsyncMethod);

            isAsyncMethodRunning = asyncExecutionMessage.getStatus().equals(AsyncExecution.Status.RUNNING);

            Thread.sleep(50);

            nbLoop++;
        }
    }

    protected Response exportPreparation(String preparationId, String stepId, String csvDelimiter, String fileName)
            throws IOException, InterruptedException {
        return export(preparationId, "", stepId, csvDelimiter, fileName);
    }

    protected Response exportPreparation(String preparationId, String stepId, String csvDelimiter)
            throws IOException, InterruptedException {
        return export(preparationId, "", stepId, csvDelimiter, null);
    }

    protected Response exportPreparation(String preparationId, String stepId) throws IOException, InterruptedException {
        return export(preparationId, "", stepId, null, null);
    }

    protected Response exportDataset(String datasetId, String stepId) throws IOException, InterruptedException {
        return export("", datasetId, stepId, null, null);
    }

    protected Response export(String preparationId, String datasetId, String stepId, String csvDelimiter, String fileName)
            throws IOException, InterruptedException {
        // when
        RequestSpecification exportRequest = given() //
                .formParam("exportType", "CSV") //
                .formParam(ExportFormat.PREFIX + CSVFormat.ParametersCSV.ENCLOSURE_MODE,
                        CSVFormat.ParametersCSV.ENCLOSURE_ALL_FIELDS) //
                .formParam("preparationId", preparationId) //
                .formParam("stepId", stepId) //
                .formParam("datasetId", datasetId); //

        if (StringUtils.isNotEmpty(csvDelimiter)) {
            exportRequest.formParam(ExportFormat.PREFIX + CSVFormat.ParametersCSV.FIELDS_DELIMITER, csvDelimiter);
        }

        if (StringUtils.isNotEmpty(fileName)) {
            exportRequest.formParam(ExportFormat.PREFIX + "fileName", fileName);
        }

        Response export = exportRequest
                .when() //
                .get("/api/export");

        if (HttpStatus.ACCEPTED.value() == export.getStatusCode()) {
            // first time we have a 202 with a Location to see asynchronous method status
            final String asyncMethodStatusUrl = export.getHeader("Location");

            waitForAsynchronousMethodTofinish(asyncMethodStatusUrl);

            export = given() //
                    .formParam("exportType", "CSV") //
                    .formParam(ExportFormat.PREFIX + CSVFormat.ParametersCSV.ENCLOSURE_MODE,
                            CSVFormat.ParametersCSV.ENCLOSURE_ALL_FIELDS) //
                    .formParam(ExportFormat.PREFIX + CSVFormat.ParametersCSV.FIELDS_DELIMITER, csvDelimiter) //
                    .formParam(ExportFormat.PREFIX + "fileName", fileName) //
                    .formParam("preparationId", preparationId) //
                    .formParam("stepId", stepId) //
                    .formParam("datasetId", datasetId) //
                    .when() //
                    .expect()
                    .statusCode(200)
                    .log()
                    .ifError() //
                    .get("/api/export");
        }

        return export;
    }
}
