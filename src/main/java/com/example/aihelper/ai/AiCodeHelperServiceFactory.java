package com.example.aihelper.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;

//@Configuration
public class AiCodeHelperServiceFactory {

    @Resource
    private ChatModel chatModel;


    @Bean
    public AiCodeService aiCodeService() {
        return AiServices.create(AiCodeService.class, chatModel);
    }


}

