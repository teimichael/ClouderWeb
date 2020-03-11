package stu.napls.clouderweb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stu.napls.clouderweb.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUuid(String uuid);
}
