package com.fang123;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Fang123Application {

    public static void main(String[] args) {
        SpringApplication.run(Fang123Application.class, args);
    }
}
