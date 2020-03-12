package stu.napls.clouderweb.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;
import stu.napls.clouderweb.auth.annotation.Auth;
import stu.napls.clouderweb.core.response.Response;
import stu.napls.clouderweb.service.UserService;
import stu.napls.clouderweb.util.SessionGetter;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * @Author Tei Michael
 * @Date 3/11/2020
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private SessionGetter sessionGetter;

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @GetMapping("/get/info")
    public Response getInfo(@ApiIgnore HttpSession httpSession) {
        return Response.success(sessionGetter.getUser(httpSession));
    }

}
