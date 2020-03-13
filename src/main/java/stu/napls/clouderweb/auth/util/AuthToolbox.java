package stu.napls.clouderweb.auth.util;

import org.springframework.stereotype.Component;
import stu.napls.clouderweb.auth.model.AuthResponse;
import stu.napls.clouderweb.auth.model.AuthVerify;
import stu.napls.clouderweb.auth.request.AuthRequest;
import stu.napls.clouderweb.core.exception.Assert;
import stu.napls.clouderweb.core.response.ResponseCode;

import javax.annotation.Resource;

@Component
public class AuthToolbox {

    @Resource
    private AuthRequest authRequest;

    /**
     * Verify token
     *
     * @param token
     * @return User UUID
     */
    public String verifyToken(String token) {
        AuthVerify authVerify = new AuthVerify(token);
        AuthResponse authResponse = authRequest.verify(authVerify);
        Assert.isTrue(authResponse != null, "Authentication failed.");
        Assert.isTrue(authResponse.getCode() == ResponseCode.SUCCESS, authResponse.getMessage());
        return authResponse.getData().toString();
    }
}
