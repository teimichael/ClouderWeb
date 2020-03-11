package stu.napls.clouderweb.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;
import stu.napls.clouderweb.auth.annotation.Auth;
import stu.napls.clouderweb.auth.model.*;
import stu.napls.clouderweb.auth.request.AuthRequest;
import stu.napls.clouderweb.config.GlobalConstant;
import stu.napls.clouderweb.core.dictionary.ResponseCode;
import stu.napls.clouderweb.core.exception.Assert;
import stu.napls.clouderweb.core.response.Response;
import stu.napls.clouderweb.model.User;
import stu.napls.clouderweb.service.UserService;
import stu.napls.clouderweb.util.SessionGetter;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * @Author Tei Michael
 * @Date 3/11/2020
 */
@RestController
@RequestMapping("/access")
public class AccessController {

    @Resource
    private AuthRequest authRequest;

    @Resource
    private UserService userService;

    @Resource
    private SessionGetter sessionGetter;

    @PostMapping("/login")
    public Response login(@RequestBody AuthLogin authLogin) {
        authLogin.setSource(GlobalConstant.SERVICE_ID);
        AuthResponse authResponse = authRequest.login(authLogin);

        Assert.notNull(authResponse, "Authentication failed.");
        Assert.isTrue(authResponse.getCode() == ResponseCode.SUCCESS, authResponse.getMessage());

        return Response.success("Login successfully.", authResponse.getData());
    }

    @PostMapping("/register")
    public Response register(@RequestParam String username, @RequestParam String password, @RequestBody User user) {
        AuthPreregister authPreregister = new AuthPreregister();
        authPreregister.setUsername(username);
        authPreregister.setPassword(password);
        authPreregister.setSource(GlobalConstant.SERVICE_ID);
        AuthResponse authResponse = authRequest.preregister(authPreregister);
        Assert.notNull(authResponse, "Preregistering auth server failed.");
        Assert.isTrue(authResponse.getCode() == ResponseCode.SUCCESS, authResponse.getMessage());
        String uuid = authResponse.getData().toString();

        user.setUuid(uuid);
        userService.update(user);

        AuthRegister authRegister = new AuthRegister();
        authRegister.setUuid(uuid);
        authResponse = authRequest.register(authRegister);
        Assert.isTrue(authResponse != null && authResponse.getCode() == ResponseCode.SUCCESS, "Register failed. Please contact the administrator.");

        return Response.success("Register successfully");
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
                    required = true, dataType = "string", paramType = "header")})
    @Auth
    @PostMapping("/logout")
    public Response logout(@ApiIgnore HttpSession httpSession) {
        AuthLogout authLogout = new AuthLogout();
        authLogout.setUuid(sessionGetter.getUserUUID(httpSession));
        AuthResponse authResponse = authRequest.logout(authLogout);
        Assert.notNull(authResponse, "Authentication failed.");
        Assert.isTrue(authResponse.getCode() == ResponseCode.SUCCESS, "Logout failed.");
        return Response.success("Logout successfully.");
    }

}
