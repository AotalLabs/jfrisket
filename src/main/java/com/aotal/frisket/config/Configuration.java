package com.aotal.frisket.config;

import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;
import org.apache.tika.Tika;
import org.springframework.context.annotation.Bean;

/**
 * Created by allan on 9/02/17.
 */
@EnableSpringBootMetricsCollector
@EnablePrometheusEndpoint
public class Configuration {

    @Bean
    public Tika tika() {
        return new Tika();
    }
}
