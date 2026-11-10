package com.nebula.api.search;

import java.text.Normalizer;
import java.util.Locale;

/** 候选索引用的宽松文字折叠；最终仍用MySQL原排序规则和当前公开状态核验命中。 */
public final class SearchText {
    private SearchText() {}

    /** 拒绝NUL和不成对的UTF-16代理项，避免进入ES通配符或SQL编码器后被替换为其他字符。 */
    public static boolean validKeyword(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == 0) return false;
            if (Character.isHighSurrogate(c)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) return false;
            } else if (Character.isLowSurrogate(c)) return false;
        }
        return true;
    }

    /** 折叠大小写、重音和全角字符，移除NUL及不可见格式符以放宽候选；原格式差异由MySQL精确核验。 */
    public static String fold(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT);
        var result = new StringBuilder();
        decomposed.codePoints().filter(cp -> cp != 0).filter(cp -> {
            int type = Character.getType(cp);
            return type != Character.NON_SPACING_MARK && type != Character.COMBINING_SPACING_MARK
                    && type != Character.ENCLOSING_MARK && type != Character.FORMAT;
        }).forEach(cp -> result.appendCodePoint(cp == 0x03c2 ? 0x03c3 : cp));
        return result.toString();
    }

    /** 把通配符查询中的操作符作为普通文字转义，禁止把用户输入当查询语法。 */
    public static String literalPattern(String value) {
        return "*" + fold(value).replace("\\", "\\\\").replace("*", "\\*").replace("?", "\\?") + "*";
    }
}
