package com.web.aipictureslib;

import lombok.EqualsAndHashCode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableAspectJAutoProxy(exposeProxy = true)
public class AiPicturesLibApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiPicturesLibApplication.class, args);
    }

}
