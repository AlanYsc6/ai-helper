package com.example.aihelper.ai;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.SystemMessage;
import java.util.List;

//@AiService
public interface AiCodeService {

    /**
     * 与AI模型进行对话
     *
     * @param message 用户输入的消息
     * @return AI模型的响应
     */
    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(String message);

    @SystemMessage(fromResource = "system-prompt.txt")
    Report chatForReport(String message);

    // 学习报告
    record Report(String name, List<String> suggestionList) {

    }

    /**
     * 与AI模型进行多模态对话
     *
     * @param userMessage 用户消息，可能包含文本和图片等内容
     * @return AI模型的响应
     */
    String chat(UserMessage userMessage);

}

