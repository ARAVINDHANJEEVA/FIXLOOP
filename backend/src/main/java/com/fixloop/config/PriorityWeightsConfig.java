package com.fixloop.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "fixloop.priority")
@Getter
@Setter
public class PriorityWeightsConfig {
    private double w1 = 0.48;
    private double w2 = 0.21;
    private double w3 = 0.08;
    private double w4 = 0.23;
}
