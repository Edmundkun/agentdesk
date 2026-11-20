package com.agentdesk.knowledge;

import com.agentdesk.common.BizException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 文档解析：txt / md 直接按 UTF-8 读取，pdf 使用 PDFBox 抽取文本。
 */
@Component
public class DocumentParser {

    private static final int MAX_TEXT_LENGTH = 200_000;

    public String parse(String extension, byte[] bytes) {
        String text = switch (extension.toLowerCase()) {
            case "txt", "md" -> new String(bytes, StandardCharsets.UTF_8);
            case "pdf" -> parsePdf(bytes);
            default -> throw BizException.badRequest("仅支持 txt / md / pdf 格式");
        };
        return text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text;
    }

    private String parsePdf(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (IOException e) {
            throw BizException.badRequest("PDF 解析失败：" + e.getMessage());
        }
    }
}
