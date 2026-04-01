package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import com.glmx.tools.common.ResultCode;
import com.glmx.tools.dto.QrCodeGenerateRequest;
import com.glmx.tools.dto.QrCodeParseResponse;
import com.glmx.tools.dto.QrCodeResponse;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

@Service
public class QrCodeService {

    private static final Map<DecodeHintType, Object> DECODE_HINTS = new EnumMap<>(DecodeHintType.class);
    
    static {
        DECODE_HINTS.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        DECODE_HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        DECODE_HINTS.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
    }

    public QrCodeResponse generateQrCode(QrCodeGenerateRequest request) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, getErrorCorrectionLevel(request.getErrorCorrectionLevel()));
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    request.getContent(),
                    BarcodeFormat.QR_CODE,
                    request.getSize(),
                    request.getSize(),
                    hints
            );

            BufferedImage image = toBufferedImage(bitMatrix, request);
            String base64 = imageToBase64(image);

            QrCodeResponse response = new QrCodeResponse();
            response.setBase64Image(base64);
            response.setDataUrl("data:image/png;base64," + base64);
            response.setContent(request.getContent());
            response.setSize(request.getSize());

            return response;
        } catch (WriterException | IOException e) {
            throw new BusinessException(ResultCode.QRCODE_GENERATE_ERROR.getCode(), "二维码生成失败: " + e.getMessage());
        }
    }

    public QrCodeParseResponse parseQrCode(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException("请选择要识别的二维码图片");
        }

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new BusinessException("无法读取图片文件");
            }

            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            try {
                QRCodeReader reader = new QRCodeReader();
                Result result = reader.decode(bitmap, DECODE_HINTS);

                QrCodeParseResponse response = new QrCodeParseResponse();
                response.setContents(Collections.singletonList(result.getText()));
                response.setCount(1);
                response.setFormat(getImageFormat(file.getOriginalFilename()));

                return response;
            } catch (NotFoundException e) {
                throw new BusinessException("未找到二维码");
            } catch (ChecksumException e) {
                throw new BusinessException("二维码校验失败");
            } catch (FormatException e) {
                throw new BusinessException("二维码格式错误");
            }
        }
    }

    public QrCodeParseResponse parseQrCodeFromBase64(String base64Data) throws IOException {
        String pureBase64 = base64Data;
        if (base64Data.contains(",")) {
            pureBase64 = base64Data.split(",")[1];
        }

        byte[] imageBytes = Base64.getDecoder().decode(pureBase64);
        ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
        BufferedImage image = ImageIO.read(bis);

        if (image == null) {
            throw new BusinessException("无法解析图片数据");
        }

        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        try {
            QRCodeReader reader = new QRCodeReader();
            Result result = reader.decode(bitmap, DECODE_HINTS);

            QrCodeParseResponse response = new QrCodeParseResponse();
            response.setContents(Collections.singletonList(result.getText()));
            response.setCount(1);

            return response;
        } catch (NotFoundException e) {
            throw new BusinessException("未找到二维码");
        } catch (ChecksumException e) {
            throw new BusinessException("二维码校验失败");
        } catch (FormatException e) {
            throw new BusinessException("二维码格式错误");
        }
    }

    public byte[] generateQrCodeBytes(QrCodeGenerateRequest request) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, getErrorCorrectionLevel(request.getErrorCorrectionLevel()));
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    request.getContent(),
                    BarcodeFormat.QR_CODE,
                    request.getSize(),
                    request.getSize(),
                    hints
            );

            BufferedImage image = toBufferedImage(bitMatrix, request);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (WriterException | IOException e) {
            throw new BusinessException(ResultCode.QRCODE_GENERATE_ERROR.getCode(), "二维码生成失败: " + e.getMessage());
        }
    }

    private BufferedImage toBufferedImage(BitMatrix matrix, QrCodeGenerateRequest request) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Color foreground = parseColor(request.getForegroundColor());
        Color background = parseColor(request.getBackgroundColor());

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? foreground.getRGB() : background.getRGB());
            }
        }

        return image;
    }

    private String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private ErrorCorrectionLevel getErrorCorrectionLevel(String level) {
        switch (level.toUpperCase()) {
            case "L":
                return ErrorCorrectionLevel.L;
            case "Q":
                return ErrorCorrectionLevel.Q;
            case "H":
                return ErrorCorrectionLevel.H;
            case "M":
            default:
                return ErrorCorrectionLevel.M;
        }
    }

    private Color parseColor(String hex) {
        try {
            return new Color(Integer.parseInt(hex, 16));
        } catch (NumberFormatException e) {
            return Color.BLACK;
        }
    }

    private String getImageFormat(String filename) {
        if (filename == null) return "png";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return "png";
    }
}
