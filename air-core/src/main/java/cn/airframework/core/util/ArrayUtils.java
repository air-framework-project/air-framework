package cn.airframework.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 数组工具类
 *
 * @author huangchengxing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ArrayUtils {

    /**
     * 获取首个非{@code null}元素
     *
     * @param array 数组
     * @param <T> 元素类型
     * @return 首个非{@code null}元素，若数组为空或所有元素都为{@code null}则返回{@code null}
     */
    @SafeVarargs
    public static <T> T getFirstNotNull(T... array) {
        if (Objects.isNull(array)) {
            return null;
        }
        for (T t : array) {
            if (Objects.nonNull(t)) {
                return t;
            }
        }
        return null;
    }

    /**
     * 将元素追加到数组并返回一个新数组。
     *
     * @param array 数组
     * @param elements 元素
     * @param <T> 元素类型
     * @return 添加了元素的新数组，如果数组为{@code null}，则直接返回{@code elements}
     */
    @SafeVarargs
    public static <T> T[] append(T[] array, T... elements) {
        if (Objects.isNull(array)) {
            return elements;
        }
        if (isEmpty(elements)) {
            return Arrays.copyOf(array, array.length);
        }
        T[] result = Arrays.copyOf(array, array.length + elements.length);
        System.arraycopy(elements, 0, result, array.length, elements.length);
        return result;
    }

    /**
     * <p>检查数组是否为空或{@code null}
     *
     * @param array 数组
     * @return 是否
     */
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * <p>检查数组是否不为空或{@code null}
     *
     * @param array 数组
     * @return 是否
     */
    public static boolean isNotEmpty(Object[] array) {
        return !isEmpty(array);
    }

    /**
     * <p>将数组转为使用分隔符分隔的字符串
     *
     * @param array 数组
     * @param mapper 将元素转为到字符串的方法
     * @param delimiter 分隔符
     * @param <T> 元素类型
     * @return 字符串
     */
    public static <T> String join(T[] array, Function<T, String> mapper, String delimiter) {
        if (Objects.isNull(array)) {
            return "";
        }
        return stream(array).map(mapper).collect(Collectors.joining(delimiter));
    }

    /**
     * <p>将数组转为使用分隔符分隔的字符串
     *
     * @param array 数组
     * @param delimiter 分隔符
     * @return 字符串
     */
    public static String join(String[] array, String delimiter) {
        return join(array, Function.identity(), delimiter);
    }

    /**
     * <p>判断{@code target}是否包含在{@code array}中。
     *
     * @param array 数组，如果为null则返回false
     * @param target 目标
     * @param <T> 数组的类型
     * @return {@code target}是否包含在{@code array}中
     */
    public static <T> boolean contains(T[] array, T target) {
        if (Objects.isNull(array)) {
            return false;
        }
        return Arrays.asList(array).contains(target);
    }

    /**
     * <p>获取数组的流，如果数组为null则返回空流。
     *
     * @param array 数组
     * @param <T> 数组的类型
     * @return 数组的流
     */
    public static <T> Stream<T> stream(T[] array) {
        if (Objects.isNull(array)) {
            return Stream.empty();
        }
        return Stream.of(array);
    }

    /**
     * <p>获取数组中指定索引处的元素，如果数组为null或索引超出范围则返回null。
     *
     * @param array 数组
     * @param index 索引
     * @param <T> 数组的类型
     * @return 元素
     */
    public static <T> T get(T[] array, int index) {
        if (Objects.isNull(array)) {
            return null;
        }
        if (index < 0 || index >= array.length) {
            return null;
        }
        return array[index];
    }

    /**
     * <p>获取数组的长度，如果数组为null则返回0。
     *
     * @param array 数组
     * @param <T> 数组的类型
     * @return 数组的长度
     */
    public static <T> int length(T[] array) {
        if (Objects.isNull(array)) {
            return 0;
        }
        return array.length;
    }

    /**
     * <p>比较两个数组是否相等，使用{@link Objects#equals(Object, Object)}进行比较。
     *
     * @param array1 数组1
     * @param array2 数组2
     * @param <T> 数组1的类型
     * @param <U> 数组2的类型
     * @return 如果相等则返回true
     */
    public static <T, U> boolean isEquals(T[] array1, U[] array2) {
        return isEquals(array1, array2, Objects::equals);
    }

    /**
     * <p>比较两个数组是否相等，使用{@code predicate}进行比较。
     *
     * @param array1 数组1
     * @param array2 数组2
     * @param predicate 用于比较的断言
     * @param <T> 数组1的类型
     * @param <U> 数组2的类型
     * @return 如果相等则返回true
     */
    public static <T, U> boolean isEquals(T[] array1, U[] array2, BiPredicate<T, U> predicate) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null) {
            return false;
        }
        int length = array1.length;
        if (length != array2.length) {
            return false;
        }
        predicate = Objects.requireNonNull(predicate, "predicate must not null").negate();
        for (int i = 0; i < length; ++i) {
            T o1 = array1[i];
            U o2 = array2[i];
            if (predicate.test(o1, o2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 创建一个指定类型和长度的数组
     *
     * @param componentType 数组元素类型
     * @param length 数组长度
     * @return 数组
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] newInstance(@NonNull Class<T> componentType, int length) {
        return (T[]) Array.newInstance(componentType, length);
    }

    /**
     * 将集合转为数组
     *
     * @param coll 集合
     * @param componentType 数组元素类型
     * @return 数组
     */
    public static <T> T[] toArray(Collection<T> coll, @NonNull Class<T> componentType) {
        return Objects.isNull(coll) ?
            newInstance(componentType, 0) : coll.toArray(len -> newInstance(componentType, len));
    }
}
