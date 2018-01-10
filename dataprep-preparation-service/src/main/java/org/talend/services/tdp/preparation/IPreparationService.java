package org.talend.services.tdp.preparation;

import java.util.List;
import java.util.stream.Stream;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.talend.daikon.annotation.Service;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.folder.Folder;
import org.talend.dataprep.api.preparation.*;
import org.talend.dataprep.exception.json.JsonErrorCodeDescription;
import org.talend.dataprep.metrics.Timed;
import org.talend.dataprep.preparation.service.UserPreparation;
import org.talend.dataprep.util.SortAndOrderHelper.Order;
import org.talend.dataprep.util.SortAndOrderHelper.Sort;

/**
 * Preparation service.
 */
@Service(name = "dataprep.preparation")
public interface IPreparationService {

    /**
     * Create a preparation from the http request body.
     *
     * @param preparation the preparation to create.
     * @param folderId where to store the preparation.
     * @return the created preparation id.
     */
    @RequestMapping(value = "/preparations", method = RequestMethod.POST)
    @Timed
    String create(@RequestBody @Valid Preparation preparation, @RequestParam(name = "folderId") String folderId);

    /**
     * List all preparation details.
     *
     * @param name of the preparation.
     * @param folderPath filter on the preparation path.
     * @param path preparation full path in the form folder_path/preparation_name. Overrides folderPath and name if
     * present.
     * @param sort how to sort the preparations.
     * @param order how to order the sort.
     * @return the preparation details.
     */
    @RequestMapping(value = "/preparations/details", method = RequestMethod.GET)
    @Timed
    Stream<UserPreparation> listAll(@RequestParam(name = "name", required = false) String name, //
            @RequestParam(required = false, name = "folder_path") String folderPath, //
            @RequestParam(required = false, name = "path") String path, //
            @RequestParam(name = "sort", defaultValue = "lastModificationDate") Sort sort, //
            @RequestParam(name = "order", defaultValue = "desc") Order order);

    /**
     * List all preparation summaries.
     *
     * @return the preparation summaries, sorted by descending last modification date.
     */
    @RequestMapping(value = "/preparations/summaries", method = RequestMethod.GET)
    @Timed
    Stream<PreparationSummary> listSummary(@RequestParam(required = false, name = "name") String name, //
            @RequestParam(required = false, name = "folder_path") String folderPath, //
            @RequestParam(required = false, name = "path") String path, //
            @RequestParam(name = "sort", defaultValue = "lastModificationDate") Sort sort, //
            @RequestParam(name = "order", defaultValue = "desc") Order order);

    /**
     * <p>
     * Search preparation entry point.
     * </p>
     * <p>
     * <p>
     * So far at least one search criteria can be processed at a time among the following ones :
     * <ul>
     * <li>dataset id</li>
     * <li>preparation name & exact match</li>
     * <li>folderId path</li>
     * </ul>
     * </p>
     *
     * @param dataSetId to search all preparations based on this dataset id.
     * @param folderId to search all preparations located in this folderId.
     * @param name to search all preparations that match this name.
     * @param exactMatch if true, the name matching must be exact.
     * @param path
     * @param sort Sort key (by name, creation date or modification date).
     * @param order Order for sort key (desc or asc).
     */
    @RequestMapping(value = "/preparations/search", method = RequestMethod.GET)
    @Timed
    Stream<UserPreparation> searchPreparations(@RequestParam(name = "dataSetId", required = false) String dataSetId, //
            @RequestParam(name = "folderId", required = false) String folderId, //
            @RequestParam(name = "name", required = false) String name, //
            @RequestParam(name = "exactMatch", defaultValue = "true") boolean exactMatch, //
            @RequestParam(name = "path", required = false) String path, //
            @RequestParam(name = "sort", defaultValue = "lastModificationDate") Sort sort, //
            @RequestParam(name = "order", defaultValue = "desc") Order order);

    /**
     * Copy the given preparation to the given name / folder ans returns the new if in the response.
     *
     * @param name the name of the copied preparation, if empty, the name is "orginal-preparation-name Copy"
     * @param destination the folder path where to copy the preparation, if empty, the copy is in the same folder.
     * @return The new preparation id.
     */
    @RequestMapping(value = "/preparations/{id}/copy", method = RequestMethod.POST,
            produces = MediaType.TEXT_PLAIN_VALUE)
    @Timed
    String copy(@PathVariable(name = "id") String preparationId, //
            @RequestParam(name = "name", required = false) String name, //
            @RequestParam(name = "destination") String destination);

