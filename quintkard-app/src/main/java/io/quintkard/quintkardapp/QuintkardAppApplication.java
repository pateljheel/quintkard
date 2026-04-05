package io.quintkard.quintkardapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class QuintkardAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuintkardAppApplication.class, args);
    }

}
