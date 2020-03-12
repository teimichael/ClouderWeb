package stu.napls.clouderweb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stu.napls.clouderweb.model.Depository;

public interface DepositoryRepository extends JpaRepository<Depository, Long> {
}
