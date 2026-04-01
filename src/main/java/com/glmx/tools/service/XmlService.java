package com.glmx.tools.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.glmx.tools.common.BusinessException;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;

@Service
public class XmlService {

    private final XmlMapper xmlMapper = new XmlMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public XmlResult format(String xml, int indent) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new BusinessException("XML内容不能为空");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
            transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
            
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indent));

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));

            XmlResult result = new XmlResult();
            result.setResult(writer.toString());
            result.setValid(true);
            result.setOriginalSize(xml.length());
            result.setResultSize(writer.toString().length());
            return result;
        } catch (SAXException e) {
            XmlResult result = new XmlResult();
            result.setValid(false);
            result.setError("XML格式错误: " + e.getMessage());
            return result;
        } catch (Exception e) {
            throw new BusinessException("格式化失败: " + e.getMessage());
        }
    }

    public XmlResult compress(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new BusinessException("XML内容不能为空");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));

            String compressed = removeWhitespace(document);

            XmlResult result = new XmlResult();
            result.setResult(compressed);
            result.setValid(true);
            result.setOriginalSize(xml.length());
            result.setResultSize(compressed.length());
            result.setCompressionRatio(String.format("%.2f%%", 
                (1 - (double) compressed.length() / xml.length()) * 100));
            return result;
        } catch (SAXException e) {
            XmlResult result = new XmlResult();
            result.setValid(false);
            result.setError("XML格式错误: " + e.getMessage());
            return result;
        } catch (Exception e) {
            throw new BusinessException("压缩失败: " + e.getMessage());
        }
    }

    private String removeWhitespace(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));

        return writer.toString().replaceAll(">\\s+<", "><").trim();
    }

    public XmlResult validate(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new BusinessException("XML内容不能为空");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new InputSource(new StringReader(xml)));

            XmlResult result = new XmlResult();
            result.setValid(true);
            result.setMessage("XML格式有效");
            return result;
        } catch (SAXException e) {
            XmlResult result = new XmlResult();
            result.setValid(false);
            result.setError("XML格式错误: " + e.getMessage());
            return result;
        } catch (Exception e) {
            XmlResult result = new XmlResult();
            result.setValid(false);
            result.setError("验证失败: " + e.getMessage());
            return result;
        }
    }

    public XmlResult xpath(String xml, String xpathExpr) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new BusinessException("XML内容不能为空");
        }
        if (xpathExpr == null || xpathExpr.trim().isEmpty()) {
            throw new BusinessException("XPath表达式不能为空");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            NodeList nodeList = (NodeList) xpath.evaluate(xpathExpr, document, XPathConstants.NODESET);

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                result.append(nodeToString(node)).append("\n");
            }

            XmlResult xmlResult = new XmlResult();
            xmlResult.setResult(result.toString().trim());
            xmlResult.setMatchCount(nodeList.getLength());
            return xmlResult;
        } catch (Exception e) {
            throw new BusinessException("XPath查询失败: " + e.getMessage());
        }
    }

    private String nodeToString(Node node) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        return writer.toString().trim();
    }

    public XmlResult toJson(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new BusinessException("XML内容不能为空");
        }

        try {
            JsonNode jsonNode = xmlMapper.readTree(xml.getBytes());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);

            XmlResult result = new XmlResult();
            result.setResult(json);
            result.setValid(true);
            return result;
        } catch (Exception e) {
            throw new BusinessException("转换失败: " + e.getMessage());
        }
    }

    public XmlResult escape(String xml) {
        if (xml == null) {
            throw new BusinessException("XML内容不能为空");
        }

        String escaped = xml
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");

        XmlResult result = new XmlResult();
        result.setResult(escaped);
        return result;
    }

    public XmlResult unescape(String xml) {
        if (xml == null) {
            throw new BusinessException("XML内容不能为空");
        }

        String unescaped = xml
                .replace("&apos;", "'")
                .replace("&quot;", "\"")
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .replace("&amp;", "&");

        XmlResult result = new XmlResult();
        result.setResult(unescaped);
        return result;
    }

    public static class XmlResult {
        private String result;
        private Boolean valid;
        private String message;
        private String error;
        private Integer originalSize;
        private Integer resultSize;
        private String compressionRatio;
        private Integer matchCount;

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
        public Integer getMatchCount() { return matchCount; }
        public void setMatchCount(Integer matchCount) { this.matchCount = matchCount; }
    }
}
