package cn.airframework.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link CharSequence}或{@link String}工具类
 *
 * @author huangchengxing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtils {

    /**
     * 使用指定的映射器（mapper）和分隔符（separator）连接数组（array）。
     *
     * @param mapper 映射器
     * @param separator 分隔符
     * @param array 数组
     * @return 连接后的字符串
     */
    @SafeVarargs
    public static <T> String join(Function<T, CharSequence> mapper, String separator, T... array) {
        if (ArrayUtils.isEmpty(array)) {
            return "";
        }
        return join(Arrays.asList(array), mapper, separator);
    }

    /**
     * 使用指定的映射器（mapper）和分隔符（separator）连接集合（coll）。
     *
     * @param coll 集合
     * @param mapper 映射器
     * @param separator 分隔符
     * @return 连接后的字符串
     */
    public static <T> String join(Collection<T> coll, Function<T, CharSequence> mapper, String separator) {
        if (CollectionUtils.isEmpty(coll)) {
            return "";
        }
        return coll.stream()
            .map(mapper)
            .collect(Collectors.joining(separator));
    }

    /**
     * <p>判断 {@code searchStr} 是否在 {@code str} 中。<br />
     * 例如：
     * <ul>
     *     <li>{@code "abc", "abc"} 将返回 {@code true}</li>
     *     <li>{@code "abc", "b"} 将返回 {@code true}</li>
     *     <li>{@code "abc", "d"} 将返回 {@code false}</li>
     *     <li>{@code null, "a"} 将返回 {@code false}</li>
     *     <li>{@code "a", null} 将返回 {@code false}</li>
     *     <li>{@code null, null} 将返回 {@code false}</li>
     * </ul>
     *
     * @param str 要检查的字符序列，可能为null
     * @param searchStr 要查找的字符序列，可能为null
     * @return 如果查找的字符序列在字符序列中，则返回true
     */
    public static boolean contains(CharSequence str, CharSequence searchStr) {
        // 如果全部为null，则返回false
        if (Objects.isNull(str) && Objects.isNull(searchStr)) {
            return false;
        }
        if (Objects.equals(str, searchStr)) {
            return true;
        }
        if (Objects.isNull(str) || Objects.isNull(searchStr)) {
            return false;
        }
        return str.toString().contains(searchStr);
    }

    /**
     * <p>使用占位符 {} 格式化字符串。<br />
     * 例如：{@code "a{}c", "b"} 将返回 {@code "abc"}
     *
     * @param template 模板
     * @param args 参数
     * @return 格式化后的字符串
     */
    public static String format(String template, Object... args) {
        if (isEmpty(template) || ArrayUtils.isEmpty(args)) {
            return template;
        }
        StringBuilder sb = new StringBuilder();
        int cursor = 0;
        int index = 0;
        while (cursor < template.length()) {
            int placeholderIndex = template.indexOf("{}", cursor);
            if (placeholderIndex == -1) {
                sb.append(template.substring(cursor));
                break;
            }
            sb.append(template, cursor, placeholderIndex);
            if (index < args.length) {
                sb.append(args[index++]);
            } else {
                sb.append("{}");
            }
            cursor = placeholderIndex + 2;
        }
        return sb.toString();
    }

    /**
     * <p>判断给定的 {@link CharSequence} 是否为空。<br />
     * 例如：{@code null, ""} 将返回true，{@code "a", " "} 将返回false。
     *
     * @param cs 字符序列
     * @return 是否为空
     */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * <p>判断给定的 {@link CharSequence} 是否不为空。<br />
     * 例如：{@code null, ""} 将返回false，{@code "a", " "} 将返回true。
     *
     * @param cs 字符序列
     * @return 是否不为空
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }

    /**
     * <p>如果 {@code str} 不为空，则返回 {@code str}，否则返回 {@code defaultStr}。
     *
     * @param str 字符串
     * @param defaultStr 默认字符串
     * @return 字符串或默认字符串
     */
    public static String emptyToDefault(String str, String defaultStr) {
        return isEmpty(str) ? defaultStr : str;
    }

    /**
     * <p>如果 {@code str} 不为空，则返回 {@code str}，否则返回 {@code null}。
     *
     * @param str 字符串
     * @return 字符串或null
     */
    public static String emptyToNull(String str) {
        return isEmpty(str) ? null : str;
    }

    /**
     * <p>将第一个字符大写并添加前缀。<br />
     * 例如：{@code "bc", "a"} 将返回 {@code "aBc"}，{@code null, "a"} 将返回 {@code "anull"}。
     *
     * @param str 字符串
     * @param prefix 前缀
     * @return 第一个字符大写并带有前缀的字符串
     */
    public static String upperFirstAndAddPrefix(String str, String prefix) {
        return prefix + upperFirst(str);
    }

    /**
     * <p>将第一个字符大写。<br />
     * 例如：{@code "abc"} 将返回 {@code "Abc"}。
     *
     * @param str 字符串
     * @return 第一个字符大写的字符串
     */
    public static String upperFirst(String str) {
        if (isEmpty(str)) {
            return str;
        }
        char[] charArray = str.toCharArray();
        charArray[0] = Character.toUpperCase(charArray[0]);
        return new String(charArray);
    }

    /**
     * <p>判断给定的 {@link CharSequence} 是否为空格。<br />
     * 例如：{@code null, "", " "} 将返回true，{@code "a"} 将返回false。
     *
     * @param cs 字符序列
     * @return 是否为空格
     */
    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; ++i) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>判断给定的 {@link CharSequence} 是否不为空格。<br />
     * 例如：{@code null, "", " "} 将返回false，{@code "a"} 将返回true。
     *
     * @param cs 字符序列
     * @return 是否不为空格
     */
    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * 使用MD5算法将字符串转换为十六进制。
     *
     * @param str 字符串
     * @return MD5十六进制
     */
    @SneakyThrows
    public static String md5DigestAsHex(@NonNull String str) {
        MessageDigest md5 = MessageDigest.getInstance("md5");
        byte[] digest = md5.digest(str.getBytes(StandardCharsets.UTF_8));
        return new BigInteger(1, digest).toString(16);
    }
}
