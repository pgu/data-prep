package org.talend.services.tdp.transformation;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.talend.daikon.annotation.Service;
import org.talend.dataprep.api.action.ActionForm;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.api.dataset.statistics.SemanticDomain;
import org.talend.dataprep.api.preparation.StepDiff;
import org.talend.dataprep.exception.json.JsonErrorCodeDescription;
import org.talend.dataprep.metrics.Timed;
import org.talend.dataprep.metrics.VolumeMetered;
import org.talend.dataprep.security.PublicAPI;
import org.talend.dataprep.services.api.AggregationParameters;
import org.talend.dataprep.services.transformation.AggregationResult;
import org.talend.dataprep.services.transformation.ExportFormatMessage;
import org.talend.dataprep.services.transformation.ExportParameters;
import org.talend.dataprep.services.transformation.PreviewParameters;
import org.talend.dataprep.transformation.api.action.dynamic.GenericParameter;

/**
 *
 */
@Service(name = "dataprep.transformation")
public interface ITransformationService {

    /**
     * Run the transformation given the provided export parameters.
     *
     * @param parameters Preparation id to apply.
     * @return A transformed DataSet
     */
    @RequestMapping(value = "/apply", method = RequestMethod.POST)
    @VolumeMetered
    StreamingResponseBody execute(@RequestBody @Valid ExportParameters parameters);

    /**
     * This operation transforms the dataset or preparation using parameters in export parameters.
     *
     * @param preparationId
     * @param stepId
     * @return
     */
    @RequestMapping(value = "/apply/preparation/{preparationId}/{stepId}/metadata", method = RequestMethod.GET)
    @VolumeMetered
    DataSetMetadata executeMetadata(@PathVariable(name = "preparationId") String preparationId, //
            @PathVariable(name = "stepId") String stepId);

    /**
     * Transform the given preparation to the given format on the given dataset id.
     *
     * @param preparationId Preparation id to apply.
     * @param datasetId DataSet id to transform.
     * @param formatName Output format
     * @param stepId Step id, defaults to "head"
     * @param name Name of the transformation, defaults to "untitled"
     * @param exportParams
     * @return
     */
    @RequestMapping(value = "/apply/preparation/{preparationId}/dataset/{datasetId}/{format}", method = RequestMethod.GET)
    @VolumeMetered
    StreamingResponseBody applyOnDataset(@PathVariable(name = "preparationId") String preparationId, //
            @PathVariable(name = "datasetId") String datasetId, //
            @PathVariable(name = "format") String formatName, //
            @RequestParam(name = "stepId", required = false, defaultValue = "head") String stepId, //
            @RequestParam(name = "name", required = false, defaultValue = "untitled") String name, //
            @RequestParam(name = "exportParams") Map<String, String> exportParams);

    /**
     * Export the given dataset.
     *
     * @param datasetId DataSet id to transform.
     * @param formatName Output format
     * @param name Name of the transformation
     * @param exportParams
     * @return
     */
    @RequestMapping(value = "/export/dataset/{datasetId}/{format}", method = RequestMethod.GET)
    @Timed
    StreamingResponseBody exportDataset(@PathVariable(name = "datasetId") String datasetId, //
            @PathVariable(name = "format") String formatName, //
            @RequestParam(name = "name", required = false, defaultValue = "untitled") String name,
            @RequestParam(name = "exportParams") Map<String, String> exportParams);

