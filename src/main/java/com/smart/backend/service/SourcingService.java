package com.smart.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SourcingService {

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper; // Spring Boot가 기본 제공하는 Bean 주입

    // application.yml에 설정된 실제 Queue URL을 가져옵니다.
    // 예: https://sqs.ap-northeast-2.amazonaws.com/123456789/market-analysis-queue
    @Value("${spring.cloud.aws.sqs.endpoint}")
    private String queueUrl;

    /**
     * 분석 요청 키워드를 SQS 대기열에 메시지로 전송합니다.
     */
    public void sendAnalysisRequest(String keyword) {
        log.info("🚀 SQS 요청 준비 - 키워드: {}", keyword);

        try {
            // 1. Map을 사용하여 JSON 객체 생성 (String.format보다 안전함)
            Map<String, String> payload = new HashMap<>();
            payload.put("keyword", keyword);

            // 2. Jackson ObjectMapper를 통해 JSON 문자열로 변환
            // 예: keyword가 '아이폰 "케이스"'여도 -> {"keyword": "아이폰 \"케이스\""} 로 안전하게 변환됨
            String messageBody = objectMapper.writeValueAsString(payload);

            // 3. SQS 전송
            sqsTemplate.send(queueUrl, messageBody);

            log.info("✅ SQS 전송 성공: {}", messageBody);
        } catch (Exception e) {
            log.error("❌ SQS 메시지 전송 실패 (키워드: {}): {}", keyword, e.getMessage());
            // 비즈니스 로직에 따라 예외를 다시 던지거나, 여기서 처리(Alert 등)할 수 있음
            throw new RuntimeException("SQS 메시지 전송 중 오류 발생", e);
        }
    }
}