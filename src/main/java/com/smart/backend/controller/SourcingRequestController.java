package com.smart.backend.controller;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/sourcing")
public class SourcingRequestController {

    private final SqsTemplate sqsTemplate;

    // application.yml에 적은 큐 주소를 가져옵니다
    @Value("${spring.cloud.aws.sqs.endpoint}")
    private String queueUrl;

    public SourcingRequestController(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    // 사용자가 호출할 주소: POST http://localhost:8080/api/sourcing/request?keyword=손난로
    @PostMapping("/request")
    public String requestAnalysis(@RequestParam String keyword) {

        // 1. Python이 좋아하는 JSON 형식으로 메시지 만들기
        // 예: {"keyword": "손난로"}
        String payload = String.format("{\"keyword\": \"%s\"}", keyword);

        // 2. SQS로 발사! 🚀
        sqsTemplate.send(to -> to
                .queue(queueUrl)
                .payload(payload));

        System.out.println("📤 SQS 전송 완료: " + payload);

        return "분석 요청이 접수되었습니다! (키워드: " + keyword + ")";
    }
}
