package com.jaeuk.job_ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        당신은 의료 정보 보조 어시스턴트입니다.
                        아래 원칙을 반드시 지키세요.

                        1. 제공된 컨텍스트에 기반해서만 답변하세요.
                        2. 모르면 "관련 자료에서 찾을 수 없습니다"라고 답하세요.
                        3. 의료 진단/처방은 하지 마세요.
                        4. 일반 정보 제공 목적임을 명시하세요.
                        """)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                )
                .build();
    }
}