package com.example.aihelper.ai;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class StreamingZhipuService {

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

    /**
     * 流式文本对话
     */
    public SseEmitter streamChat(String message, String sessionId) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        CompletableFuture.runAsync(() -> {
            try {
                SystemMessage systemMessage = SystemMessage.from(SYSTEM_MESSAGE);
                UserMessage userMessage = UserMessage.from(message);

                // 使用普通ChatModel获取完整响应
                ChatResponse response = chatModel.chat(systemMessage, userMessage);
                String fullResponse = response.aiMessage().text();

                // 模拟流式输出，逐字符发送
                simulateStreamingOutput(emitter, fullResponse, sessionId);

            } catch (Exception e) {
                log.error("启动流式对话失败，会话ID: {}", sessionId, e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("启动对话失败: " + e.getMessage()));
                } catch (IOException ioException) {
                    log.error("发送启动错误信息失败", ioException);
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 流式多模态对话
     */
    public SseEmitter streamChat(UserMessage userMessage, String sessionId) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        CompletableFuture.runAsync(() -> {
            try {
                // 使用普通ChatModel获取完整响应
                ChatResponse response = chatModel.chat(userMessage);
                String fullResponse = response.aiMessage().text();

                // 模拟流式输出
                simulateStreamingOutput(emitter, fullResponse, sessionId);

            } catch (Exception e) {
                log.error("启动流式多模态对话失败，会话ID: {}", sessionId, e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("启动对话失败: " + e.getMessage()));
                } catch (IOException ioException) {
                    log.error("发送启动错误信息失败", ioException);
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 模拟流式输出
     */
    private void simulateStreamingOutput(SseEmitter emitter, String fullResponse, String sessionId) {
        try {
            // 按词语分割，模拟更自然的流式输出
            String[] words = fullResponse.split("(?<=\\s)|(?=\\s)|(?<=。)|(?=。)|(?<=！)|(?=！)|(?<=？)|(?=？)");

            for (String word : words) {
                if (word.trim().isEmpty()) continue;

                try {
                    // 发送流式数据
                    emitter.send(SseEmitter.event()
                        .name("token")
                        .data(word));

                    // 添加小延迟，模拟真实的流式输出
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    log.error("发送SSE数据失败", e);
                    emitter.completeWithError(e);
                    return;
                }
            }

            // 发送完成信号
            emitter.send(SseEmitter.event()
                .name("complete")
                .data(""));
            emitter.complete();

            log.info("流式对话完成，会话ID: {}, 响应长度: {}", sessionId, fullResponse.length());

        } catch (IOException e) {
            log.error("发送完成信号失败", e);
            emitter.completeWithError(e);
        }
    }
}
