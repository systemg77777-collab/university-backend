package com.university.ojt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OjtSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(OjtSystemApplication.class, args);
    }
}
 