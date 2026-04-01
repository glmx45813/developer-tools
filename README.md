# Developer Tools - 开发者工具集

一站式开发者工具平台，提供丰富的常用开发工具，提升开发效率。

## 功能特性

### AI工具
- **AI对话** - 支持多种AI模型统一调用（OpenAI、Qwen通义千问、Moonshot月之暗面、Doubao字节豆包、DeepSeek）
- **Prompt模板库** - 精选高质量提示词模板，支持自定义创建和管理

### 加密工具
- MD5加密
- SHA256加密
- AES加密/解密
- RSA加密/解密
- JWT工具（生成与验证）
- Jasypt加密

### 编码转换
- Base64图片编解码
- URL编解码
- Unicode编解码
- HTML实体编解码

### 生成工具
- 二维码生成与解析
- 随机数生成
- 图片处理

### 开发工具
- JSON格式化/压缩/校验
- XML格式化/校验
- YAML工具
- Properties与YAML/JSON互转
- 正则表达式测试
- 时间戳转换
- 文本对比（Diff）
- Cron表达式解析
- SQL格式化

### 网络连接
- HTTP请求测试
- HTTP接口压测
- MQTT连接（发布/订阅）
- TCP连接测试

## 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 2.7.18 | 后端框架 |
| Java 8 | 开发语言 |
| Thymeleaf | 前端模板引擎 |
| H2 Database | 嵌入式数据库 |
| Caffeine | 高性能缓存 |
| Swagger 3.0 | API文档 |
| WebSocket | 实时通信 |
| Netty | TCP客户端 |
| ZXing | 二维码处理 |

## 快速开始

### 环境要求
- JDK 8 或更高版本
- Maven 3.6+

### 构建项目
```bash
mvn clean package -DskipTests
```

### 运行项目
```bash
# 使用jar包运行
java -jar target/developer-tools-1.0.0.jar

# 或使用Maven运行
mvn spring-boot:run
```

### 访问应用
- 主页：http://localhost:8080/tools/
- API文档：http://localhost:8080/tools/swagger-ui/
- H2控制台：http://localhost:8080/tools/h2-console/

### 环境变量配置

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `OPENAI_API_KEY` | OpenAI API密钥 | - |
| `QWEN_API_KEY` | 阿里云Qwen API密钥 | - |
| `MOONSHOT_API_KEY` | Moonshot API密钥 | - |
| `DOUBAO_API_KEY` | 字节Doubao API密钥 | - |
| `DOUBAO_ENDPOINT_ID` | Doubao端点ID | - |
| `DEEPSEEK_API_KEY` | DeepSeek API密钥 | - |

## 部署指南

### 独立部署

1. 构建项目：`mvn clean package -DskipTests`
2. 配置环境变量（可选）
3. 运行：`java -jar target/developer-tools-1.0.0.jar`

### Nginx反向代理部署

如果需要通过域名加前缀的方式访问（如 `/tools`），可以使用项目提供的 `nginx.conf.example` 配置示例：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /tools {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**注意**：使用前缀部署时，需确保 `application.yml` 中 `server.servlet.context-path` 配置为对应的前缀路径。

## 项目结构

```
src/
├── main/
│   ├── java/com/glmx/tools/
│   │   ├── ai/                    # AI模型相关
│   │   │   ├── config/            # AI配置
│   │   │   ├── controller/        # AI控制器
│   │   │   ├── model/             # AI数据模型
│   │   │   ├── provider/          # AI模型提供者
│   │   │   └── service/           # AI服务
│   │   ├── common/                # 公共类
│   │   ├── config/                # 应用配置
│   │   ├── controller/            # 工具控制器
│   │   ├── dto/                   # 数据传输对象
│   │   ├── security/              # 安全相关
│   │   └── service/               # 工具服务
│   └── resources/
│       ├── static/                # 静态资源
│       └── templates/             # HTML模板
└── test/                          # 测试代码
```

## License

MIT License
