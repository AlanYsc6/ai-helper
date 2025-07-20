package com.example.aihelper.controller;

import com.example.aihelper.ai.AiCodeService;
import com.example.aihelper.ai.StreamingZhipuService;
import com.example.aihelper.ai.ZhipuService;
import com.example.aihelper.service.SessionService;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private ZhipuService zhipuService;

    @Autowired
    private StreamingZhipuService streamingZhipuService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private AiCodeService aiCodeService;

    /**
     * 生成新的会话ID
     */
    @PostMapping("/session/new")
    public ResponseEntity<SessionResponse> createNewSession() {
        try {
            String sessionId = sessionService.generateSessionId();
            return ResponseEntity.ok(new SessionResponse(true, sessionId, null));
        } catch (Exception e) {
            log.error("创建会话失败", e);
            return ResponseEntity.ok(new SessionResponse(false, null, "创建会话失败: " + e.getMessage()));
        }
    }

    /**
     * 流式文本对话接口
     */
    @GetMapping(value = "/stream/text", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTextChat(
            @RequestParam("message") String message,
            @RequestParam("sessionId") String sessionId) {
        try {
            log.info("收到流式文本对话请求: {}, 会话ID: {}", message, sessionId);

            // 添加用户消息到会话历史
            UserMessage userMessage = UserMessage.from(message);
            sessionService.addUserMessage(sessionId, userMessage);

            return streamingZhipuService.streamChat(message, sessionId);
        } catch (Exception e) {
            log.error("流式文本对话处理失败", e);
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("处理失败: " + e.getMessage()));
                emitter.complete();
            } catch (IOException ioException) {
                emitter.completeWithError(ioException);
            }
            return emitter;
        }
    }

    /**
     * 文本对话接口（保持向后兼容）
     */
    @PostMapping("/text")
    public ResponseEntity<ChatResponse> textChat(@RequestBody ChatRequest request) {
        try {
            log.info("收到文本对话请求: {}", request.getMessage());
            String response = zhipuService.chat(request.getMessage());
            return ResponseEntity.ok(new ChatResponse(true, response, null));
        } catch (Exception e) {
            log.error("文本对话处理失败", e);
            return ResponseEntity.ok(new ChatResponse(false, null, "处理失败: " + e.getMessage()));
        }
    }

    /**
     * 流式多模态对话接口
     */
    @PostMapping(value = "/stream/multimodal", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMultimodalChat(
            @RequestParam("message") String message,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {
        try {
            log.info("收到流式多模态对话请求: {}, 会话ID: {}, 图片数量: {}",
                message, sessionId, images != null ? images.length : 0);

            List<dev.langchain4j.data.message.Content> contents = new ArrayList<>();

            // 添加文本内容
            if (message != null && !message.trim().isEmpty()) {
                contents.add(TextContent.from(message));
            }

            // 添加图片内容
            if (images != null && images.length > 0) {
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        try {
                            byte[] imageBytes = image.getBytes();
                            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                            String mimeType = image.getContentType();
                            String dataUrl = "data:" + mimeType + ";base64," + base64Image;
                            contents.add(ImageContent.from(dataUrl));
                        } catch (IOException e) {
                            log.error("处理图片失败: {}", image.getOriginalFilename(), e);
                        }
                    }
                }
            }

            if (contents.isEmpty()) {
                SseEmitter emitter = new SseEmitter();
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("请提供文本或图片内容"));
                    emitter.complete();
                } catch (IOException ioException) {
                    emitter.completeWithError(ioException);
                }
                return emitter;
            }

            UserMessage userMessage = UserMessage.from(contents);
            // 添加用户消息到会话历史
            sessionService.addUserMessage(sessionId, userMessage);

            return streamingZhipuService.streamChat(userMessage, sessionId);
        } catch (Exception e) {
            log.error("流式多模态对话处理失败", e);
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("处理失败: " + e.getMessage()));
                emitter.complete();
            } catch (IOException ioException) {
                emitter.completeWithError(ioException);
            }
            return emitter;
        }
    }

    /**
     * 多模态对话接口（文本+图片）（保持向后兼容）
     */
    @PostMapping("/multimodal")
    public ResponseEntity<ChatResponse> multimodalChat(
            @RequestParam("message") String message,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {
        try {
            log.info("收到多模态对话请求: {}, 图片数量: {}", message, images != null ? images.length : 0);
            
            List<dev.langchain4j.data.message.Content> contents = new ArrayList<>();
            
            // 添加文本内容
            if (message != null && !message.trim().isEmpty()) {
                contents.add(TextContent.from(message));
            }
            
            // 添加图片内容
            if (images != null && images.length > 0) {
                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        try {
                            byte[] imageBytes = image.getBytes();
                            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                            String mimeType = image.getContentType();
                            String dataUrl = "data:" + mimeType + ";base64," + base64Image;
                            contents.add(ImageContent.from(dataUrl));
                        } catch (IOException e) {
                            log.error("处理图片失败: {}", image.getOriginalFilename(), e);
                        }
                    }
                }
            }
            
            if (contents.isEmpty()) {
                return ResponseEntity.ok(new ChatResponse(false, null, "请提供文本或图片内容"));
            }
            
            UserMessage userMessage = UserMessage.from(contents);
            String response = zhipuService.chat(userMessage);
            
            return ResponseEntity.ok(new ChatResponse(true, response, null));
        } catch (Exception e) {
            log.error("多模态对话处理失败", e);
            return ResponseEntity.ok(new ChatResponse(false, null, "处理失败: " + e.getMessage()));
        }
    }

    /**
     * 获取学习报告
     */
    @PostMapping("/report")
    public ResponseEntity<ReportResponse> getReport(@RequestBody ChatRequest request) {
        try {
            log.info("收到学习报告请求: {}", request.getMessage());
            AiCodeService.Report report = aiCodeService.chatForReport(request.getMessage());
            return ResponseEntity.ok(new ReportResponse(true, report, null));
        } catch (Exception e) {
            log.error("学习报告生成失败", e);
            return ResponseEntity.ok(new ReportResponse(false, null, "处理失败: " + e.getMessage()));
        }
    }

    /**
     * 聊天请求对象
     */
    public static class ChatRequest {
        private String message;

        public ChatRequest() {}

        public ChatRequest(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * 聊天响应对象
     */
    public static class ChatResponse {
        private boolean success;
        private String message;
        private String error;

        public ChatResponse() {}

        public ChatResponse(boolean success, String message, String error) {
            this.success = success;
            this.message = message;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    /**
     * 报告响应对象
     */
    public static class ReportResponse {
        private boolean success;
        private AiCodeService.Report report;
        private String error;

        public ReportResponse() {}

        public ReportResponse(boolean success, AiCodeService.Report report, String error) {
            this.success = success;
            this.report = report;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public AiCodeService.Report getReport() {
            return report;
        }

        public void setReport(AiCodeService.Report report) {
            this.report = report;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    /**
     * 会话响应对象
     */
    public static class SessionResponse {
        private boolean success;
        private String sessionId;
        private String error;

        public SessionResponse() {}

        public SessionResponse(boolean success, String sessionId, String error) {
            this.success = success;
            this.sessionId = sessionId;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
