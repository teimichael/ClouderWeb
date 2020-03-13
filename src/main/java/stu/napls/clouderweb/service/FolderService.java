package stu.napls.clouderweb.service;

import stu.napls.clouderweb.model.Folder;

import java.util.List;

public interface FolderService {

    Folder create(Folder folder);

    Folder findByUserIdAndPath(long userId, String path);

    List<Folder> findByUserIdAndParentFolderId(long userId, long parentFolderId);
}
