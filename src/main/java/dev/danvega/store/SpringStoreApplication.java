package dev.danvega.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringStoreApplication.class, args);
    }
}

