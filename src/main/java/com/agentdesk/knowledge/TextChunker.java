package com.agentdesk.knowledge;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 文本切片：优先按句子边界切分，相邻切片保留一定重叠（overlap），避免语义被截断。
 */
public final class TextChunker {

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[。！？!?；;])");

    private TextChunker() {
    }

    public static List<String> split(String raw, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        String text = normalize(raw);
        if (text.isBlank()) {
            return chunks;
        }
        int size = Math.max(120, chunkSize);
        int overlapSize = Math.max(0, Math.min(overlap, size / 2));

        List<String> units = new ArrayList<>();
        for (String sentence : SENTENCE_BOUNDARY.split(text)) {
            if (sentence.isBlank()) {
                continue;
            }
            if (sentence.length() <= size) {
                units.add(sentence);
            } else {
                for (int i = 0; i < sentence.length(); i += size) {
                    units.add(sentence.substring(i, Math.min(sentence.length(), i + size)));
                }
            }
        }

        StringBuilder buffer = new StringBuilder();
        for (String unit : units) {
            if (buffer.length() > 0 && buffer.length() + unit.length() > size) {
                chunks.add(buffer.toString().trim());
                String tail = buffer.substring(Math.max(0, buffer.length() - overlapSize));
                // 重叠部分 + 下一句不能超过上限，否则截短重叠区
                int maxTail = Math.max(0, size - unit.length());
                if (tail.length() > maxTail) {
                    tail = tail.substring(tail.length() - maxTail);
                }
                buffer.setLength(0);
                buffer.append(tail);
            }
            buffer.append(unit);
        }
        if (!buffer.toString().isBlank()) {
            chunks.add(buffer.toString().trim());
        }
        return chunks.stream().filter(chunk -> !chunk.isBlank()).toList();
    }

    static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\u0000", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
