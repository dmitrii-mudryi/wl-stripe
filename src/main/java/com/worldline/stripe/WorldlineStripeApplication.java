package com.worldline.stripe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WorldlineStripeApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorldlineStripeApplication.class, args);
    }

}
