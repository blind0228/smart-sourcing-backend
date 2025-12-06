package com.smart.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class AwsConfig {

    // 1. ~/.aws/credentials 파일에서 복사한 값을 여기에 넣으세요 (따옴표 필수)
    private String accessKey = "ASIA2UC27C24O7O6KYUW"; // 액세스 키
    private String secretKey = "OpJVx74UUhMPXQu8RC6Md/dKRHeEpUJ9NWUPm2K";     // 시크릿 키
    private String sessionToken = "IQoJb3JpZ2luX2VjEKL//////////wEaCXVzLXdlc3QtMiJIMEYCIQCMwmpWLu9qbTFtnZv7zwFj6QHcar826ekfhFK+KQjMwgIhAOkK7kBFzeMo3IzD8czwoeOCwRx/YTtj7Wpq8djQpHSfKqMCCGoQABoMNzMwMzM1MjIxNDMyIgzwJbI0mwb0WZa2KXkqgAKc4kOVA/jlta0Y9vX3kOKEyETuc2Zof8gMYYwZCNdRGuBxyKj2jB7/7OLfhH1TB1Si9MLJBRmGfqbb6scldx+MbTVoSlnq+d/I2c9SJW0l6ChXOJ1Rbh6U6HJQcjBvLDotO1yshmV6OiE8JMYNuYm0+Ki8JomyuTBAUOTT4PNpA897ZAkbuWjQKn4VUGPHqc6W8MYfL6nClPo0fJ64wJUpBFYcmONMbRKBkCcZv8G3RNdo/FRjHk/qDowW73DK5DqmHMErgEla91Q03fLKMndKMMqtm9bk71QKLYk3s47avodZkKqMG0j7TLNKbK58eQIizbffIAU3mXKUZPOQhIhYMOaJzskGOpwBopnNrQD7dTkKi8QRDa7Wi124spIS8xCDOaxrMlPvDVFRLGEkTn7t+7L0Q4zlds1bJi0BI8ljV0Wk1/dV5OcCUBCm8kQnJvxFck5h2AX5hkfIj29j8ebWW7EULfDSAGmgGC5H56Qoe9JVwhf1nnZX/Cbu+OOv9oEjHcwQs1+9AnBQK9G6EbVO104CStxVn72RksX5VhE+QEY33pBR";  // 세션 토큰 (엄청 긴 문자열)

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        // 이 코드가 'Access Key + Secret Key + Token'을 한 묶음으로 만들어줍니다.
        return SqsAsyncClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsSessionCredentials.create(accessKey, secretKey, sessionToken)
                ))
                .build();
    }
}