    /**
     * Move a preparation to an other folder.
     *
     * @param folder The original folder of the preparation.
     * @param destination The new folder of the preparation.
     * @param newName The new preparation name.
     */
    @RequestMapping(value = "/preparations/{id}/move", method = RequestMethod.PUT)
    @Timed
    void move(@PathVariable(name = "id") String preparationId, //
            @RequestParam(name = "folder") String folder, //
            @RequestParam(name = "destination") String destination, //
            @RequestParam(name = "newName", defaultValue = "") String newName);

    /**
     * Delete the preparation that match the given id.
     *
     * @param preparationId the preparation id to delete.
     */
    @RequestMapping(value = "/preparations/{id}", method = RequestMethod.DELETE)
    @Timed
    void delete(@PathVariable(name = "id") String preparationId);

    /**
     * Update a preparation.
     *
     * @param preparationId the preparation id to update.
     * @param preparation the updated preparation.
     * @return the updated preparation id.
     */
    @RequestMapping(value = "/preparations/{id}", method = RequestMethod.PUT)
    @Timed
    String update(@PathVariable(name = "id") String preparationId, @RequestBody Preparation preparation);

    /**
     * Copy the steps from the another preparation to this one.
     * <p>
     * This is only allowed if this preparation has no steps.
     *
     * @param id the preparation id to update.
     * @param from the preparation id to copy the steps from.
     */
    @RequestMapping(value = "/preparations/{id}/steps/copy", method = RequestMethod.PUT)
    @Timed
    void copyStepsFrom(@PathVariable(name = "id") String id, @RequestParam(name = "from") String from);

    /**
     * Return a preparation details.
     *
     * @param id the wanted preparation id.
     * @param stepId the optional step id.
     * @return the preparation details.
     */
    @RequestMapping(value = "/preparations/{id}/details", method = RequestMethod.GET)
    @Timed
    PreparationMessage getPreparationDetails(@PathVariable(name = "id") String id,
            @RequestParam(name = "stepId", defaultValue = "head") String stepId);

    /**
     * Return the folder that holds this preparation.
     *
     * @param id the wanted preparation id.
     * @return the folder that holds this preparation.
     */
    @RequestMapping(value = "/preparations/{id}/folder", method = RequestMethod.GET)
    @Timed
    Folder searchLocation(@PathVariable(name = "id") String id);

    @RequestMapping(value = "/preparations/{id}/steps", method = RequestMethod.GET)
    @Timed
    List<String> getSteps(@PathVariable(name = "id") String id);

    /**
     * Adds an action at the end of preparation.
     * Does not return any value, client may expect successful operation based on HTTP status code.
     * 
     * @param preparationId
     * @param step
     */
    @RequestMapping(value = "/preparations/{id}/actions", method = RequestMethod.POST)
    @Timed
    void addPreparationAction(@PathVariable(name = "id") String preparationId, @RequestBody List<AppendStep> step);

    /**
     * Update a step in a preparation <b>Strategy</b><br/>
     * The goal here is to rewrite the preparation history from 'the step to modify' (STM) to the head, with STM
     * containing the new action.<br/>
     * <ul>
     * <li>1. Extract the actions from STM (excluded) to the head</li>
     * <li>2. Insert the new actions before the other extracted actions. The actions list contains all the actions from
     * the <b>NEW</b> STM to the head</li>
     * <li>3. Set preparation head to STM's parent, so STM will be excluded</li>
     * <li>4. Append each action (one step is created by action) after the new preparation head</li>
     * </ul>
     */
    @RequestMapping(value = "/preparations/{id}/actions/{stepId}", method = RequestMethod.PUT)
    @Timed
    void updateAction(@PathVariable(name = "id") String preparationId,
            @PathVariable(name = "stepId") String stepToModifyId, @RequestBody AppendStep newStep);

    /**
     * Delete a step in a preparation.<br/>
     * STD : Step To Delete <br/>
     * <br/>
     * <ul>
     * <li>1. Extract the actions from STD (excluded) to the head. The actions list contains all the actions from the
     * STD's child to the head.</li>
     * <li>2. Filter the preparations that apply on a column created by the step to delete. Those steps will be removed
     * too.</li>
     * <li>2bis. Change the actions that apply on columns > STD last created column id. The created columns ids after
     * the STD are shifted.</li>
     * <li>3. Set preparation head to STD's parent, so STD will be excluded</li>
     * <li>4. Append each action after the new preparation head</li>
     * </ul>
     *
     * @param id the preparation id.
     * @param stepToDeleteId the step id to delete.
     */
    @RequestMapping(value = "/preparations/{id}/actions/{stepId}", method = RequestMethod.DELETE)
    void deleteAction(@PathVariable(name = "id") String id, @PathVariable(name = "stepId") String stepToDeleteId);