    /**
     * Compute the aggregation according to the request body rawParams
     * 
     * @param rawParams The aggregation rawParams in json
     * @return The aggregation result.
     */
    @RequestMapping(value = "/aggregate", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @VolumeMetered
    AggregationResult aggregate(@RequestBody AggregationParameters rawParams);

    /**
     * This operation returns the input data diff between the old and the new transformation actions.
     *  @param previewParameters Preview parameters.
     * */
    @RequestMapping(value = "/transform/preview", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @VolumeMetered
    StreamingResponseBody transformPreview(@RequestBody PreviewParameters previewParameters);

    /**
     * Given a list of requested preview, it applies the diff to each one. A diff is between 2 sets of actions and
     * return the info like created columns ids.
     * 
     * @param previewParameters Preview parameters list in json.
     * @return
     */
    @RequestMapping(value = "/transform/diff/metadata", method = RequestMethod.POST)
    @VolumeMetered
    Stream<StepDiff> getCreatedColumns(@RequestBody List<PreviewParameters> previewParameters);

    /**
     * This operation remove content entries related to the preparation.
     * 
     * @param preparationId Preparation Id.
     */
    @RequestMapping(value = "/preparation/{preparationId}/cache", method = RequestMethod.DELETE)
    @VolumeMetered
    void evictCache(@PathVariable(name = "preparationId") String preparationId);

    /**
     * Get the transformation dynamic parameters.
     *
     * @param action Action name.
     * @param columnId The column id.
     * @param content Data set content as JSON.
     * @return Return all actions for a column (regardless of column metadata).
     */
    @RequestMapping(value = "/transform/suggest/{action}/params", method = RequestMethod.POST)
    @Timed
    GenericParameter dynamicParams(@PathVariable(name = "action") String action, //
            @RequestParam(name = "columnId") String columnId, //
            InputStream content);

    /**
     * This operation returns an array of actions.
     * 
     * @param column
     * @return
     */
    @RequestMapping(value = "/actions/column", method = RequestMethod.POST)
    @ResponseBody
    Stream<ActionForm> columnActions(@RequestBody(required = false) ColumnMetadata column);

    /**
     * This operation returns an array of suggested actions in decreasing order of importance.
     * 
     * @param column
     * @param limit How many actions should be suggested at most. Defaults to 5.
     * @return
     */
    @RequestMapping(value = "/suggest/column", method = RequestMethod.POST)
    @ResponseBody
    Stream<ActionForm> suggest(@RequestBody(required = false) ColumnMetadata column,
            @RequestParam(name = "limit", defaultValue = "5", required = false) int limit);

    /**
     * This operation returns an array of actions.
     * 
     * @return
     */
    @RequestMapping(value = "/actions/line", method = RequestMethod.GET)
    @ResponseBody
    Stream<ActionForm> lineActions();

    /**
     * Return all actions on the whole dataset.
     * 
     * @return
     */
    @RequestMapping(value = "/actions/dataset", method = RequestMethod.GET)
    @ResponseBody
    Stream<ActionForm> datasetActions();

    /**
     * This operation returns an array of suggested actions in decreasing order of importance.
     *
     * @param dataSetMetadata
     * @return
     */
    @RequestMapping(value = "/suggest/dataset", method = RequestMethod.POST)
    @ResponseBody
    List<ActionForm> suggest(@RequestBody DataSetMetadata dataSetMetadata);

    /**
     * Returns the list of all transformation related error codes.
     * 
     * @return
     */
    @RequestMapping(value = "/transform/errors", method = RequestMethod.GET)
    @Timed
    Iterable<JsonErrorCodeDescription> listErrors();

    /**
     * Get the available format types
     * 
     * @return
     */
    @RequestMapping(value = "/export/formats", method = RequestMethod.GET)
    @Timed
    @PublicAPI
    Stream<ExportFormatMessage> exportTypes();

    /**
     * Get the available format types for the preparation
     * 
     * @param preparationId
     * @return
     */
    @RequestMapping(value = "/export/formats/preparations/{preparationId}", method = RequestMethod.GET)
    @Timed
    Stream<ExportFormatMessage> getPreparationExportTypesForPreparation(@PathVariable(name = "preparationId") String preparationId);

    /**
     * Get the available format types for the preparation
     * 
     * @param dataSetId
     * @return
     */
    @RequestMapping(value = "/export/formats/datasets/{dataSetId}", method = RequestMethod.GET)
    @Timed
    Stream<ExportFormatMessage> getPreparationExportTypesForDataSet(@PathVariable(name = "dataSetId") String dataSetId);

    /**
     * Get current dictionary (as serialized object).
     * 
     * @return
     */
    @RequestMapping(value = "/dictionary", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Timed
    StreamingResponseBody getDictionary();

    /**
     * This list can be used by user to change the column type.
     * 
     * @param preparationId The preparation id
     * @param columnId The column id
     * @param stepId The preparation version, default to "head".
     * @return
     */
    @RequestMapping(value = "/preparations/{preparationId}/columns/{columnId}/types", method = RequestMethod.GET)
    @Timed
    @PublicAPI
    List<SemanticDomain> getPreparationColumnSemanticCategories(@PathVariable(name = "preparationId") String preparationId,
            @PathVariable(name = "columnId") String columnId, @RequestParam(name = "stepId", defaultValue = "head") String stepId);
}
