package stu.napls.clouderweb.core.dictionary;

import org.springframework.http.HttpStatus;

public interface ResponseCode {
    int SUCCESS = HttpStatus.OK.value();
    int FAILURE = -1;
}
