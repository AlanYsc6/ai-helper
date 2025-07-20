class ChatApp {
    constructor() {
        this.currentTab = 'chat';
        this.selectedImages = [];
        this.sessionId = null;
        this.currentStreamingMessage = null;
        this.init();
    }

    async init() {
        await this.createSession();
        this.bindEvents();
        this.initImageUpload();
    }

    /**
     * 创建新的会话
     */
    async createSession() {
        try {
            const response = await fetch('/api/chat/session/new', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            const data = await response.json();
            if (data.success) {
                this.sessionId = data.sessionId;
                console.log('创建会话成功，会话ID:', this.sessionId);
                this.updateSessionStatus('connected', '会话已连接');
            } else {
                console.error('创建会话失败:', data.error);
                this.sessionId = 'fallback-' + Date.now(); // 使用备用会话ID
                this.updateSessionStatus('disconnected', '会话创建失败');
            }
        } catch (error) {
            console.error('创建会话请求失败:', error);
            this.sessionId = 'fallback-' + Date.now(); // 使用备用会话ID
            this.updateSessionStatus('disconnected', '连接失败');
        }
    }

    /**
     * 更新会话状态指示器
     */
    updateSessionStatus(status, message) {
        const statusElement = document.getElementById('sessionStatus');
        if (statusElement) {
            statusElement.className = `session-status ${status}`;
            statusElement.innerHTML = `<i class="fas fa-circle"></i> ${message}`;
        }
    }

    bindEvents() {
        // 选项卡切换
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.switchTab(e.target.dataset.tab);
            });
        });

        // 发送按钮事件
        document.getElementById('sendTextBtn').addEventListener('click', () => {
            this.sendTextMessage();
        });

        document.getElementById('sendMultimodalBtn').addEventListener('click', () => {
            this.sendMultimodalMessage();
        });

        document.getElementById('sendReportBtn').addEventListener('click', () => {
            this.sendReportRequest();
        });

        // 回车发送消息
        document.getElementById('messageInput').addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendTextMessage();
            }
        });

        document.getElementById('multimodalMessageInput').addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMultimodalMessage();
            }
        });

        document.getElementById('reportMessageInput').addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendReportRequest();
            }
        });
    }

    switchTab(tab) {
        // 更新选项卡状态
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        document.querySelector(`[data-tab="${tab}"]`).classList.add('active');

        // 显示对应的输入区域
        document.querySelectorAll('.input-section').forEach(section => {
            section.classList.add('hidden');
        });
        
        if (tab === 'chat') {
            document.getElementById('textInput').classList.remove('hidden');
        } else if (tab === 'multimodal') {
            document.getElementById('multimodalInput').classList.remove('hidden');
        } else if (tab === 'report') {
            document.getElementById('reportInput').classList.remove('hidden');
        }

        this.currentTab = tab;
    }

    initImageUpload() {
        const uploadArea = document.getElementById('imageUploadArea');
        const imageInput = document.getElementById('imageInput');

        // 点击上传
        uploadArea.addEventListener('click', () => {
            imageInput.click();
        });

        // 文件选择
        imageInput.addEventListener('change', (e) => {
            this.handleImageFiles(e.target.files);
        });

        // 拖拽上传
        uploadArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            uploadArea.classList.add('dragover');
        });

        uploadArea.addEventListener('dragleave', () => {
            uploadArea.classList.remove('dragover');
        });

        uploadArea.addEventListener('drop', (e) => {
            e.preventDefault();
            uploadArea.classList.remove('dragover');
            this.handleImageFiles(e.dataTransfer.files);
        });
    }

    handleImageFiles(files) {
        Array.from(files).forEach(file => {
            if (file.type.startsWith('image/')) {
                this.selectedImages.push(file);
                this.displayImagePreview(file);
            }
        });
    }

    displayImagePreview(file) {
        const preview = document.getElementById('imagePreview');
        const reader = new FileReader();

        reader.onload = (e) => {
            const imageItem = document.createElement('div');
            imageItem.className = 'image-item';
            imageItem.innerHTML = `
                <img src="${e.target.result}" alt="预览图片">
                <button class="image-remove" onclick="chatApp.removeImage(${this.selectedImages.length - 1})">
                    <i class="fas fa-times"></i>
                </button>
            `;
            preview.appendChild(imageItem);
        };

        reader.readAsDataURL(file);
    }

    removeImage(index) {
        this.selectedImages.splice(index, 1);
        this.refreshImagePreview();
    }

    refreshImagePreview() {
        const preview = document.getElementById('imagePreview');
        preview.innerHTML = '';
        this.selectedImages.forEach((file, index) => {
            this.displayImagePreview(file);
        });
    }

    async sendTextMessage() {
        const input = document.getElementById('messageInput');
        const message = input.value.trim();

        if (!message) return;
        if (!this.sessionId) {
            await this.createSession();
        }

        this.addMessage(message, 'user');
        input.value = '';

        // 创建流式消息容器
        this.currentStreamingMessage = this.addStreamingMessage();

        try {
            const url = `/api/chat/stream/text?message=${encodeURIComponent(message)}&sessionId=${encodeURIComponent(this.sessionId)}`;
            const eventSource = new EventSource(url);

            eventSource.onmessage = (event) => {
                // 默认处理，通常不会用到
                console.log('Default message:', event.data);
            };

            eventSource.addEventListener('token', (event) => {
                this.appendToStreamingMessage(event.data);
            });

            eventSource.addEventListener('complete', (event) => {
                this.completeStreamingMessage();
                eventSource.close();
            });

            eventSource.addEventListener('error', (event) => {
                console.error('SSE Error:', event);
                this.completeStreamingMessage();
                this.addMessage(`错误: ${event.data || '连接中断'}`, 'bot');
                eventSource.close();
            });

            eventSource.onerror = (error) => {
                console.error('EventSource failed:', error);
                this.completeStreamingMessage();
                this.addMessage('网络连接错误，请重试', 'bot');
                eventSource.close();
            };

        } catch (error) {
            this.completeStreamingMessage();
            this.addMessage(`网络错误: ${error.message}`, 'bot');
        }
    }

    async sendMultimodalMessage() {
        const input = document.getElementById('multimodalMessageInput');
        const message = input.value.trim();

        if (!message && this.selectedImages.length === 0) {
            alert('请输入文字或上传图片');
            return;
        }

        if (!this.sessionId) {
            await this.createSession();
        }

        // 显示用户消息
        let userMessageContent = message;
        if (this.selectedImages.length > 0) {
            userMessageContent += ` [包含 ${this.selectedImages.length} 张图片]`;
        }
        this.addMessage(userMessageContent, 'user');

        input.value = '';

        // 创建流式消息容器
        this.currentStreamingMessage = this.addStreamingMessage();

        try {
            const formData = new FormData();
            formData.append('message', message);
            formData.append('sessionId', this.sessionId);

            this.selectedImages.forEach((image, index) => {
                formData.append('images', image);
            });

            const response = await fetch('/api/chat/stream/multimodal', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                const chunk = decoder.decode(value);
                const lines = chunk.split('\n');

                for (const line of lines) {
                    if (line.startsWith('data: ')) {
                        const data = line.slice(6);
                        if (data.trim()) {
                            try {
                                const eventData = JSON.parse(data);
                                if (eventData.type === 'token') {
                                    this.appendToStreamingMessage(eventData.data);
                                } else if (eventData.type === 'complete') {
                                    this.completeStreamingMessage();
                                } else if (eventData.type === 'error') {
                                    this.completeStreamingMessage();
                                    this.addMessage(`错误: ${eventData.data}`, 'bot');
                                }
                            } catch (e) {
                                // 可能是纯文本数据
                                this.appendToStreamingMessage(data);
                            }
                        }
                    } else if (line.startsWith('event: ')) {
                        // 处理事件类型，如果需要的话
                    }
                }
            }

            this.completeStreamingMessage();

        } catch (error) {
            this.completeStreamingMessage();
            this.addMessage(`网络错误: ${error.message}`, 'bot');
        }

        // 清空图片选择
        this.selectedImages = [];
        document.getElementById('imagePreview').innerHTML = '';
    }

    async sendReportRequest() {
        const input = document.getElementById('reportMessageInput');
        const message = input.value.trim();

        if (!message) return;

        this.addMessage(message, 'user');
        input.value = '';
        this.showLoading();

        try {
            const response = await fetch('/api/chat/report', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ message })
            });

            const data = await response.json();
            this.hideLoading();

            if (data.success) {
                this.addReportMessage(data.report);
            } else {
                this.addMessage(`错误: ${data.error}`, 'bot');
            }
        } catch (error) {
            this.hideLoading();
            this.addMessage(`网络错误: ${error.message}`, 'bot');
        }
    }

    addMessage(content, sender) {
        const messagesContainer = document.getElementById('chatMessages');
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${sender}-message`;

        const avatar = sender === 'bot' ? '<i class="fas fa-robot"></i>' : '<i class="fas fa-user"></i>';

        messageDiv.innerHTML = `
            <div class="message-avatar">
                ${avatar}
            </div>
            <div class="message-content">
                <p>${this.formatMessage(content)}</p>
            </div>
        `;

        messagesContainer.appendChild(messageDiv);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
        return messageDiv;
    }

    /**
     * 添加流式消息容器
     */
    addStreamingMessage() {
        const messagesContainer = document.getElementById('chatMessages');
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message bot-message streaming';

        messageDiv.innerHTML = `
            <div class="message-avatar">
                <i class="fas fa-robot"></i>
            </div>
            <div class="message-content">
                <p class="streaming-content"></p>
                <div class="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
            </div>
        `;

        messagesContainer.appendChild(messageDiv);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
        return messageDiv;
    }

    /**
     * 向流式消息追加内容
     */
    appendToStreamingMessage(token) {
        if (!this.currentStreamingMessage) return;

        const contentElement = this.currentStreamingMessage.querySelector('.streaming-content');
        if (contentElement) {
            contentElement.textContent += token;
            // 滚动到底部
            const messagesContainer = document.getElementById('chatMessages');
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        }
    }

    /**
     * 完成流式消息
     */
    completeStreamingMessage() {
        if (!this.currentStreamingMessage) return;

        // 移除流式样式和打字指示器
        this.currentStreamingMessage.classList.remove('streaming');
        const typingIndicator = this.currentStreamingMessage.querySelector('.typing-indicator');
        if (typingIndicator) {
            typingIndicator.remove();
        }

        // 格式化最终内容
        const contentElement = this.currentStreamingMessage.querySelector('.streaming-content');
        if (contentElement) {
            const finalContent = contentElement.textContent;
            contentElement.innerHTML = this.formatMessage(finalContent);
        }

        this.currentStreamingMessage = null;
    }

    addReportMessage(report) {
        const messagesContainer = document.getElementById('chatMessages');
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message bot-message';

        const suggestionsList = report.suggestionList.map(item => `<li>${item}</li>`).join('');
        
        messageDiv.innerHTML = `
            <div class="message-avatar">
                <i class="fas fa-robot"></i>
            </div>
            <div class="message-content">
                <h4>📊 学习报告 - ${report.name}</h4>
                <h5>💡 建议清单：</h5>
                <ul>${suggestionsList}</ul>
            </div>
        `;

        messagesContainer.appendChild(messageDiv);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    formatMessage(message) {
        // 处理思考和回答标签的格式化
        let formattedMessage = message.replace(/\n/g, '<br>');

        // 处理【思考】标签
        formattedMessage = formattedMessage.replace(
            /【思考】([\s\S]*?)【\/思考】/g,
            '<div class="thinking-section"><div class="thinking-header"><i class="fas fa-brain"></i> 思考过程</div><div class="thinking-content">$1</div></div>'
        );

        // 处理【回答】标签
        formattedMessage = formattedMessage.replace(
            /【回答】([\s\S]*?)【\/回答】/g,
            '<div class="answer-section"><div class="answer-header"><i class="fas fa-lightbulb"></i> 最终回答</div><div class="answer-content">$1</div></div>'
        );

        return formattedMessage;
    }

    showLoading() {
        document.getElementById('loadingOverlay').classList.remove('hidden');
    }

    hideLoading() {
        document.getElementById('loadingOverlay').classList.add('hidden');
    }
}

// 初始化应用
const chatApp = new ChatApp();
