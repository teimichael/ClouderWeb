package stu.napls.clouderweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ClouderWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClouderWebApplication.class, args);
    }

}
