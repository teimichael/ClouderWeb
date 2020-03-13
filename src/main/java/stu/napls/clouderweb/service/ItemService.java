package stu.napls.clouderweb.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import stu.napls.clouderweb.model.Item;

public interface ItemService {

    Item update(Item item);

    void delete(Item item);

    Item findByPathAndName(String path, String name);

    Page<Item> findAllByDepositoryId(long depositoryId, Pageable pageable);

}
