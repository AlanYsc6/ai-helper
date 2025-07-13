package com.example.aihelper.config;

import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class ZhipuAiConfig {

    @Value("${zhipu.key}")
    private String apiKey;

    @Bean
    public ChatModel zhipuChatModel() {
        return ZhipuAiChatModel.builder()
            .apiKey(apiKey)
            .model("GLM-4.1V-Thinking-FlashX")
            .temperature(0.6)
            .maxToken(1024)
            .maxRetries(2)
            .connectTimeout(Duration.ofSeconds(60))
            .readTimeout(Duration.ofSeconds(60))
            .build();
    }

}
