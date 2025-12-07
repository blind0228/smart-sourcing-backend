// com.smart.backend.service.SourcingService.java

package com.smart.backend.service;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SourcingService {

    private final SqsTemplate sqsTemplate;

    // application.yml 또는 AWS SQS 설정에서 큐 URL을 주입
    // 예시: application.yml에 spring.cloud.aws.sqs.endpoint 설정 필요
    @Value("${spring.cloud.aws.sqs.endpoint}")
    private String queueUrl;

    /**
     * 분석 요청 키워드를 SQS 대기열에 메시지로 전송합니다.
     */
    public void sendAnalysisRequest(String keyword) {
        log.info("SQS 요청 전송 시작: {}", keyword);

        try {
            // Python Worker가 파싱할 수 있도록 JSON 형태로 전송
            String messageBody = String.format("{\"keyword\": \"%s\"}", keyword);

            sqsTemplate.send(queueUrl, messageBody);

            log.info("SQS 요청 전송 성공: {}", keyword);
        } catch (Exception e) {
            log.error("SQS 메시지 전송 실패 (키워드: {}): {}", keyword, e.getMessage());
            throw new RuntimeException("SQS 메시지 전송 중 오류 발생", e);
        }
    }
}