package com.example.aihelper.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SessionService {

    // 存储每个会话的消息历史
    private final Map<String, List<ChatMessage>> sessionMessages = new ConcurrentHashMap<>();
    
    // 会话超时时间（毫秒）
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000; // 30分钟
    
    // 存储会话最后活动时间
    private final Map<String, Long> sessionLastActivity = new ConcurrentHashMap<>();

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
     * 生成新的会话ID
     */
    public String generateSessionId() {
        String sessionId = UUID.randomUUID().toString();
        initSession(sessionId);
        log.info("生成新会话ID: {}", sessionId);
        return sessionId;
    }

    /**
     * 初始化会话
     */
    private void initSession(String sessionId) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(SYSTEM_MESSAGE));
        sessionMessages.put(sessionId, messages);
        sessionLastActivity.put(sessionId, System.currentTimeMillis());
    }

    /**
     * 添加用户消息到会话
     */
    public void addUserMessage(String sessionId, UserMessage userMessage) {
        if (!sessionMessages.containsKey(sessionId)) {
            initSession(sessionId);
        }
        
        sessionMessages.get(sessionId).add(userMessage);
        sessionLastActivity.put(sessionId, System.currentTimeMillis());
        log.debug("添加用户消息到会话 {}, 当前消息数: {}", sessionId, sessionMessages.get(sessionId).size());
    }

    /**
     * 添加AI消息到会话
     */
    public void addAiMessage(String sessionId, String aiResponse) {
        if (!sessionMessages.containsKey(sessionId)) {
            log.warn("会话 {} 不存在，无法添加AI消息", sessionId);
            return;
        }
        
        sessionMessages.get(sessionId).add(AiMessage.from(aiResponse));
        sessionLastActivity.put(sessionId, System.currentTimeMillis());
        log.debug("添加AI消息到会话 {}, 当前消息数: {}", sessionId, sessionMessages.get(sessionId).size());
    }

    /**
     * 获取会话的所有消息
     */
    public List<ChatMessage> getSessionMessages(String sessionId) {
        if (!sessionMessages.containsKey(sessionId)) {
            initSession(sessionId);
        }
        
        sessionLastActivity.put(sessionId, System.currentTimeMillis());
        return new ArrayList<>(sessionMessages.get(sessionId));
    }

    /**
     * 检查会话是否存在
     */
    public boolean sessionExists(String sessionId) {
        return sessionMessages.containsKey(sessionId);
    }

    /**
     * 清理过期会话
     */
    public void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        List<String> expiredSessions = new ArrayList<>();
        
        for (Map.Entry<String, Long> entry : sessionLastActivity.entrySet()) {
            if (currentTime - entry.getValue() > SESSION_TIMEOUT) {
                expiredSessions.add(entry.getKey());
            }
        }
        
        for (String sessionId : expiredSessions) {
            sessionMessages.remove(sessionId);
            sessionLastActivity.remove(sessionId);
            log.info("清理过期会话: {}", sessionId);
        }
        
        if (!expiredSessions.isEmpty()) {
            log.info("清理了 {} 个过期会话", expiredSessions.size());
        }
    }

    /**
     * 获取会话统计信息
     */
    public SessionStats getSessionStats() {
        return new SessionStats(
            sessionMessages.size(),
            sessionMessages.values().stream()
                .mapToInt(List::size)
                .sum()
        );
    }

    /**
     * 会话统计信息
     */
    public record SessionStats(int activeSessions, int totalMessages) {}

    /**
     * 删除指定会话
     */
    public void deleteSession(String sessionId) {
        sessionMessages.remove(sessionId);
        sessionLastActivity.remove(sessionId);
        log.info("删除会话: {}", sessionId);
    }

    /**
     * 获取会话消息数量
     */
    public int getSessionMessageCount(String sessionId) {
        List<ChatMessage> messages = sessionMessages.get(sessionId);
        return messages != null ? messages.size() : 0;
    }
}
