package cn.airframework.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link Object}工具类
 *
 * @author huangchengxing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ObjectUtils {

    /**
     * <p>获取目标元素的类型：
     * <ul>
     *     <li>{@code null};</li>
     *     <li>单个对象：{@link Object#getClass()};</li>
     *     <li>数组：{@link Class#getComponentType()};</li>
     *     <li>集合或迭代器：首个非{@code null}元素的类型;</li>
     * </ul>
     *
     * @param target target
     * @return element type
     */
    @Nullable
    public static Class<?> getElementType(Object target) {
        return switch (target) {
            case null -> null;
            case Object[] array -> array.getClass().getComponentType();
            case Iterable<?> iter -> Optional.ofNullable(CollectionUtils.getFirstNotNull(iter)).map(Object::getClass).orElse(null);
            case Iterator<?> iter -> Optional.ofNullable(CollectionUtils.getFirstNotNull(iter)).map(Object::getClass).orElse(null);
            default -> target.getClass();
        };
    }

    /**
     * <p>若{@code target}则返回默认值
     *
     * @param target 目标值
     * @param defaultValue 默认值
     * @param <T> 元素类型
     * @return 若{@code target}则返回默认值，否则返回{@code target}
     */
    public static <T> T defaultIfNull(T target, T defaultValue) {
        return Objects.isNull(target) ? defaultValue : target;
    }

    /**
     * <p>获取指定的索引元素对象.
     *
     * @param target target
     * @param <T> element type
     * @return element
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T get(Object target, int index) {
        return switch (target) {
            case null -> null;
            case List<?> list -> (T) CollectionUtils.get(list, index);
            case Iterable<?> iter -> (T) CollectionUtils.get(iter, index);
            case Iterator<?> iter -> (T) CollectionUtils.get(iter, index);
            case Object[] array -> (T) ArrayUtils.get(array, index);
            case Map<?, ?> map -> get(map.values(), index);
            default -> null;
        };
    }

    /**
     * <p>判断元素是否为空
     *
     * @param target target
     * @return boolean
     */
    public static boolean isEmpty(Object target) {
        return switch (target) {
            case null -> true;
            case Map<?, ?> map -> map.isEmpty();
            case Iterable<?> iter -> CollectionUtils.isEmpty(iter);
            case Iterator<?> iter -> CollectionUtils.isEmpty(iter);
            case Object[] array -> ArrayUtils.isEmpty(array);
            case CharSequence cs -> StringUtils.isEmpty(cs);
            default -> false;
        };
    }

    /**
     * 判断元素是否非空
     *
     * @param target target
     * @return boolean
     */
    public static boolean isNotEmpty(Object target) {
        return !isEmpty(target);
    }
}
