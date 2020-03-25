package stu.napls.clouderweb.service;

import stu.napls.clouderweb.model.Folder;

import java.util.List;

public interface FolderService {

    Folder findById(long id);

    Folder update(Folder folder);

    List<Folder> saveAll(List<Folder> folders);

    void delete(Folder folder);

    void deleteByIds(List<Long> ids);

    Folder findByParentFolderIdAndName(long parentFolderId, String name);

    Folder findByPathAndDepositoryId(String path, long depositoryId);

    List<Folder> findByParentFolderId(long parentFolderId);

}
