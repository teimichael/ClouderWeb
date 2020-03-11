package stu.napls.clouderweb.config.property;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Author Tei Michael
 * @Date 1/13/2020
 */
@Component
@Getter
public class StaticServer {
    @Value("${staticserver.url}")
    private String url;

    @Value("${staticserver.root-path}")
    private String rootPath;
}
