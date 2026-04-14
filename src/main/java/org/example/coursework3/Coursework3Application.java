package org.example.coursework3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
public class Coursework3Application {

    public static void main(String[] args) {
        SpringApplication.run(Coursework3Application.class, args);
    }

}
