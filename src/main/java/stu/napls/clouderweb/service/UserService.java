package stu.napls.clouderweb.service;

import stu.napls.clouderweb.model.User;

import java.util.List;

public interface UserService {
    User update(User user);

    User findUserByUuid(String uuid);

    List<User> findAllUser();

}
