package com.example.demo.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thêm .env vào Environment (sau {@code systemEnvironment}). Luồng chính vẫn là
 * {@link com.example.demo.DemoApplication#main} gọi {@link DotEnvFileSupport} trước khi Spring chạy.
 */
public class WorkspaceDotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String SOURCE_NAME = "workspaceDotenvFiles";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, String> merged = DotEnvFileSupport.readMergedDotEnv();
        if (merged.isEmpty()) {
            return;
        }
        Map<String, Object> asObjectMap = new LinkedHashMap<>(merged);
        MutablePropertySources sources = environment.getPropertySources();
        if (sources.contains("systemEnvironment")) {
            sources.addAfter("systemEnvironment", new MapPropertySource(SOURCE_NAME, asObjectMap));
        } else {
            sources.addLast(new MapPropertySource(SOURCE_NAME, asObjectMap));
        }
    }
}
