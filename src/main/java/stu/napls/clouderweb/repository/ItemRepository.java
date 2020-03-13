package stu.napls.clouderweb.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import stu.napls.clouderweb.model.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Item findByPathAndName(String path, String name);

    Page<Item> findByDepositoryIdAndStatus(Long depositoryId, Integer status, Pageable pageable);

}
