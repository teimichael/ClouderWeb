package stu.napls.clouderweb.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import stu.napls.clouderweb.model.Item;

import java.util.List;

public interface ItemService {

    Item findById(long id);

    Item update(Item item);

    List<Item> saveAll(List<Item> item);

    void delete(Item item);

    void deleteByIds(List<Long> ids);

    Item findByFolderIdAndName(long folderId, String name);

    List<Item> findAllByFolderId(long folderId);

    Page<Item> findAllByFolderId(long folderId, Pageable pageable);

    Page<Item> findAllByDepositoryIdAndType(long depositoryId, int type, Pageable pageable);

}
