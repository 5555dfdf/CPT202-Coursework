package org.example.courework3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class Courework3Application {

    public static void main(String[] args) {
        SpringApplication.run(Courework3Application.class, args);
    }

}
