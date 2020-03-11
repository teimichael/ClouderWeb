package stu.napls.clouderweb.core.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import stu.napls.clouderweb.core.response.Response;

/**
 * @Author Tei Michael
 * @Date 12/29/2019
 */
@ControllerAdvice
public class SystemExceptionHandler {
    private final static Logger logger = LoggerFactory.getLogger(SystemExceptionHandler.class);

    @ResponseBody
    @ExceptionHandler(value = SystemException.class)
    public Response systemExceptionHandler(SystemException e) {
        logger.error(e.getMessage());
        return Response.failure(e.getCode(), e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public Response errorHandler(Exception e) {
        logger.error(e.getMessage());
        return Response.failure("Internal Error.");
    }


}
