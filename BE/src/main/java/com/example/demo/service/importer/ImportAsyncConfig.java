package com.example.demo.service.importer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class ImportAsyncConfig {

    @Bean(name = "importJobExecutor")
    public Executor importJobExecutor() {
        return Executors.newFixedThreadPool(2);
    }
}
