package org.talend.services.tdp.folder;

import java.util.stream.Stream;

import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.talend.daikon.annotation.Service;
import org.talend.dataprep.api.folder.Folder;
import org.talend.dataprep.api.folder.FolderInfo;
import org.talend.dataprep.api.folder.FolderTreeNode;
import org.talend.dataprep.api.folder.UserFolder;
import org.talend.dataprep.metrics.Timed;
import org.talend.dataprep.util.SortAndOrderHelper.Order;
import org.talend.dataprep.util.SortAndOrderHelper.Sort;

/**
 * Folder service
 */
@Service(name = "dataprep.folders")
public interface IFolderService {

    /**
     * Get folders. If parentId is supplied, it will be used as filter.
     *
     * @param parentId the parent folder id parameter
     * @return direct sub folders for the given id.
     */
    //@formatter:off
@RequestMapping(value = "/folders", method = RequestMethod.GET, produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    @Timed
    Stream<UserFolder> list(@RequestParam(name = "parentId", required = false) String parentId, //
                        @RequestParam(name = "sort", defaultValue = "lastModificationDate") Sort sort, //
                        @RequestParam(name = "order", defaultValue = "desc") Order order);

    /**
     * Get a folder metadata with its hierarchy
     *
     * @param id the folder id.
     * @return the folder metadata with its hierarchy.
     */
    @RequestMapping(value = "/folders/{id}", method = RequestMethod.GET, produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    @Timed
    FolderInfo getFolderAndHierarchyById(@PathVariable(name = "id") String id);

    /**
     * Search for folders.
     *
     * @param name the folder name to search.
     * @param strict strict mode means the name is the full name.
     * @return the folders whose part of their name match the given path.
     */
    @RequestMapping(value = "/folders/search", method = RequestMethod.GET, produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    @Timed
    Stream<UserFolder> search(@RequestParam(name = "name", required = false, defaultValue = "") String name, //
                          @RequestParam(name = "strict", required = false, defaultValue = "false") Boolean strict, //
                          @RequestParam(name = "path", required = false) String path);

    /**
     * Add a folder.
     *
     * @param parentId where to add the folder.
     * @return the created folder.
     */
    @RequestMapping(value = "/folders", method = RequestMethod.PUT, produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    @Timed
    Folder addFolder(@RequestParam(name = "parentId", required = false) String parentId, @RequestParam(name = "path") String path);

    /**
     * Remove the folder. Throws an exception if the folder, or one of its sub folders, contains an entry.
     *
     * @param id the id that points to the folder to remove.
     */
    @RequestMapping(value = "/folders/{id}", method = RequestMethod.DELETE)
    @Timed
    void removeFolder(@PathVariable(name = "id") String id);

    /**
     * Rename the folder to the new id.
     *
     * @param id where to look for the folder.
     * @param newName the new folder id.
     */
    @RequestMapping(value = "/folders/{id}/name", method = RequestMethod.PUT)
    @Timed
    void renameFolder(@PathVariable(name = "id") String id, @RequestBody String newName);

    @RequestMapping(value = "/folders/tree", method = RequestMethod.GET, produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
    @Timed
    FolderTreeNode getTree();
}
