package stu.napls.clouderweb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stu.napls.clouderweb.model.Folder;

import java.util.List;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    Folder findByParentFolderIdAndNameAndStatus(long parentFolderId, String name, int status);

    Folder findByDepositoryIdAndPathAndStatus(long depositoryId, String path, int status);

    List<Folder> findByDepositoryIdAndParentFolderIdAndStatus(long depositoryId, long parentFolderId, int status);

    List<Folder> findByParentFolderIdAndStatus(long parentFolderId, int status);

    Folder findByPathAndDepositoryIdAndStatus(String path, long depositoryId, int status);

    void deleteByIdIn(List<Long> ids);
}
