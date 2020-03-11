package stu.napls.clouderweb.util;

import org.springframework.stereotype.Component;
import stu.napls.clouderweb.model.User;
import stu.napls.clouderweb.service.UserService;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@Component
public class SessionGetter {

    @Resource
    private UserService userService;

    public User getUser(HttpSession httpSession) {
        return userService.findUserByUuid(httpSession.getAttribute("uuid").toString());
    }

    public String getUserUUID(HttpSession httpSession) {
        return httpSession.getAttribute("uuid").toString();
    }
}
