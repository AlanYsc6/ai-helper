# AI编程助手 - 多模态对话系统

这是一个基于Spring Boot和LangChain4j的AI编程助手，支持文本和图片的多模态对话功能。

## 功能特性

### 🤖 智能对话
- 支持纯文本对话
- 编程学习路线规划
- 项目学习建议
- 求职面试指导

### 🖼️ 多模态对话
- 支持图片上传和分析
- 文本+图片组合对话
- 拖拽上传图片
- 多图片同时上传

### 📊 学习报告
- 个性化学习路线生成
- 技能评估和建议
- 结构化报告输出

## 技术栈

### 后端
- **Spring Boot 3.5.3** - Web框架
- **LangChain4j 1.1.0** - AI集成框架
- **智谱AI GLM-4.1V-Thinking-FlashX** - 多模态AI模型
- **Java 21** - 编程语言

### 前端
- **HTML5/CSS3/JavaScript** - 原生Web技术
- **Font Awesome** - 图标库
- **响应式设计** - 支持移动端

## 项目结构

```
src/
├── main/
│   ├── java/com/example/aihelper/
│   │   ├── ai/                          # AI服务层
│   │   │   ├── AiCodeService.java       # AI服务接口
│   │   │   ├── AiCodeHelperServiceFactory.java  # AI服务工厂
│   │   │   └── ZhipuService.java        # 智谱AI服务实现
│   │   ├── config/                      # 配置类
│   │   │   ├── WebConfig.java           # Web配置
│   │   │   └── ZhipuAiConfig.java       # 智谱AI配置
│   │   ├── controller/                  # 控制器层
│   │   │   ├── ChatController.java      # 聊天API控制器
│   │   │   └── HomeController.java      # 首页控制器
│   │   └── AiHelperApplication.java     # 主应用类
│   └── resources/
│       ├── static/                      # 静态资源
│       │   ├── css/style.css           # 样式文件
│       │   ├── js/app.js               # 前端JavaScript
│       │   └── index.html              # 主页面
│       ├── application.yml             # 应用配置
│       └── system-prompt.txt           # 系统提示词
└── test/                               # 测试代码
```

## 快速开始

### 1. 环境要求
- Java 21+
- Maven 3.6+
- 智谱AI API Key

### 2. 配置API Key
创建 `src/main/resources/application-loc.yml` 文件：

```yaml
zhipu:
  key: your-zhipu-ai-api-key-here
```

### 3. 启动应用
```bash
mvn spring-boot:run
```

### 4. 访问应用
打开浏览器访问：http://localhost:8080

## API接口

### 文本对话
```http
POST /api/chat/text
Content-Type: application/json

{
  "message": "你好，我想学习Java"
}
```

### 多模态对话
```http
POST /api/chat/multimodal
Content-Type: multipart/form-data

message: 请分析这张图片
images: [图片文件]
```

### 学习报告
```http
POST /api/chat/report
Content-Type: application/json

{
  "message": "我是Alan，Java开发2年经验"
}
```

## 界面功能

### 智能对话模式
- 输入文本问题
- 获得AI助手的专业回答
- 支持编程学习和求职相关问题

### 多模态对话模式
- 上传图片（支持JPG、PNG、GIF）
- 结合文字描述进行提问
- AI分析图片内容并回答问题

### 学习报告模式
- 输入个人基本信息
- 生成个性化学习建议
- 获得结构化的学习路线

## 特色功能

### 🎨 现代化UI设计
- 渐变色背景
- 卡片式布局
- 平滑动画效果
- 响应式设计

### 📱 移动端适配
- 自适应布局
- 触摸友好的交互
- 优化的移动端体验

### 🚀 高性能
- 异步处理
- 加载状态提示
- 错误处理机制

## 开发说明

### 添加新功能
1. 在 `AiCodeService` 接口中定义新方法
2. 在 `ChatController` 中添加对应的API端点
3. 在前端添加相应的UI和交互逻辑

### 自定义AI模型
修改 `ZhipuAiConfig.java` 中的模型配置：
```java
.model("GLM-4.1V-Thinking-FlashX")  // 更换为其他模型
.temperature(0.6)                   // 调整创造性
.maxToken(1024)                     // 调整响应长度
```

### 修改系统提示词
编辑 `src/main/resources/system-prompt.txt` 文件来自定义AI助手的行为。

## 贡献指南

1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证。

## 联系方式

如有问题或建议，请提交 Issue 或 Pull Request。
