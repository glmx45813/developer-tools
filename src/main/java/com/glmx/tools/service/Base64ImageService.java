package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import com.glmx.tools.common.ResultCode;
import com.glmx.tools.dto.Base64ImageResponse;
import com.glmx.tools.dto.ImageFromBase64Response;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class Base64ImageService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("bmp", "image/bmp");
        MIME_TYPES.put("webp", "image/webp");
    }

    public Base64ImageResponse imageToBase64(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException("请选择要转换的图片");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE);
        }

        String originalFilename = file.getOriginalFilename();
        String format = getImageFormat(originalFilename);
        if (format == null) {
            throw new BusinessException(ResultCode.INVALID_FILE_TYPE);
        }

        byte[] bytes = file.getBytes();
        String base64 = Base64.getEncoder().encodeToString(bytes);
        String mimeType = MIME_TYPES.getOrDefault(format.toLowerCase(), "image/" + format);

        Base64ImageResponse response = new Base64ImageResponse();
        response.setBase64(base64);
        response.setDataUrl("data:" + mimeType + ";base64," + base64);
        response.setFormat(format.toLowerCase());
        response.setSize(file.getSize());
        response.setFilename(originalFilename);

        return response;
    }

    public ImageFromBase64Response base64ToImage(String base64Data) throws IOException {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            throw new BusinessException("Base64数据不能为空");
        }

        String pureBase64 = base64Data;
        String format = "png";

        if (base64Data.contains(",")) {
            String[] parts = base64Data.split(",");
            String header = parts[0];
            pureBase64 = parts[1];

            if (header.contains("image/")) {
                int start = header.indexOf("image/") + 6;
                int end = header.indexOf(";", start);
                if (end > start) {
                    format = header.substring(start, end);
                }
            }
        }

        pureBase64 = pureBase64.replaceAll("\\s+", "");

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(pureBase64);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的Base64编码");
        }

        if (imageBytes.length > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE);
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
        BufferedImage image = ImageIO.read(bis);
        if (image == null) {
            throw new BusinessException("无法解析图片数据");
        }

        ImageFromBase64Response response = new ImageFromBase64Response();
        response.setImageData(base64Data);
        response.setFormat(format);
        response.setWidth(image.getWidth());
        response.setHeight(image.getHeight());

        return response;
    }

    public byte[] base64ToImageBytes(String base64Data) throws IOException {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            throw new BusinessException("Base64数据不能为空");
        }

        String pureBase64 = base64Data;
        if (base64Data.contains(",")) {
            pureBase64 = base64Data.split(",")[1];
        }

        pureBase64 = pureBase64.replaceAll("\\s+", "");

        try {
            return Base64.getDecoder().decode(pureBase64);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的Base64编码");
        }
    }

    public String detectImageFormat(String base64Data) {
        if (base64Data.contains(",")) {
            String header = base64Data.split(",")[0];
            if (header.contains("image/")) {
                int start = header.indexOf("image/") + 6;
                int end = header.indexOf(";", start);
                if (end > start) {
                    return header.substring(start, end);
                }
            }
        }
        return "png";
    }

    private String getImageFormat(String filename) {
        if (filename == null) {
            return null;
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            String ext = filename.substring(dotIndex + 1).toLowerCase();
            if (MIME_TYPES.containsKey(ext)) {
                return ext;
            }
        }
        return null;
    }
}
