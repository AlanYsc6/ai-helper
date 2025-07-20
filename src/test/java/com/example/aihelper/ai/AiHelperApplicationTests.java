package com.example.aihelper.ai;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiHelperApplicationTests {

    @Resource
    private ZhipuService zhipuService;

    @Resource
    private AiCodeService aiCodeService;

    @Test
    void chatTest() {
        String response = aiCodeService.chat("你好,你是什么模型");
        System.out.println("AI Response: " + response);
    }

    @Test
    void multimodalChatTest() {
        UserMessage userMessage = UserMessage.from(
            TextContent.from("描述图片"),
            ImageContent.from("https://www.codefather.cn/logo.png")
        );
        String response = zhipuService.chat(userMessage);
        System.out.println("AI Response: " + response);
    }

}
