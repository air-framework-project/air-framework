package cn.airframework.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Array;
import java.util.*;

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

    /**
     * 针对给定的 object 返回对应的 {@code Object.hashcode}, 如果 object 是数组类型则使用 {@code Arrays.hashcode}, 如果 object 是 {@code null} 返回 0
     *
     * @see Object#hashCode()
     * @see Array#hashCode()
     * @return hashcode
     */
    public static Integer nullSafeHashCode(Object object) {
        if (object == null) {
            return 0;
        }
        if (object.getClass().isArray()) {
            return switch (object) {
                case Object[] objects : yield Arrays.hashCode(objects);
                case boolean[] booleans : yield Arrays.hashCode(booleans);
                case byte[] bytes : yield Arrays.hashCode(bytes);
                case char[] chars : yield Arrays.hashCode(chars);
                case short[] shorts : yield Arrays.hashCode(shorts);
                case int[] ints : yield Arrays.hashCode(ints);
                case long[] longs : yield Arrays.hashCode(longs);
                case double[] doubles : yield Arrays.hashCode(doubles);
                default: yield object.hashCode();
            };
        }
        return object.hashCode();
    }

    /**
     * 比对两个对象是否相等, 优先采用 {@code Object.equals}, 如果是数组类型则采用 {@code arrayEquals} 判断
     *
     * @see ObjectUtils#arrayEquals(Object, Object)
     * @return boolean
     */
    public static boolean nullSafeEquals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        if (o1.equals(o2)) {
            return true;
        }
        if (o1.getClass().isArray() && o2.getClass().isArray()) {
            return arrayEquals(o1, o2);
        }
        return false;
    }

    /**
     * 判断两个数组类型是否相等, 根据其对应的类型采用 {code Arrays.equals} 进行比对
     * 
     * @see Arrays#equals
     * @return boolean
     */
    public static boolean arrayEquals(Object o1, Object o2) {
        if (o1 instanceof Object[] objects1 && o2 instanceof Object[] objects2) {
            return Arrays.equals(objects1, objects2);
        }
        if (o1 instanceof boolean[] booleans1 && o2 instanceof boolean[] booleans2) {
            return Arrays.equals(booleans1, booleans2);
        }
        if (o1 instanceof byte[] bytes1 && o2 instanceof byte[] bytes2) {
            return Arrays.equals(bytes1, bytes2);
        }
        if (o1 instanceof char[] chars1 && o2 instanceof char[] chars2) {
            return Arrays.equals(chars1, chars2);
        }
        if (o1 instanceof double[] doubles1 && o2 instanceof double[] doubles2) {
            return Arrays.equals(doubles1, doubles2);
        }
        if (o1 instanceof float[] floats1 && o2 instanceof float[] floats2) {
            return Arrays.equals(floats1, floats2);
        }
        if (o1 instanceof int[] ints1 && o2 instanceof int[] ints2) {
            return Arrays.equals(ints1, ints2);
        }
        if (o1 instanceof long[] longs1 && o2 instanceof long[] longs2) {
            return Arrays.equals(longs1, longs2);
        }
        if (o1 instanceof short[] shorts1 && o2 instanceof short[] shorts2) {
            return Arrays.equals(shorts1, shorts2);
        }
        return false;
    }
}
