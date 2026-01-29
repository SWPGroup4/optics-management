package com.glassystem.optics.configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@FieldDefaults(level = AccessLevel.PUBLIC)
public class S3Config {
    @Value("${aws.s3.access-key}") String accessKey;
    @Value("${aws.s3.secret-key}") String secretKey;
    @Value("${aws.s3.region}") String region;

    @Bean
    S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(()-> AwsBasicCredentials.create(accessKey, secretKey))
                .build();
    }
}
