package com.example.aihelper.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiHelperApplicationTests {

    @Resource
    private ZhipuService zhipuService;

    @Test
    void chatTest() {
        String response = zhipuService.chat("你好,你是什么模型");
        System.out.println("AI Response: " + response);
    }

}
