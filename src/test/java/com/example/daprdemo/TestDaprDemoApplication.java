package com.example.daprdemo;

import com.example.daprdemo.config.DaprTestContainersConfig;
import org.springframework.boot.SpringApplication;

public class TestDaprDemoApplication {
    public static void main(String[] args) {
        SpringApplication
            .from(DaprDemoApplication::main)
            .with(DaprTestContainersConfig.class)
            .run(args);
    }
}
