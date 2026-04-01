package com.glmx.tools.ai.service;

import com.glmx.tools.ai.model.PromptTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PromptTemplateService {

    private final Map<String, PromptTemplate> systemTemplates = new ConcurrentHashMap<>();
    private final Map<String, List<PromptTemplate>> categoryTemplates = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        initSystemTemplates();
    }

    private void initSystemTemplates() {
        List<PromptTemplate> templates = Arrays.asList(
            PromptTemplate.systemTemplate("code-review", "代码审查", "开发辅助", 
                "帮助审查代码质量、发现潜在问题",
                "请作为一名资深的代码审查专家，对以下代码进行全面审查：\n\n```\n{{code}}\n```\n\n请从以下几个方面进行评估：\n1. 代码质量：可读性、可维护性\n2. 潜在问题：Bug、安全漏洞、性能问题\n3. 最佳实践：设计模式、编码规范\n4. 改进建议：具体的优化方案\n\n请用中文回答，并提供具体的代码示例说明问题。"),
            
            PromptTemplate.systemTemplate("code-explain", "代码解释", "开发辅助",
                "解释代码的功能和逻辑",
                "请详细解释以下代码的功能和逻辑：\n\n```\n{{code}}\n```\n\n请包括：\n1. 代码的整体功能说明\n2. 关键逻辑的解释\n3. 使用的技术和设计模式\n4. 代码的执行流程"),
            
            PromptTemplate.systemTemplate("code-refactor", "代码重构", "开发辅助",
                "提供代码重构建议",
                "请对以下代码进行重构优化：\n\n```\n{{code}}\n```\n\n重构要求：\n1. 提高代码可读性\n2. 遵循SOLID原则\n3. 优化性能\n4. 添加必要的注释\n\n请提供重构后的代码和改进说明。"),
            
            PromptTemplate.systemTemplate("code-convert", "代码转换", "开发辅助",
                "将代码从一种语言转换为另一种",
                "请将以下{{source_lang}}代码转换为{{target_lang}}：\n\n```\n{{code}}\n```\n\n要求：\n1. 保持功能完全一致\n2. 遵循目标语言的最佳实践\n3. 添加必要的注释\n\n请提供转换后的完整代码。"),
            
            PromptTemplate.systemTemplate("generate-test", "生成测试用例", "开发辅助",
                "为代码生成单元测试",
                "请为以下代码生成完整的单元测试：\n\n```\n{{code}}\n```\n\n要求：\n1. 覆盖所有公共方法\n2. 包含正常和异常情况\n3. 使用{{test_framework}}框架\n4. 添加清晰的测试描述"),
            
            PromptTemplate.systemTemplate("sql-optimize", "SQL优化", "数据库",
                "优化SQL查询语句",
                "请优化以下SQL语句：\n\n```sql\n{{sql}}\n```\n\n请从以下方面进行优化：\n1. 查询性能优化\n2. 索引建议\n3. SQL最佳实践\n4. 潜在问题分析\n\n请提供优化后的SQL和详细说明。"),
            
            PromptTemplate.systemTemplate("translate", "翻译助手", "文本处理",
                "高质量多语言翻译",
                "请将以下文本翻译成{{target_language}}：\n\n{{text}}\n\n翻译要求：\n1. 保持原文的语气和风格\n2. 使用地道的表达方式\n3. 专业术语保持准确\n4. 如有歧义请标注"),
            
            PromptTemplate.systemTemplate("summarize", "文本摘要", "文本处理",
                "生成文本摘要",
                "请为以下文本生成摘要：\n\n{{text}}\n\n要求：\n1. 提取核心观点\n2. 保持简洁明了\n3. 字数控制在{{max_words}}字以内\n4. 保留关键信息"),
            
            PromptTemplate.systemTemplate("grammar-check", "语法检查", "文本处理",
                "检查并修正文本语法错误",
                "请检查以下文本的语法和表达问题：\n\n{{text}}\n\n请：\n1. 指出语法错误\n2. 提供修改建议\n3. 改善表达方式\n4. 给出修改后的完整文本"),
            
            PromptTemplate.systemTemplate("email-write", "邮件撰写", "写作助手",
                "帮助撰写专业邮件",
                "请帮我写一封{{email_type}}邮件：\n\n主题：{{subject}}\n背景：{{context}}\n\n要求：\n1. 语气：{{tone}}\n2. 语言：{{language}}\n3. 简洁专业\n4. 格式规范"),
            
            PromptTemplate.systemTemplate("doc-write", "文档撰写", "写作助手",
                "帮助撰写技术文档",
                "请帮我撰写一份{{doc_type}}文档：\n\n主题：{{subject}}\n内容要点：{{key_points}}\n\n要求：\n1. 结构清晰\n2. 内容详实\n3. 格式规范\n4. 包含示例"),
            
            PromptTemplate.systemTemplate("api-doc", "API文档生成", "写作助手",
                "根据代码生成API文档",
                "请根据以下代码生成API文档：\n\n```\n{{code}}\n```\n\n文档要求：\n1. 接口描述\n2. 请求参数说明\n3. 响应格式说明\n4. 使用示例\n5. 错误码说明"),
            
            PromptTemplate.systemTemplate("interview-qa", "面试问答", "面试准备",
                "模拟面试问答",
                "请模拟{{position}}岗位的面试，针对以下问题给出专业回答：\n\n问题：{{question}}\n\n要求：\n1. 回答专业全面\n2. 结合实际经验\n3. 突出个人优势\n4. 控制在{{time}}分钟内回答完"),
            
            PromptTemplate.systemTemplate("resume-optimize", "简历优化", "面试准备",
                "优化简历内容",
                "请优化以下简历内容：\n\n{{resume}}\n\n优化方向：\n1. 突出核心技能\n2. 量化工作成果\n3. 使用专业术语\n4. 提高可读性\n\n请提供优化后的版本。"),
            
            PromptTemplate.systemTemplate("data-analysis", "数据分析", "数据分析",
                "分析数据并给出洞察",
                "请分析以下数据：\n\n{{data}}\n\n分析要求：\n1. 数据趋势分析\n2. 异常值识别\n3. 关键指标解读\n4. 改进建议\n\n请用图表和文字说明分析结果。"),
            
            PromptTemplate.systemTemplate("regex-gen", "正则表达式生成", "开发辅助",
                "根据描述生成正则表达式",
                "请根据以下描述生成正则表达式：\n\n需求描述：{{description}}\n\n要求：\n1. 提供完整的正则表达式\n2. 解释各部分的含义\n3. 提供测试用例\n4. 说明边界情况"),
            
            PromptTemplate.systemTemplate("json-schema", "JSON Schema生成", "开发辅助",
                "根据JSON生成Schema",
                "请根据以下JSON数据生成JSON Schema：\n\n```json\n{{json}}\n```\n\n要求：\n1. 完整的字段定义\n2. 类型约束\n3. 必填字段标识\n4. 字段描述说明"),
            
            PromptTemplate.systemTemplate("git-commit", "Git提交信息", "开发辅助",
                "生成规范的Git提交信息",
                "请根据以下代码变更生成Git提交信息：\n\n变更内容：{{changes}}\n\n要求：\n1. 遵循Conventional Commits规范\n2. 简洁明了\n3. 说明变更原因\n4. 格式：type(scope): description"),
            
            PromptTemplate.systemTemplate("readme-gen", "README生成", "写作助手",
                "生成项目README文档",
                "请为以下项目生成README文档：\n\n项目名称：{{project_name}}\n项目描述：{{description}}\n技术栈：{{tech_stack}}\n\n要求包含：\n1. 项目简介\n2. 功能特性\n3. 安装指南\n4. 使用说明\n5. 贡献指南"),
            
            PromptTemplate.systemTemplate("prompt-improve", "提示词优化", "AI技巧",
                "优化提示词以获得更好的AI回答",
                "请优化以下提示词：\n\n原始提示词：{{original_prompt}}\n\n优化要求：\n1. 明确任务目标\n2. 提供必要上下文\n3. 指定输出格式\n4. 添加约束条件\n\n请提供优化后的提示词和优化说明。")
        );
        
        templates.forEach(this::addSystemTemplate);
    }

    private void addSystemTemplate(PromptTemplate template) {
        systemTemplates.put(template.getId(), template);
        categoryTemplates.computeIfAbsent(template.getCategory(), k -> new ArrayList<>())
            .add(template);
    }

    public List<PromptTemplate> getAllSystemTemplates() {
        return new ArrayList<>(systemTemplates.values());
    }

    public PromptTemplate getTemplateById(String id) {
        PromptTemplate template = systemTemplates.get(id);
        if (template != null) {
            template.setUsageCount(template.getUsageCount() + 1);
        }
        return template;
    }

    public List<PromptTemplate> getTemplatesByCategory(String category) {
        return categoryTemplates.getOrDefault(category, Collections.emptyList());
    }

    public List<String> getAllCategories() {
        return new ArrayList<>(categoryTemplates.keySet());
    }

    public List<PromptTemplate> searchTemplates(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllSystemTemplates();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        return systemTemplates.values().stream()
            .filter(t -> t.getName().toLowerCase().contains(lowerKeyword) ||
                        t.getDescription().toLowerCase().contains(lowerKeyword) ||
                        t.getCategory().toLowerCase().contains(lowerKeyword) ||
                        (t.getTags() != null && Arrays.stream(t.getTags())
                            .anyMatch(tag -> tag.toLowerCase().contains(lowerKeyword))))
            .collect(Collectors.toList());
    }

    public String fillTemplate(String id, Map<String, String> variables) {
        PromptTemplate template = getTemplateById(id);
        if (template == null) {
            return null;
        }
        
        String content = template.getContent();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return content;
    }

    public List<PromptTemplate> getHotTemplates(int limit) {
        return systemTemplates.values().stream()
            .sorted((a, b) -> b.getUsageCount().compareTo(a.getUsageCount()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    public boolean isSystemTemplate(String id) {
        return systemTemplates.containsKey(id);
    }
}
