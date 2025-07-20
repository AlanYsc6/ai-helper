package com.example.aihelper.config;

import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
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
            .model("GLM-4.1V-Thinking-FlashX")  // 恢复原来的思考版本
            .temperature(0.6)
            .maxRetries(2)
            .connectTimeout(Duration.ofSeconds(60))
            .readTimeout(Duration.ofSeconds(60))
            .build();
    }

    @Bean
    public StreamingChatModel zhipuStreamingChatModel() {
        try {
            return ZhipuAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .model("GLM-4.1V-Thinking-FlashX")  // 恢复原来的思考版本
                .temperature(0.6)
                .connectTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .build();
        } catch (Exception e) {
            // 如果StreamingChatModel不可用，返回null，让Spring处理
            throw new RuntimeException("无法创建StreamingChatModel: " + e.getMessage(), e);
        }
    }

}
