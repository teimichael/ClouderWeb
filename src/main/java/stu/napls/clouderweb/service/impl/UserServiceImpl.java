package stu.napls.clouderweb.service.impl;

import org.springframework.stereotype.Service;
import stu.napls.clouderweb.model.User;
import stu.napls.clouderweb.repository.UserRepository;
import stu.napls.clouderweb.service.UserService;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author Tei Michael
 * @Date 12/29/2019
 */
@Service("userService")
public class UserServiceImpl implements UserService {

    @Resource
    private UserRepository userRepository;

    @Override
    public User update(User user) {
        return userRepository.save(user);
    }

    @Override
    public User findUserByUuid(String uuid) {
        return userRepository.findByUuid(uuid);
    }

    @Override
    public List<User> findAllUser() {
        return userRepository.findAll();
    }
}
