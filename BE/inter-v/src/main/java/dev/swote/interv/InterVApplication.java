package dev.swote.interv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class InterVApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterVApplication.class, args);
    }

}
