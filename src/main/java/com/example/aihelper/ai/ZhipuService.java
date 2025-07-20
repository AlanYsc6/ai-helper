package com.example.aihelper.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
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

    private static final String SYSTEM_MESSAGE = """
        你是编程领域的小助手，帮助用户解答编程学习和求职面试相关的问题，并给出建议。重点关注 4 个方向：
        1. 规划清晰的编程学习路线
        2. 提供项目学习建议
        3. 给出程序员求职全流程指南（比如简历优化、投递技巧）
        4. 分享高频面试题和面试技巧

        回答格式要求：
        - 如果有思考过程，请用【思考】标签包围思考内容
        - 最终回答请用【回答】标签包围
        - 回答要详细完整，不要因为长度限制而省略重要内容
        - 用简洁易懂的语言回答，助力用户高效学习与求职

        示例格式：
        【思考】
        这里是我的思考过程...
        【/思考】

        【回答】
        这里是我的最终回答...
        【/回答】
        """;

    public String chat(String message) {
        SystemMessage systemMessage = SystemMessage.from(SYSTEM_MESSAGE);
        UserMessage userMessage = UserMessage.from(message);
        ChatResponse response = chatModel.chat(systemMessage, userMessage);
        AiMessage aiMessage = response.aiMessage();
        log.info("AI Response: {}", aiMessage.toString());
        return aiMessage.text();
    }

    public String chat(UserMessage userMessage) {
        ChatResponse response = chatModel.chat(userMessage);
        AiMessage aiMessage = response.aiMessage();
        log.info("AI Response: {}", aiMessage.toString());
        return aiMessage.text();
    }

}
