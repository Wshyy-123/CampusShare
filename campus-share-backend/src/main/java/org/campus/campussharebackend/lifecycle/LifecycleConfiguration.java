package org.campus.campussharebackend.lifecycle;

import java.time.Clock;
import java.time.ZoneId;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Import this configuration from the shared backend application. */
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = LifecycleConfiguration.class)
@MapperScan(basePackageClasses = LifecycleMapper.class)
@EnableScheduling
public class LifecycleConfiguration {
    @Bean
    @Qualifier("lifecycleClock")
    public Clock lifecycleClock() {
        return Clock.system(ZoneId.of("Asia/Shanghai"));
    }
}
