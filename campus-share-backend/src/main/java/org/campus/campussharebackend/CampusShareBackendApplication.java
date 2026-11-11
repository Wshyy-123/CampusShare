package org.campus.campussharebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan({
        "org.campus.campussharebackend.mapper",
        "org.campus.campussharebackend.auth.mapper",
        "org.campus.campussharebackend.resource.mapper",
        "org.campus.campussharebackend.reservation.mapper",
        "org.campus.campussharebackend.schedule.mapper"
})
public class CampusShareBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusShareBackendApplication.class, args);
    }

}
