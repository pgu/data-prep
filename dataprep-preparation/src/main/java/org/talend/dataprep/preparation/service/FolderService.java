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

package org.talend.dataprep.preparation.service;

import static java.util.stream.Collectors.toList;
import static org.talend.daikon.exception.ExceptionContext.build;
import static org.talend.dataprep.api.folder.FolderContentType.PREPARATION;
import static org.talend.dataprep.exception.error.FolderErrorCodes.FOLDER_NOT_FOUND;
import static org.talend.dataprep.util.SortAndOrderHelper.getFolderComparator;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.talend.daikon.annotation.ServiceImplementation;
import org.talend.daikon.exception.ExceptionContext;
import org.talend.dataprep.api.folder.Folder;
import org.talend.dataprep.api.folder.FolderInfo;
import org.talend.dataprep.api.folder.FolderTreeNode;
import org.talend.dataprep.api.folder.UserFolder;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.folder.store.FolderRepository;
import org.talend.dataprep.security.Security;
import org.talend.dataprep.util.SortAndOrderHelper.Order;
import org.talend.dataprep.util.SortAndOrderHelper.Sort;
import org.talend.services.tdp.folder.IFolderService;

@ServiceImplementation
public class FolderService implements IFolderService {

    /** This class' logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FolderService.class);

    /** Where the folders are stored. */
    @Autowired
    private FolderRepository folderRepository;

    /** DataPrep abstraction to the underlying security (whether it's enabled or not). */
    @Autowired
    private Security security;

    @Override
    public Stream<UserFolder> list(String parentId, Sort sort, Order order) {
        Stream<UserFolder> children;
        if (parentId != null) {
            if (!folderRepository.exists(parentId)) {
                throw new TDPException(FOLDER_NOT_FOUND, build().put("id", parentId));
            }
            children = folderRepository.children(parentId);
        } else {
            // This will list all folders
            children = folderRepository.searchFolders("", false);
        }

        final AtomicInteger folderCount = new AtomicInteger();

        // update the number of preparations in each children
        children = children.peek(f -> {
            final long count = folderRepository.count(f.getId(), PREPARATION);
            f.setNbPreparations(count);
            folderCount.addAndGet(1);
        });

        LOGGER.info("Found {} children for parentId: {}", folderCount.get(), parentId);

        // sort the folders
        return children.sorted(getFolderComparator(sort, order));
    }

    @Override
    public FolderInfo getFolderAndHierarchyById(final String id) {
        final Folder folder = folderRepository.getFolderById(id);
        if (folder == null) {
            throw new TDPException(FOLDER_NOT_FOUND, ExceptionContext.build().put("path", id));
        }
        final List<Folder> hierarchy = folderRepository.getHierarchy(folder);

        return new FolderInfo(folder, hierarchy);
    }

    @Override
    public Stream<UserFolder> search(final String name, final Boolean strict, final String path) {
        Stream<UserFolder> folders;
        if (path == null) {
            folders = folderRepository.searchFolders(name, strict);
        } else {
            folders = folderRepository.searchFolders(name, strict).filter(f -> f.getPath().equals(path));
        }

        AtomicInteger foldersFound = new AtomicInteger(0);
        folders = folders.peek(folder -> {
            folder.setNbPreparations(folderRepository.count(folder.getId(), PREPARATION));
            foldersFound.incrementAndGet();
        });

        LOGGER.info("Found {} folder(s) searching for {}", foldersFound, name);

        return folders;
    }

    @Override
    public Folder addFolder(String parentId, String path) {
        if (parentId == null) {
            parentId = folderRepository.getHome().getId();
        }
        return folderRepository.addFolder(parentId, path);
    }

    @Override
    public void removeFolder(String id) {
        folderRepository.removeFolder(id);
    }

    @Override
    public void renameFolder(String id, String newName) {
        folderRepository.renameFolder(id, newName);
    }

    @Override
    public FolderTreeNode getTree() {
        final Folder home = folderRepository.getHome();
        return getTree(home);
    }

    private FolderTreeNode getTree(final Folder root) {
        try (final Stream<UserFolder> children = folderRepository.children(root.getId())) {
            final List<FolderTreeNode> childrenSubtrees =
                    StreamSupport.stream(children.spliterator(), false).map(this::getTree).collect(toList());
            return new FolderTreeNode(root, childrenSubtrees);
        }
    }
}
