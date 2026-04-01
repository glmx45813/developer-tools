package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DiffService {

    public DiffResult compare(String text1, String text2, String mode) {
        if (text1 == null) text1 = "";
        if (text2 == null) text2 = "";
        
        if (mode == null || mode.isEmpty()) {
            mode = "line";
        }
        
        DiffResult result = new DiffResult();
        
        switch (mode.toLowerCase()) {
            case "char":
                result = compareByChar(text1, text2);
                break;
            case "word":
                result = compareByWord(text1, text2);
                break;
            default:
                result = compareByLine(text1, text2);
        }
        
        return result;
    }

    private DiffResult compareByLine(String text1, String text2) {
        String[] lines1 = text1.split("\n");
        String[] lines2 = text2.split("\n");
        
        List<DiffItem> diffs = diff(lines1, lines2);
        
        DiffResult result = new DiffResult();
        result.setDiffs(diffs);
        result.setMode("line");
        result.setStats(calculateStats(diffs));
        return result;
    }

    private DiffResult compareByChar(String text1, String text2) {
        String[] chars1 = text1.split("");
        String[] chars2 = text2.split("");
        
        List<DiffItem> diffs = diff(chars1, chars2);
        
        DiffResult result = new DiffResult();
        result.setDiffs(diffs);
        result.setMode("char");
        result.setStats(calculateStats(diffs));
        return result;
    }

    private DiffResult compareByWord(String text1, String text2) {
        String[] words1 = text1.split("(?<=\\s)|(?=\\s)");
        String[] words2 = text2.split("(?<=\\s)|(?=\\s)");
        
        List<DiffItem> diffs = diff(words1, words2);
        
        DiffResult result = new DiffResult();
        result.setDiffs(diffs);
        result.setMode("word");
        result.setStats(calculateStats(diffs));
        return result;
    }

    private List<DiffItem> diff(String[] arr1, String[] arr2) {
        List<DiffItem> result = new ArrayList<>();
        
        int m = arr1.length;
        int n = arr2.length;
        
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else if (arr1[i - 1].equals(arr2[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        
        int i = m, j = n;
        List<DiffItem> reverseResult = new ArrayList<>();
        
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && arr1[i - 1].equals(arr2[j - 1])) {
                reverseResult.add(new DiffItem("equal", arr1[i - 1], i - 1, j - 1));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] <= dp[i - 1][j])) {
                reverseResult.add(new DiffItem("added", arr2[j - 1], -1, j - 1));
                j--;
            } else if (i > 0) {
                reverseResult.add(new DiffItem("removed", arr1[i - 1], i - 1, -1));
                i--;
            }
        }
        
        for (int k = reverseResult.size() - 1; k >= 0; k--) {
            result.add(reverseResult.get(k));
        }
        
        return result;
    }

    private DiffStats calculateStats(List<DiffItem> diffs) {
        DiffStats stats = new DiffStats();
        
        int added = 0;
        int removed = 0;
        int unchanged = 0;
        
        for (DiffItem item : diffs) {
            switch (item.getType()) {
                case "added":
                    added++;
                    break;
                case "removed":
                    removed++;
                    break;
                default:
                    unchanged++;
            }
        }
        
        stats.setAddedCount(added);
        stats.setRemovedCount(removed);
        stats.setUnchangedCount(unchanged);
        stats.setTotalCount(diffs.size());
        
        return stats;
    }

    public String generateHtmlDiff(DiffResult diffResult) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"diff-container\">");
        
        int lineNumber1 = 1;
        int lineNumber2 = 1;
        
        for (DiffItem item : diffResult.getDiffs()) {
            String content = escapeHtml(item.getContent());
            
            switch (item.getType()) {
                case "added":
                    html.append("<div class=\"diff-line added\">");
                    html.append("<span class=\"line-num\">").append(lineNumber2++).append("</span>");
                    html.append("<span class=\"line-content\">+ ").append(content).append("</span>");
                    html.append("</div>");
                    break;
                case "removed":
                    html.append("<div class=\"diff-line removed\">");
                    html.append("<span class=\"line-num\">").append(lineNumber1++).append("</span>");
                    html.append("<span class=\"line-content\">- ").append(content).append("</span>");
                    html.append("</div>");
                    break;
                default:
                    html.append("<div class=\"diff-line unchanged\">");
                    html.append("<span class=\"line-num\">").append(lineNumber1++).append("</span>");
                    html.append("<span class=\"line-num\">").append(lineNumber2++).append("</span>");
                    html.append("<span class=\"line-content\">  ").append(content).append("</span>");
                    html.append("</div>");
            }
        }
        
        html.append("</div>");
        return html.toString();
    }

    public String generateUnifiedDiff(String text1, String text2, int contextLines) {
        String[] lines1 = text1.split("\n");
        String[] lines2 = text2.split("\n");
        
        List<DiffItem> diffs = diff(lines1, lines2);
        
        StringBuilder result = new StringBuilder();
        result.append("--- Original\n");
        result.append("+++ Modified\n");
        
        int i = 0;
        while (i < diffs.size()) {
            DiffItem item = diffs.get(i);
            
            if (!item.getType().equals("equal")) {
                int startLine = Math.max(0, i - contextLines);
                int endLine = Math.min(diffs.size(), i + contextLines + 1);
                
                while (startLine > 0 && diffs.get(startLine - 1).getType().equals("equal")) {
                    startLine--;
                }
                while (endLine < diffs.size() && diffs.get(endLine).getType().equals("equal")) {
                    endLine++;
                }
                
                result.append("@@ -").append(startLine + 1).append(" +").append(startLine + 1).append(" @@\n");
                
                for (int j = startLine; j < endLine; j++) {
                    DiffItem d = diffs.get(j);
                    switch (d.getType()) {
                        case "added":
                            result.append("+").append(d.getContent()).append("\n");
                            break;
                        case "removed":
                            result.append("-").append(d.getContent()).append("\n");
                            break;
                        default:
                            result.append(" ").append(d.getContent()).append("\n");
                    }
                }
                
                i = endLine;
            } else {
                i++;
            }
        }
        
        return result.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace(" ", "&nbsp;");
    }

    public static class DiffItem {
        private String type;
        private String content;
        private int oldIndex;
        private int newIndex;

        public DiffItem(String type, String content, int oldIndex, int newIndex) {
            this.type = type;
            this.content = content;
            this.oldIndex = oldIndex;
            this.newIndex = newIndex;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public int getOldIndex() { return oldIndex; }
        public void setOldIndex(int oldIndex) { this.oldIndex = oldIndex; }
        public int getNewIndex() { return newIndex; }
        public void setNewIndex(int newIndex) { this.newIndex = newIndex; }
    }

    public static class DiffResult {
        private List<DiffItem> diffs;
        private String mode;
        private DiffStats stats;

        public List<DiffItem> getDiffs() { return diffs; }
        public void setDiffs(List<DiffItem> diffs) { this.diffs = diffs; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public DiffStats getStats() { return stats; }
        public void setStats(DiffStats stats) { this.stats = stats; }
    }

    public static class DiffStats {
        private int addedCount;
        private int removedCount;
        private int unchangedCount;
        private int totalCount;

        public int getAddedCount() { return addedCount; }
        public void setAddedCount(int addedCount) { this.addedCount = addedCount; }
        public int getRemovedCount() { return removedCount; }
        public void setRemovedCount(int removedCount) { this.removedCount = removedCount; }
        public int getUnchangedCount() { return unchangedCount; }
        public void setUnchangedCount(int unchangedCount) { this.unchangedCount = unchangedCount; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    }
}
