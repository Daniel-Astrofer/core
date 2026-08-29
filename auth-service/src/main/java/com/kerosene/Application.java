package com.kerosene;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {
        "com.kerosene.auth.model.entity",
        "com.kerosene.notification.model.entity",
        "com.kerosene.content.model.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.kerosene.auth.application.infra.persistence.jpa",
        "com.kerosene.notification.repository",
        "com.kerosene.content.repository"
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
