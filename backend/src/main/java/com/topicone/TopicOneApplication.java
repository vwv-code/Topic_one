package com.topicone;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * TopicOne 后端启动类
 */
@SpringBootApplication
@MapperScan("com.topicone.mapper")
@EnableScheduling
public class TopicOneApplication {

    public static void main(String[] args) {
        SpringApplication.run(TopicOneApplication.class, args);
    }
}
