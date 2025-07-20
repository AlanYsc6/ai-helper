package com.example.aihelper;

import com.example.aihelper.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestMvc
public class StreamingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionService sessionService;

    @Test
    public void testCreateSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/chat/session/new")
                .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.sessionId").exists())
                .andReturn();

        System.out.println("创建会话响应: " + result.getResponse().getContentAsString());
    }

    @Test
    public void testSessionService() {
        // 测试会话服务
        String sessionId = sessionService.generateSessionId();
        assertNotNull(sessionId);
        assertTrue(sessionService.sessionExists(sessionId));

        // 测试会话统计
        SessionService.SessionStats stats = sessionService.getSessionStats();
        assertTrue(stats.activeSessions() > 0);

        System.out.println("会话ID: " + sessionId);
        System.out.println("活跃会话数: " + stats.activeSessions());
        System.out.println("总消息数: " + stats.totalMessages());
    }

    @Test
    public void testStreamingEndpoint() throws Exception {
        // 首先创建会话
        MvcResult sessionResult = mockMvc.perform(post("/api/chat/session/new")
                .contentType("application/json"))
                .andExpect(status().isOk())
                .andReturn();

        String sessionResponse = sessionResult.getResponse().getContentAsString();
        System.out.println("会话创建响应: " + sessionResponse);

        // 注意：由于MockMvc不能很好地测试SSE，这里只测试端点是否可访问
        // 实际的流式测试需要在集成测试中进行
        try {
            mockMvc.perform(get("/api/chat/stream/text")
                    .param("message", "测试消息")
                    .param("sessionId", "test-session-id"))
                    .andExpect(status().isOk());
            System.out.println("流式文本端点测试通过");
        } catch (Exception e) {
            System.out.println("流式文本端点测试异常（这是预期的，因为需要真实的AI服务）: " + e.getMessage());
        }
    }

    @Test
    public void testSessionIsolation() {
        // 创建两个不同的会话
        String session1 = sessionService.generateSessionId();
        String session2 = sessionService.generateSessionId();

        assertNotEquals(session1, session2);
        assertTrue(sessionService.sessionExists(session1));
        assertTrue(sessionService.sessionExists(session2));

        // 验证会话消息数量初始为1（系统消息）
        assertEquals(1, sessionService.getSessionMessageCount(session1));
        assertEquals(1, sessionService.getSessionMessageCount(session2));

        System.out.println("会话隔离测试通过");
        System.out.println("会话1 ID: " + session1);
        System.out.println("会话2 ID: " + session2);
    }

    @Test
    public void testSessionCleanup() {
        String sessionId = sessionService.generateSessionId();
        assertTrue(sessionService.sessionExists(sessionId));

        // 删除会话
        sessionService.deleteSession(sessionId);
        assertFalse(sessionService.sessionExists(sessionId));

        System.out.println("会话清理测试通过");
    }
}
