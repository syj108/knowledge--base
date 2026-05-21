package com.example.precip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PrecipApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrecipApplication.class, args);
    }
}
