package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import com.glmx.tools.dto.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PromptOptimizeService {

    private static final Map<String, String> DOMAIN_TEMPLATES = new HashMap<>();
    private static final Set<String> COMMON_WEAK_WORDS = new HashSet<>();
    
    static {
        DOMAIN_TEMPLATES.put("programming", "作为编程专家，请");
        DOMAIN_TEMPLATES.put("writing", "作为专业写作顾问，请");
        DOMAIN_TEMPLATES.put("analysis", "作为数据分析师，请");
        DOMAIN_TEMPLATES.put("translation", "作为专业翻译专家，请");
        DOMAIN_TEMPLATES.put("marketing", "作为营销策划专家，请");
        DOMAIN_TEMPLATES.put("education", "作为教育专家，请");
        DOMAIN_TEMPLATES.put("legal", "作为法律顾问，请");
        DOMAIN_TEMPLATES.put("medical", "作为医学专家，请");
        DOMAIN_TEMPLATES.put("finance", "作为金融分析师，请");
        DOMAIN_TEMPLATES.put("general", "请");
        
        COMMON_WEAK_WORDS.add("一些");
        COMMON_WEAK_WORDS.add("可能");
        COMMON_WEAK_WORDS.add("大概");
        COMMON_WEAK_WORDS.add("好像");
        COMMON_WEAK_WORDS.add("差不多");
        COMMON_WEAK_WORDS.add("随便");
        COMMON_WEAK_WORDS.add("东西");
        COMMON_WEAK_WORDS.add("那个");
        COMMON_WEAK_WORDS.add("这个");
    }
    
    public PromptOptimizeResponse optimize(PromptOptimizeRequest request) {
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            throw new BusinessException("提示词不能为空");
        }
        
        String originalPrompt = request.getPrompt().trim();
        PromptOptimizeResponse response = new PromptOptimizeResponse();
        response.setOriginalPrompt(originalPrompt);
        response.setOriginalLength(originalPrompt.length());
        
        PromptAnalysis analysis = analyzePrompt(originalPrompt);
        response.setAnalysis(analysis);
        
        List<String> extractedKeywords = extractKeywords(originalPrompt);
        response.setExtractedKeywords(extractedKeywords);
        
        List<String> enhancedKeywords = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getEnhanceKeywords())) {
            enhancedKeywords = enhanceKeywords(extractedKeywords);
        }
        response.setEnhancedKeywords(enhancedKeywords);
        
        String optimizedPrompt = originalPrompt;
        
        if (Boolean.TRUE.equals(request.getAddStructure())) {
            optimizedPrompt = addStructure(optimizedPrompt);
        }
        
        if (Boolean.TRUE.equals(request.getImproveClarity())) {
            optimizedPrompt = improveClarity(optimizedPrompt);
        }
        
        String domain = request.getDomain() != null ? request.getDomain() : "general";
        optimizedPrompt = applyDomainTemplate(optimizedPrompt, domain);
        
        if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {
            optimizedPrompt = incorporateKeywords(optimizedPrompt, request.getKeywords());
        }
        
        if (request.getMaxLength() != null && optimizedPrompt.length() > request.getMaxLength()) {
            optimizedPrompt = truncatePrompt(optimizedPrompt, request.getMaxLength());
        }
        
        response.setOptimizedPrompt(optimizedPrompt);
        response.setOptimizedLength(optimizedPrompt.length());
        
        List<String> suggestions = generateSuggestions(analysis, originalPrompt, optimizedPrompt);
        response.setSuggestions(suggestions);
        
        return response;
    }
    
    private PromptAnalysis analyzePrompt(String prompt) {
        PromptAnalysis analysis = new PromptAnalysis();
        List<String> issues = new ArrayList<>();
        List<String> strengths = new ArrayList<>();
        
        double clarityScore = 100.0;
        double completenessScore = 100.0;
        double relevanceScore = 100.0;
        
        if (prompt.length() < 10) {
            issues.add("提示词过短，可能缺少必要信息");
            completenessScore -= 30;
        }
        
        if (prompt.length() > 500) {
            issues.add("提示词较长，可能影响模型理解");
            clarityScore -= 10;
        }
        
        for (String weakWord : COMMON_WEAK_WORDS) {
            if (prompt.contains(weakWord)) {
                issues.add("包含模糊词汇: " + weakWord);
                clarityScore -= 5;
            }
        }
        
        if (!prompt.contains("请") && !prompt.contains("帮我") && !prompt.contains("如何")) {
            issues.add("缺少明确的请求指令");
            completenessScore -= 20;
        }
        
        if (prompt.contains("具体") || prompt.contains("详细") || prompt.contains("步骤")) {
            strengths.add("包含明确的输出要求");
            relevanceScore += 10;
        }
        
        if (prompt.contains("例如") || prompt.contains("比如") || prompt.contains("案例")) {
            strengths.add("包含示例要求");
            relevanceScore += 10;
        }
        
        if (prompt.matches(".*[。！？].*")) {
            strengths.add("语句结构完整");
        } else {
            issues.add("缺少标点符号");
            clarityScore -= 5;
        }
        
        analysis.setIssues(issues);
        analysis.setStrengths(strengths);
        analysis.setClarityScore(Math.max(0, Math.min(100, clarityScore)));
        analysis.setCompletenessScore(Math.max(0, Math.min(100, completenessScore)));
        analysis.setRelevanceScore(Math.max(0, Math.min(100, relevanceScore)));
        
        return analysis;
    }
    
    private List<String> extractKeywords(String prompt) {
        List<String> keywords = new ArrayList<>();
        
        Pattern nounPattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,4}(?=的|是|有|在|和|与|或|，|。|！|？|\\s|$)");
        Matcher matcher = nounPattern.matcher(prompt);
        while (matcher.find()) {
            String word = matcher.group();
            if (!COMMON_WEAK_WORDS.contains(word) && word.length() >= 2) {
                keywords.add(word);
            }
        }
        
        Pattern techPattern = Pattern.compile("[A-Za-z]+(?:[A-Za-z0-9_\\-\\+\\#]+)*");
        matcher = techPattern.matcher(prompt);
        while (matcher.find()) {
            String word = matcher.group();
            if (word.length() >= 2) {
                keywords.add(word);
            }
        }
        
        return keywords.stream().distinct().limit(10).collect(Collectors.toList());
    }
    
    private List<String> enhanceKeywords(List<String> originalKeywords) {
        Map<String, List<String>> keywordSynonyms = new HashMap<>();
        keywordSynonyms.put("代码", Arrays.asList("程序", "脚本", "实现"));
        keywordSynonyms.put("优化", Arrays.asList("改进", "提升", "完善"));
        keywordSynonyms.put("分析", Arrays.asList("解析", "研究", "评估"));
        keywordSynonyms.put("设计", Arrays.asList("规划", "架构", "方案"));
        keywordSynonyms.put("问题", Arrays.asList("疑问", "难题", "困惑"));
        
        List<String> enhanced = new ArrayList<>(originalKeywords);
        for (String keyword : originalKeywords) {
            if (keywordSynonyms.containsKey(keyword)) {
                enhanced.addAll(keywordSynonyms.get(keyword));
            }
        }
        
        return enhanced.stream().distinct().collect(Collectors.toList());
    }
    
    private String addStructure(String prompt) {
        StringBuilder sb = new StringBuilder();
        
        if (!prompt.startsWith("请") && !prompt.startsWith("作为") && !prompt.startsWith("假设")) {
            sb.append("请");
        }
        
        sb.append(prompt);
        
        if (!prompt.contains("要求") && !prompt.contains("格式") && !prompt.contains("输出")) {
            sb.append("\n\n请按以下格式输出：");
            sb.append("\n1. 核心内容");
            sb.append("\n2. 详细说明");
            sb.append("\n3. 示例（如适用）");
        }
        
        return sb.toString();
    }
    
    private String improveClarity(String prompt) {
        String improved = prompt;
        
        for (String weakWord : COMMON_WEAK_WORDS) {
            improved = improved.replace(weakWord, "");
        }
        
        improved = improved.replaceAll("\\s+", " ").trim();
        
        if (!improved.endsWith("。") && !improved.endsWith("？") && !improved.endsWith("！")) {
            improved += "。";
        }
        
        return improved;
    }
    
    private String applyDomainTemplate(String prompt, String domain) {
        String template = DOMAIN_TEMPLATES.getOrDefault(domain, DOMAIN_TEMPLATES.get("general"));
        
        if (!prompt.startsWith(template) && !prompt.startsWith("作为") && !prompt.startsWith("请")) {
            return template + prompt;
        }
        
        return prompt;
    }
    
    private String incorporateKeywords(String prompt, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return prompt;
        }
        
        StringBuilder sb = new StringBuilder(prompt);
        
        boolean hasKeywords = keywords.stream().anyMatch(k -> prompt.contains(k));
        
        if (!hasKeywords) {
            sb.append("\n\n关键词：").append(String.join("、", keywords));
        }
        
        return sb.toString();
    }
    
    private String truncatePrompt(String prompt, int maxLength) {
        if (prompt.length() <= maxLength) {
            return prompt;
        }
        
        int lastSentenceEnd = prompt.lastIndexOf("。", maxLength - 3);
        if (lastSentenceEnd > maxLength / 2) {
            return prompt.substring(0, lastSentenceEnd + 1) + "...";
        }
        
        return prompt.substring(0, maxLength - 3) + "...";
    }
    
    private List<String> generateSuggestions(PromptAnalysis analysis, String original, String optimized) {
        List<String> suggestions = new ArrayList<>();
        
        if (analysis.getClarityScore() < 70) {
            suggestions.add("建议：使用更明确的表述，避免模糊词汇");
        }
        
        if (analysis.getCompletenessScore() < 70) {
            suggestions.add("建议：添加更多上下文信息和具体要求");
        }
        
        if (analysis.getRelevanceScore() < 70) {
            suggestions.add("建议：明确输出格式和期望结果");
        }
        
        if (original.length() < 20) {
            suggestions.add("建议：扩展提示词内容，提供更多背景信息");
        }
        
        if (!original.contains("例如") && !original.contains("案例")) {
            suggestions.add("建议：添加示例以获得更准确的输出");
        }
        
        if (suggestions.isEmpty()) {
            suggestions.add("提示词质量良好，优化后更加结构化和清晰");
        }
        
        return suggestions;
    }
}
