package com.campusshare.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({
        "com.campusshare.auth.mapper",
        "com.campusshare.resource.mapper",
        "com.campusshare.reservation.mapper"
})
public class MyBatisConfig {
}
