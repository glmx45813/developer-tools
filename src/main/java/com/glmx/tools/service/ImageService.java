package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class ImageService {

    private static final Map<String, String> FORMAT_MIME_TYPES = new HashMap<>();
    
    static {
        FORMAT_MIME_TYPES.put("jpg", "image/jpeg");
        FORMAT_MIME_TYPES.put("jpeg", "image/jpeg");
        FORMAT_MIME_TYPES.put("png", "image/png");
        FORMAT_MIME_TYPES.put("gif", "image/gif");
        FORMAT_MIME_TYPES.put("bmp", "image/bmp");
        FORMAT_MIME_TYPES.put("webp", "image/webp");
    }

    public ImageResult compress(MultipartFile file, Float quality, Integer maxWidth, Integer maxHeight) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传图片文件");
        }
        
        if (quality == null) {
            quality = 0.8f;
        }
        if (quality < 0.1f || quality > 1.0f) {
            throw new BusinessException("压缩质量必须在0.1-1.0之间");
        }

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage originalImage = ImageIO.read(inputStream);
            if (originalImage == null) {
                throw new BusinessException("无法读取图片文件");
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            
            int newWidth = originalWidth;
            int newHeight = originalHeight;
            
            if (maxWidth != null && maxWidth > 0 && originalWidth > maxWidth) {
                double ratio = (double) maxWidth / originalWidth;
                newWidth = maxWidth;
                newHeight = (int) (originalHeight * ratio);
            }
            
            if (maxHeight != null && maxHeight > 0 && newHeight > maxHeight) {
                double ratio = (double) maxHeight / newHeight;
                newHeight = maxHeight;
                newWidth = (int) (newWidth * ratio);
            }

            BufferedImage resizedImage = originalImage;
            if (newWidth != originalWidth || newHeight != originalHeight) {
                resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = resizedImage.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
                g2d.dispose();
            }

            String originalFormat = getFormatName(file.getOriginalFilename());
            String outputFormat = "jpg".equalsIgnoreCase(originalFormat) || "jpeg".equalsIgnoreCase(originalFormat) ? "jpg" : "png";

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            if ("jpg".equals(outputFormat)) {
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
                if (writers.hasNext()) {
                    ImageWriter writer = writers.next();
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(quality);
                    
                    try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream)) {
                        writer.setOutput(ios);
                        writer.write(null, new IIOImage(resizedImage, null, null), param);
                    }
                    writer.dispose();
                }
            } else {
                ImageIO.write(resizedImage, outputFormat, outputStream);
            }

            byte[] compressedBytes = outputStream.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(compressedBytes);

            ImageResult result = new ImageResult();
            result.setBase64(base64);
            result.setFormat(outputFormat);
            result.setMimeType(FORMAT_MIME_TYPES.getOrDefault(outputFormat, "image/" + outputFormat));
            result.setOriginalSize(file.getSize());
            result.setResultSize((long) compressedBytes.length);
            result.setOriginalWidth(originalWidth);
            result.setOriginalHeight(originalHeight);
            result.setResultWidth(newWidth);
            result.setResultHeight(newHeight);
            result.setCompressionRatio(String.format("%.2f%%", 
                (1 - (double) compressedBytes.length / file.getSize()) * 100));
            
            return result;
        } catch (IOException e) {
            throw new BusinessException("图片压缩失败: " + e.getMessage());
        }
    }

    public ImageResult convertFormat(MultipartFile file, String targetFormat) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传图片文件");
        }
        if (targetFormat == null || targetFormat.isEmpty()) {
            throw new BusinessException("请指定目标格式");
        }

        targetFormat = targetFormat.toLowerCase();
        if (!FORMAT_MIME_TYPES.containsKey(targetFormat)) {
            throw new BusinessException("不支持的目标格式: " + targetFormat);
        }

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage originalImage = ImageIO.read(inputStream);
            if (originalImage == null) {
                throw new BusinessException("无法读取图片文件");
            }

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            BufferedImage convertedImage;
            if ("jpg".equals(targetFormat) || "jpeg".equals(targetFormat)) {
                convertedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = convertedImage.createGraphics();
                g2d.drawImage(originalImage, 0, 0, null);
                g2d.dispose();
            } else {
                convertedImage = originalImage;
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            boolean success = ImageIO.write(convertedImage, targetFormat, outputStream);
            
            if (!success) {
                throw new BusinessException("无法转换为指定格式");
            }

            byte[] convertedBytes = outputStream.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(convertedBytes);

            ImageResult result = new ImageResult();
            result.setBase64(base64);
            result.setFormat(targetFormat);
            result.setMimeType(FORMAT_MIME_TYPES.get(targetFormat));
            result.setOriginalSize(file.getSize());
            result.setResultSize((long) convertedBytes.length);
            result.setOriginalWidth(width);
            result.setOriginalHeight(height);
            result.setResultWidth(width);
            result.setResultHeight(height);
            
            return result;
        } catch (IOException e) {
            throw new BusinessException("格式转换失败: " + e.getMessage());
        }
    }

    public ImageResult resize(MultipartFile file, Integer width, Integer height, Boolean keepRatio) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传图片文件");
        }
        if (width == null && height == null) {
            throw new BusinessException("请指定宽度或高度");
        }

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage originalImage = ImageIO.read(inputStream);
            if (originalImage == null) {
                throw new BusinessException("无法读取图片文件");
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            int newWidth = width != null ? width : originalWidth;
            int newHeight = height != null ? height : originalHeight;

            if (keepRatio != null && keepRatio) {
                double widthRatio = (double) newWidth / originalWidth;
                double heightRatio = (double) newHeight / originalHeight;
                double ratio = Math.min(widthRatio, heightRatio);
                newWidth = (int) (originalWidth * ratio);
                newHeight = (int) (originalHeight * ratio);
            }

            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g2d.dispose();

            String format = getFormatName(file.getOriginalFilename());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, format, outputStream);

            byte[] resizedBytes = outputStream.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(resizedBytes);

            ImageResult result = new ImageResult();
            result.setBase64(base64);
            result.setFormat(format);
            result.setMimeType(FORMAT_MIME_TYPES.getOrDefault(format, "image/" + format));
            result.setOriginalSize(file.getSize());
            result.setResultSize((long) resizedBytes.length);
            result.setOriginalWidth(originalWidth);
            result.setOriginalHeight(originalHeight);
            result.setResultWidth(newWidth);
            result.setResultHeight(newHeight);
            
            return result;
        } catch (IOException e) {
            throw new BusinessException("图片缩放失败: " + e.getMessage());
        }
    }

    public ImageResult rotate(MultipartFile file, Integer angle) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传图片文件");
        }
        if (angle == null || (angle % 90 != 0)) {
            throw new BusinessException("旋转角度必须是90的倍数");
        }

        angle = angle % 360;
        if (angle < 0) {
            angle += 360;
        }

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage originalImage = ImageIO.read(inputStream);
            if (originalImage == null) {
                throw new BusinessException("无法读取图片文件");
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            int newWidth, newHeight;
            if (angle == 90 || angle == 270) {
                newWidth = originalHeight;
                newHeight = originalWidth;
            } else {
                newWidth = originalWidth;
                newHeight = originalHeight;
            }

            BufferedImage rotatedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = rotatedImage.createGraphics();
            
            g2d.rotate(Math.toRadians(angle), newWidth / 2.0, newHeight / 2.0);
            
            int x = (newWidth - originalWidth) / 2;
            int y = (newHeight - originalHeight) / 2;
            g2d.drawImage(originalImage, x, y, null);
            g2d.dispose();

            String format = getFormatName(file.getOriginalFilename());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(rotatedImage, format, outputStream);

            byte[] rotatedBytes = outputStream.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(rotatedBytes);

            ImageResult result = new ImageResult();
            result.setBase64(base64);
            result.setFormat(format);
            result.setMimeType(FORMAT_MIME_TYPES.getOrDefault(format, "image/" + format));
            result.setOriginalSize(file.getSize());
            result.setResultSize((long) rotatedBytes.length);
            result.setOriginalWidth(originalWidth);
            result.setOriginalHeight(originalHeight);
            result.setResultWidth(newWidth);
            result.setResultHeight(newHeight);
            
            return result;
        } catch (IOException e) {
            throw new BusinessException("图片旋转失败: " + e.getMessage());
        }
    }

    private String getFormatName(String filename) {
        if (filename == null) {
            return "png";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            String ext = filename.substring(dotIndex + 1).toLowerCase();
            if (FORMAT_MIME_TYPES.containsKey(ext)) {
                return ext;
            }
        }
        return "png";
    }

    public static class ImageResult {
        private String base64;
        private String format;
        private String mimeType;
        private Long originalSize;
        private Long resultSize;
        private Integer originalWidth;
        private Integer originalHeight;
        private Integer resultWidth;
        private Integer resultHeight;
        private String compressionRatio;

        public String getBase64() { return base64; }
        public void setBase64(String base64) { this.base64 = base64; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public Long getOriginalSize() { return originalSize; }
        public void setOriginalSize(Long originalSize) { this.originalSize = originalSize; }
        public Long getResultSize() { return resultSize; }
        public void setResultSize(Long resultSize) { this.resultSize = resultSize; }
        public Integer getOriginalWidth() { return originalWidth; }
        public void setOriginalWidth(Integer originalWidth) { this.originalWidth = originalWidth; }
        public Integer getOriginalHeight() { return originalHeight; }
        public void setOriginalHeight(Integer originalHeight) { this.originalHeight = originalHeight; }
        public Integer getResultWidth() { return resultWidth; }
        public void setResultWidth(Integer resultWidth) { this.resultWidth = resultWidth; }
        public Integer getResultHeight() { return resultHeight; }
        public void setResultHeight(Integer resultHeight) { this.resultHeight = resultHeight; }
        public String getCompressionRatio() { return compressionRatio; }
        public void setCompressionRatio(String compressionRatio) { this.compressionRatio = compressionRatio; }
    }
}
