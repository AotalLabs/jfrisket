package com.aotal.frisket.config;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;
import org.apache.tika.Tika;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by allan on 9/02/17.
 */
@EnableSpringBootMetricsCollector
@EnablePrometheusEndpoint
@Configuration
public class WebConfiguration {

    @Bean
    public AmazonS3 s3() {
        return new AmazonS3Client();
    }

    @Bean
    public AmazonSQS sqs() {
        return new AmazonSQSClient().withRegion(Regions.AP_SOUTHEAST_2);
    }

    @Bean
    public Tika tika() {
        return new Tika();
    }
}
