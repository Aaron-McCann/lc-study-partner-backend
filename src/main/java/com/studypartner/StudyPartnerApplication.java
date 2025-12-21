package com.studypartner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class StudyPartnerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyPartnerApplication.class, args);
    }
}