    @RequestMapping(value = "/preparations/{id}/head/{headId}", method = RequestMethod.PUT)
    @Timed
    void setPreparationHead(@PathVariable(name = "id") String preparationId,
            @PathVariable(name = "headId") String headId);

    /**
     * Get all the actions of a preparation at given version.
     *
     * @param id the wanted preparation id.
     * @param version the wanted preparation version.
     * @return the list of actions.
     */
    @RequestMapping(value = "/preparations/{id}/actions/{version}", method = RequestMethod.GET)
    @Timed
    List<Action> getVersionedAction(@PathVariable(name = "id") String id,
            @PathVariable(name = "version") String version);

    /**
     * List all preparation related error codes.
     */
    @RequestMapping(value = "/preparations/errors", method = RequestMethod.GET)
    @Timed
    Iterable<JsonErrorCodeDescription> listErrors();

    @RequestMapping(value = "/preparations/use/dataset/{datasetId}", method = RequestMethod.HEAD)
    @Timed
    boolean isDatasetUsedInPreparation(@PathVariable(name = "datasetId") String datasetId);

    /**
     * Moves the step with specified <i>stepId</i> just after the step with <i>parentStepId</i> as identifier within the
     * specified
     * preparation.
     *
     * @param preparationId the id of the preparation containing the step to move
     * @param stepId the id of the step to move
     * @param parentStepId the id of the step which wanted as the parent of the step to move
     */
    @RequestMapping(value = "/preparations/{id}/steps/{stepId}/order", method = RequestMethod.POST)
    @Timed
    void moveStep(@PathVariable(name = "id") String preparationId, //
            @PathVariable(name = "stepId") String stepId, //
            @RequestParam(name = "parentStepId") String parentStepId);

    /**
     * Get the step from id
     *
     * @param stepId The step id
     * @return The step with the provided id, might return <code>null</code> is step does not exist.
     * @see PreparationRepository#get(String, Class)
     */
    @RequestMapping(value = "/steps/{id}", method = RequestMethod.GET)
    @Timed
    Step getStep(@PathVariable(name = "id") String stepId);

    /**
     * Get preparation from id with null result check.
     *
     * @param preparationId The preparation id.
     * @return The preparation with the provided id
     * @throws TDPException when no preparation has the provided id
     */
    @RequestMapping(value = "/preparations/{id}", method = RequestMethod.GET)
    @Timed
    Preparation getPreparation(@PathVariable(name = "id") String preparationId);

    /**
     * Marks the specified preparation (identified by <i>preparationId</i>) as locked by the user identified by the
     * specified user (identified by <i>userId</i>).
     *
     * @param preparationId the specified preparation identifier
     * @throws TDPException if the lock is hold by another user
     */
    @RequestMapping(value = "/preparations/{preparationId}/lock", method = RequestMethod.PUT)
    @Timed
    Preparation lockPreparation(@PathVariable(name = "preparationId") String preparationId);

    /**
     * Marks the specified preparation (identified by <i>preparationId</i>) as unlocked by the user identified by the
     * specified user (identified by <i>userId</i>).
     *
     * @param preparationId the specified preparation identifier
     * @throws TDPException if the lock is hold by another user
     */
    @RequestMapping(value = "/preparations/{preparationId}/unlock", method = RequestMethod.PUT)
    @Timed
    void unlockPreparation(@PathVariable(name = "preparationId") String preparationId);

    /**
     *
     * @param stepId
     * @param rowMetadata
     */
    @RequestMapping(value = "/preparations/steps/{stepId}", method = RequestMethod.PUT)
    void updatePreparationStep(@PathVariable(name = "stepId") String stepId, @RequestBody RowMetadata rowMetadata);

    /**
     *
     * @param stepId
     * @return
     */
    @RequestMapping(value = "/preparations/steps/{stepId}/metadata", method = RequestMethod.GET)
    @Timed
    RowMetadata getPreparationStep(@PathVariable(name = "stepId") String stepId);

}
