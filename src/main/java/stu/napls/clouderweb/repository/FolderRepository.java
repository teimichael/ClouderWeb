package stu.napls.clouderweb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stu.napls.clouderweb.model.Folder;

import java.util.List;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    Folder findByUserIdAndPath(long userId, String path);

    List<Folder> findByUserIdAndParentFolderId(long userId, long parentFolderId);
}
