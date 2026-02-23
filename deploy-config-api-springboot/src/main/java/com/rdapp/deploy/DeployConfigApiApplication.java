package com.rdapp.deploy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DeployConfigApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeployConfigApiApplication.class, args);
    }
}
