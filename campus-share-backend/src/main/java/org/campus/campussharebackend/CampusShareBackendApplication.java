package org.campus.campussharebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"org.campus.campussharebackend", "com.campusshare"})
@MapperScan({
        "org.campus.campussharebackend.mapper",
        "com.campusshare.auth.mapper",
        "com.campusshare.resource.mapper",
        "com.campusshare.reservation.mapper"
})
public class CampusShareBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusShareBackendApplication.class, args);
    }

}
