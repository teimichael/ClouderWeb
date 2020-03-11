package stu.napls.clouderweb.config.property;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Author Tei Michael
 * @Date 12/31/2019
 */
@Component
@Getter
public class SocketServer {

    @Value("${socketserver.url}")
    private String url;
}
