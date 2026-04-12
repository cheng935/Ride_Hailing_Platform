package org.example.Ride_Hailing_Platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "org.example.Ride_Hailing_Platform.repository")
    public class DatabaseConfig {
    // 数据库相关配置
    }