package stu.napls.clouderweb.core.response;

import org.springframework.http.HttpStatus;

public interface ResponseCode {
    int SUCCESS = HttpStatus.OK.value();
    int FAILURE = -1;
    int NONEXISTENCE = -2;
    int ILLEGAL = -3;
    int UNSUPPORTED = -4;
    int UNAUTHORIZED = HttpStatus.UNAUTHORIZED.value();

}
