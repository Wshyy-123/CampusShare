package org.campus.campussharebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CampusShareBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusShareBackendApplication.class, args);
    }

}
