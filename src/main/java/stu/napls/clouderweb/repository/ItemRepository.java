package stu.napls.clouderweb.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import stu.napls.clouderweb.model.Item;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Item findByFolderIdAndNameAndStatus(Long folderId, String name, Integer status);

    void deleteByIdIn(List<Long> ids);

    List<Item> findByFolderIdAndStatus(Long folderId, Integer status);

    Page<Item> findByFolderIdAndStatus(Long folderId, Integer status, Pageable pageable);

    Page<Item> findByDepositoryIdAndTypeAndStatus(Long depositoryId, Integer type, Integer status, Pageable pageable);

}
