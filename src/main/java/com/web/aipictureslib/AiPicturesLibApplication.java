package com.web.aipictureslib;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.web.aipictureslib.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class AiPicturesLibApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiPicturesLibApplication.class, args);
    }

}
