package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SqlService {

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "IN", "LIKE", "BETWEEN",
        "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "ON", "AS", "GROUP", "BY",
        "ORDER", "HAVING", "LIMIT", "OFFSET", "UNION", "ALL", "DISTINCT",
        "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE",
        "TABLE", "DROP", "ALTER", "ADD", "COLUMN", "INDEX", "PRIMARY", "KEY",
        "FOREIGN", "REFERENCES", "CONSTRAINT", "DEFAULT", "NULL", "NOT",
        "TRUE", "FALSE", "CASE", "WHEN", "THEN", "ELSE", "END", "IF", "EXISTS",
        "ASC", "DESC", "NULLS", "FIRST", "LAST", "WITH", "RECURSIVE", "TEMPORARY",
        "TEMP", "VIEW", "FUNCTION", "PROCEDURE", "TRIGGER", "GRANT", "REVOKE",
        "COMMIT", "ROLLBACK", "BEGIN", "TRANSACTION", "SAVEPOINT", "DECLARE",
        "CURSOR", "FETCH", "OPEN", "CLOSE", "EXEC", "EXECUTE", "PREPARE",
        "DEALLOCATE", "DESCRIBE", "EXPLAIN", "USE", "DATABASE", "SCHEMA",
        "CAST", "CONVERT", "COALESCE", "NULLIF", "ISNULL", "IFNULL", "NVL",
        "FULL", "CROSS", "NATURAL", "USING", "INTERSECT", "EXCEPT", "MINUS",
        "ANY", "SOME", "EXISTS", "UNIQUE", "CHECK", "CASCADE", "RESTRICT",
        "NO", "ACTION", "READ", "WRITE", "ONLY", "NOWAIT", "SKIP", "LOCKED"
    ));

    private static final Set<String> MAJOR_KEYWORDS = new HashSet<>(Arrays.asList(
        "SELECT", "FROM", "WHERE", "JOIN", "LEFT JOIN", "RIGHT JOIN", "INNER JOIN",
        "FULL JOIN", "CROSS JOIN", "ON", "GROUP BY", "ORDER BY", "HAVING",
        "LIMIT", "OFFSET", "UNION", "UNION ALL", "INSERT INTO", "VALUES",
        "UPDATE", "SET", "DELETE FROM", "CREATE TABLE", "DROP TABLE", "ALTER TABLE"
    ));

    private static final Pattern STRING_PATTERN = Pattern.compile("'[^']*'");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("--[^\n]*|/\\*.*?\\*/", Pattern.DOTALL);

    public SqlResult format(String sql, String dialect, int indentSize) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new BusinessException("SQL内容不能为空");
        }

        try {
            String formatted = formatSql(sql, indentSize);
            
            SqlResult result = new SqlResult();
            result.setResult(formatted);
            result.setValid(true);
            result.setOriginalSize(sql.length());
            result.setResultSize(formatted.length());
            return result;
        } catch (Exception e) {
            throw new BusinessException("格式化失败: " + e.getMessage());
        }
    }

    private String repeat(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private String formatSql(String sql, int indentSize) {
        String indent = repeat(" ", indentSize);
        
        String processed = sql.trim().replaceAll("\\s+", " ");
        
        processed = processed.replaceAll("(?i)\\b(SELECT|FROM|WHERE|JOIN|LEFT\\s+JOIN|RIGHT\\s+JOIN|INNER\\s+JOIN|FULL\\s+JOIN|CROSS\\s+JOIN|ON|GROUP\\s+BY|ORDER\\s+BY|HAVING|LIMIT|OFFSET|UNION\\s+ALL|UNION|INSERT\\s+INTO|VALUES|UPDATE|SET|DELETE\\s+FROM|CREATE\\s+TABLE|DROP\\s+TABLE|ALTER\\s+TABLE)\\b", "\n$1");
        
        processed = processed.replaceAll("(?i)\\b(AND|OR)\\b", "\n" + indent + "$1");
        
        processed = processed.replaceAll("(?i)\\b(WHEN|THEN|ELSE)\\b", "\n" + indent + "$1");
        
        processed = processed.replaceAll("(?i)\\b(CASE)\\b", "\n$1");
        
        processed = processed.replaceAll("(?i)\\b(END)\\b", "$1\n");
        
        String[] lines = processed.split("\n");
        StringBuilder result = new StringBuilder();
        int currentIndent = 0;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String upperLine = line.toUpperCase();
            
            if (upperLine.startsWith("FROM") || upperLine.startsWith("WHERE") || 
                upperLine.startsWith("GROUP BY") || upperLine.startsWith("ORDER BY") ||
                upperLine.startsWith("HAVING") || upperLine.startsWith("LIMIT") ||
                upperLine.startsWith("OFFSET") || upperLine.startsWith("UNION") ||
                upperLine.startsWith("INSERT") || upperLine.startsWith("VALUES") ||
                upperLine.startsWith("UPDATE") || upperLine.startsWith("SET") ||
                upperLine.startsWith("DELETE") || upperLine.startsWith("CREATE") ||
                upperLine.startsWith("DROP") || upperLine.startsWith("ALTER")) {
                currentIndent = 0;
            }
            
            if (upperLine.startsWith("JOIN") || upperLine.startsWith("LEFT JOIN") ||
                upperLine.startsWith("RIGHT JOIN") || upperLine.startsWith("INNER JOIN") ||
                upperLine.startsWith("FULL JOIN") || upperLine.startsWith("CROSS JOIN")) {
                currentIndent = indentSize;
            }
            
            if (upperLine.startsWith("ON")) {
                currentIndent = indentSize * 2;
            }
            
            if (upperLine.startsWith("AND") || upperLine.startsWith("OR") ||
                upperLine.startsWith("WHEN") || upperLine.startsWith("THEN") ||
                upperLine.startsWith("ELSE")) {
                result.append(repeat(indent, currentIndent / indentSize));
            } else {
                result.append(repeat(indent, currentIndent / indentSize));
            }
            
            result.append(line).append("\n");
            
            if (upperLine.startsWith("SELECT")) {
                currentIndent = indentSize;
            }
        }
        
        return result.toString().trim();
    }

    public SqlResult compress(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new BusinessException("SQL内容不能为空");
        }

        try {
            String compressed = sql.trim()
                    .replaceAll("/\\*.*?\\*/", " ")
                    .replaceAll("--[^\n]*", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            
            SqlResult result = new SqlResult();
            result.setResult(compressed);
            result.setValid(true);
            result.setOriginalSize(sql.length());
            result.setResultSize(compressed.length());
            result.setCompressionRatio(String.format("%.2f%%", 
                (1 - (double) compressed.length() / sql.length()) * 100));
            return result;
        } catch (Exception e) {
            throw new BusinessException("压缩失败: " + e.getMessage());
        }
    }

    public SqlResult validate(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new BusinessException("SQL内容不能为空");
        }

        SqlResult result = new SqlResult();
        
        String upperSql = sql.toUpperCase().trim();
        
        if (!upperSql.startsWith("SELECT") && !upperSql.startsWith("INSERT") &&
            !upperSql.startsWith("UPDATE") && !upperSql.startsWith("DELETE") &&
            !upperSql.startsWith("CREATE") && !upperSql.startsWith("DROP") &&
            !upperSql.startsWith("ALTER") && !upperSql.startsWith("WITH") &&
            !upperSql.startsWith("BEGIN") && !upperSql.startsWith("COMMIT") &&
            !upperSql.startsWith("ROLLBACK") && !upperSql.startsWith("GRANT") &&
            !upperSql.startsWith("REVOKE") && !upperSql.startsWith("EXPLAIN") &&
            !upperSql.startsWith("DESCRIBE") && !upperSql.startsWith("SHOW") &&
            !upperSql.startsWith("USE") && !upperSql.startsWith("TRUNCATE")) {
            result.setValid(false);
            result.setError("SQL语句必须以有效的关键字开头");
            return result;
        }
        
        int openParen = countChar(sql, '(');
        int closeParen = countChar(sql, ')');
        if (openParen != closeParen) {
            result.setValid(false);
            result.setError("括号不匹配: ( 有 " + openParen + " 个, ) 有 " + closeParen + " 个");
            return result;
        }
        
        int openBracket = countChar(sql, '[');
        int closeBracket = countChar(sql, ']');
        if (openBracket != closeBracket) {
            result.setValid(false);
            result.setError("方括号不匹配");
            return result;
        }
        
        result.setValid(true);
        result.setMessage("SQL语法基本验证通过");
        return result;
    }

    private int countChar(String str, char c) {
        int count = 0;
        for (char ch : str.toCharArray()) {
            if (ch == c) count++;
        }
        return count;
    }

    public SqlResult highlight(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new BusinessException("SQL内容不能为空");
        }

        String highlighted = escapeHtml(sql);
        
        for (String keyword : KEYWORDS) {
            highlighted = highlighted.replaceAll("(?i)\\b(" + keyword + ")\\b", 
                "<span class=\"sql-keyword\">$1</span>");
        }
        
        highlighted = highlighted.replaceAll("'([^']*)'", 
            "<span class=\"sql-string\">'$1'</span>");
        
        highlighted = highlighted.replaceAll("\\b(\\d+)\\b", 
            "<span class=\"sql-number\">$1</span>");
        
        highlighted = highlighted.replaceAll("(--[^\n]*)", 
            "<span class=\"sql-comment\">$1</span>");
        
        highlighted = highlighted.replaceAll("(/\\*.*?\\*/)", 
            "<span class=\"sql-comment\">$1</span>");

        SqlResult result = new SqlResult();
        result.setResult(highlighted);
        result.setValid(true);
        return result;
    }

    private String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public static class SqlResult {
        private String result;
        private Boolean valid;
        private String message;
        private String error;
        private Integer originalSize;
        private Integer resultSize;
        private String compressionRatio;

        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public Boolean getValid() { return valid; }
        public void setValid(Boolean valid) { this.valid = valid; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public Integer getOriginalSize() { return originalSize; }
        public void setOriginalSize(Integer originalSize) { this.originalSize = originalSize; }
        public Integer getResultSize() { return resultSize; }
        public void setResultSize(Integer resultSize) { this.resultSize = resultSize; }
        public String getCompressionRatio() { return compressionRatio; }
        public void setCompressionRatio(String compressionRatio) { this.compressionRatio = compressionRatio; }
    }
}
