package org.example.ridehailing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "org.example.ridehailing.repository")
    public class DatabaseConfig {
    // 数据库相关配置
    }