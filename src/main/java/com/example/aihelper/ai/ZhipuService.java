package com.example.aihelper.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ZhipuService {

    @Resource
    private ChatModel chatModel;

    public String chat(String message) {
        UserMessage userMessage = UserMessage.from(message);
        ChatResponse response = chatModel.chat(userMessage);
        AiMessage aiMessage = response.aiMessage();
        log.info("AI Response: {}", aiMessage.toString());
        return aiMessage.text();
    }

}
