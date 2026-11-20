package com.agentdesk.embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 轻量分词器：英文按单词、中文按“单字 + 二元组”切分。
 *
 * <p>不引入第三方分词库，保证项目零额外依赖即可运行；对中文短查询（如“报销流程”）
 * 单字与二元组混合可以兼顾召回率与准确性。</p>
 */
public final class TextTokenizer {

    private static final Pattern WORD = Pattern.compile("[a-zA-Z0-9_]+");
    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fa5]");
    private static final int MAX_TOKENS = 4000;

    private TextTokenizer() {
    }

    public static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();

        Matcher wordMatcher = WORD.matcher(lower);
        while (wordMatcher.find()) {
            String word = wordMatcher.group();
            if (word.length() > 1 || !word.isBlank()) {
                tokens.add(word);
            }
        }

        List<Character> cjkChars = new ArrayList<>();
        Matcher cjkMatcher = CJK.matcher(lower);
        while (cjkMatcher.find()) {
            cjkChars.add(cjkMatcher.group().charAt(0));
        }
        for (int i = 0; i < cjkChars.size(); i++) {
            tokens.add(String.valueOf(cjkChars.get(i)));
            if (i + 1 < cjkChars.size()) {
                tokens.add("" + cjkChars.get(i) + cjkChars.get(i + 1));
            }
        }

        return tokens.size() > MAX_TOKENS ? tokens.subList(0, MAX_TOKENS) : tokens;
    }

    /**
     * 去重后的关键词集合，用于计算关键词命中率。
     */
    public static List<String> distinctTokens(String text) {
        return tokenize(text).stream().distinct().toList();
    }
}
