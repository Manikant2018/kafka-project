package com.api.logging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class KafkaProjectApplication {

    public static void main(String[] args) {
        // Set JVM timezone to UTC before Spring context starts
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.setProperty("user.timezone", "UTC");

        SpringApplication.run(KafkaProjectApplication.class, args);
    }
}
