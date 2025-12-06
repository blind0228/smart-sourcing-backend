package com.smart.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import software.amazon.awssdk.auth.credentials.AwsSessionCredentials; <- 이제 필요 없음
// import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider; <- 이제 필요 없음
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider; // 🌟 새로 추가
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class AwsConfig {

    // ⚠️ 이 하드코딩된 키와 토큰 변수들을 모두 삭제하거나 비워주세요!
    /*
    private String accessKey = "ASIA...";
    private String secretKey = "...";
    private String sessionToken = null;
    */

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        System.out.println("🔥 AWS SQS 클라이언트 환경 변수 기반으로 로드...");

        return SqsAsyncClient.builder()
                .region(Region.US_EAST_1)
                // 👇 핵심: 이제 파일이 아닌 환경변수/시스템을 먼저 확인합니다.
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}