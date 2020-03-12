package stu.napls.clouderweb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stu.napls.clouderweb.model.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
