package cn.airframework.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * {@link Collection}、{@link Map}、{@link Iterator}、{@link Iterable}工具类.
 *
 * @author huangchengxing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectionUtils {

    /**
     * <p>从目标中获取第一个非空元素。<br />
     *
     * @param iterator 迭代器
     * @param <T> 元素类型
     * @return 第一个非空元素
     */
    public static <T> T getFirstNotNull(Iterator<T> iterator) {
        if (Objects.isNull(iterator)) {
            return null;
        }
        while (iterator.hasNext()) {
            T t = iterator.next();
            if (Objects.nonNull(t)) {
                return t;
            }
        }
        return null;
    }

    /**
     * <p>从目标中获取第一个非空元素。<br />
     *
     * @param iterable 可迭代对象
     * @param <T> 元素类型
     * @return 第一个非空元素
     */
    public static <T> T getFirstNotNull(Iterable<T> iterable) {
        if (Objects.isNull(iterable)) {
            return null;
        }
        return getFirstNotNull(iterable.iterator());
    }

    /**
     * <p>反转给定的映射。
     *
     * @param map 要反转的映射
     * @return 反转后的映射
     */
    public static <K, V> Map<V, K> reverse(Map<K, V> map) {
        if (isEmpty(map)) {
            return Collections.emptyMap();
        }
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    /**
     * <p>如果集合不为空，则返回集合，否则返回默认集合。
     *
     * @param collection 集合
     * @param defaultCollection 默认集合
     * @param <T> 元素类型
     * @param <C> 集合类型
     * @return 集合
     */
    public static <T, C extends Collection<T>> C defaultIfEmpty(C collection, C defaultCollection) {
        return isEmpty(collection) ? defaultCollection : collection;
    }

    /**
     * <p>检查coll1是否包含coll2的任何元素。
     *
     * @param coll1 coll1
     * @param coll2 coll2
     * @return 如果coll1包含coll2的任何元素，则返回true，否则返回false
     */
    public static boolean containsAny(Collection<?> coll1, Collection<?> coll2) {
        if (isEmpty(coll1) || isEmpty(coll2)) {
            return false;
        }
        // coll1的大小小于coll2
        if (coll1.size() < coll2.size()) {
            for (Object obj : coll1) {
                if (coll2.contains(obj)) {
                    return true;
                }
            }
            return false;
        }
        // coll1的大小大于或等于coll2
        for (Object obj : coll2) {
            if (coll1.contains(obj)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>检查coll1是否不包含coll2的任何元素。
     *
     * @param coll1 coll1
     * @param coll2 coll2
     * @return 如果coll1不包含coll2的任何元素，则返回true，否则返回false
     */
    public static boolean notContainsAny(Collection<?> coll1, Collection<?> coll2) {
        return !containsAny(coll1, coll2);
    }

    /**
     * <p>从 {@link Collection} 中获取指定索引的值。
     *
     * @param collection 集合
     * @param index 索引，如果索引小于0或大于等于集合大小，则返回null
     * @param <T> 元素类型
     * @return 值
     */
    public static <T> T get(Collection<T> collection, int index) {
        if (Objects.isNull(collection)) {
            return null;
        }
        if (collection instanceof List) {
            // 检查边界
            if (index < 0 || index >= collection.size()) {
                return null;
            }
            return ((List<T>) collection).get(index);
        }
        return get(collection.iterator(), index);
    }

    /**
     * <p>从 {@link Iterable} 中获取指定索引的值。
     *
     * @param iterable 可迭代对象
     * @param index 索引
     * @param <T> 元素类型
     * @return 值
     */
    public static <T> T get(Iterable<T> iterable, int index) {
        if (Objects.isNull(iterable)) {
            return null;
        }
        return get(iterable.iterator(), index);
    }

    /**
     * <p>从 {@link Iterator} 中获取指定索引的值。
     *
     * @param iterable 迭代器
     * @param index 索引
     * @param <T> 元素类型
     * @return 值
     */
    public static <T> T get(Iterator<T> iterable, int index) {
        if (Objects.isNull(iterable)) {
            return null;
        }
        int i = 0;
        while (iterable.hasNext()) {
            T next = iterable.next();
            if (i == index) {
                return next;
            }
            i++;
        }
        return null;
    }

    /**
     * <p>将所有元素添加到集合中。
     *
     * @param collection 集合
     * @param elements 元素
     * @param <T> 元素类型
     * @param <C> 集合类型
     * @return 如果不为null，则返回{@link Collection} 本身，否则如果为null，则返回空集合
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public static <T, C extends Collection<T>> C addAll(C collection, T... elements) {
        if (Objects.isNull(collection)) {
            return (C) Collections.emptyList();
        }
        if (Objects.isNull(elements) || elements.length == 0) {
            return collection;
        }
        collection.addAll(Arrays.asList(elements));
        return collection;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <T, C extends Collection<T>> C addAll(C collection, Collection<T> elements) {
        if (Objects.isNull(collection)) {
            return (C) Collections.emptyList();
        }
        if (Objects.isNull(elements) || elements.isEmpty()) {
            return collection;
        }
        collection.addAll(elements);
        return collection;
    }

    /**
     * <p>创建给定元素的集合。
     *
     * @param collectionFactory 集合工厂
     * @param elements 元素
     * @param <T> 元素类型
     * @param <C> 集合类型
     * @return 集合
     */
    @SafeVarargs
    public static <T, C extends Collection<T>> C newCollection(Supplier<C> collectionFactory, T... elements) {
        C collection = collectionFactory.get();
        Objects.requireNonNull(collection, "从集合工厂获取的集合不能为空");
        if (Objects.isNull(elements) || elements.length == 0) {
            return collection;
        }
        collection.addAll(Arrays.asList(elements));
        return collection;
    }

    /**
     * <p>检查集合是否为空。
     *
     * @param collection 集合
     * @return 如果为空，则返回true
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * <p>检查集合是否不为空。
     *
     * @param collection 集合
     * @return 如果不为空，则返回true
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * <p>检查映射是否为空。
     *
     * @param map 映射
     * @return 如果为空，则返回true
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * <p>检查映射是否不为空。
     *
     * @param map 映射
     * @return 如果不为空，则返回true
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * <p>检查迭代器是否为空。
     *
     * @param iterator 迭代器
     * @return 如果为空，则返回true
     */
    public static boolean isEmpty(Iterator<?> iterator) {
        return iterator == null || !iterator.hasNext();
    }

    /**
     * <p>检查迭代器是否不为空。
     *
     * @param iterator 迭代器
     * @return 如果不为空，则返回true
     */
    public static boolean isNotEmpty(Iterator<?> iterator) {
        return !isEmpty(iterator);
    }

    /**
     * <p>检查可迭代对象是否为空。
     *
     * @param iterable 可迭代对象
     * @return 如果为空，则返回true
     */
    public static boolean isEmpty(Iterable<?> iterable) {
        return iterable == null || isEmpty(iterable.iterator());
    }

    /**
     * <p>检查可迭代对象是否不为空。
     *
     * @param iterable 可迭代对象
     * @return 如果不为空，则返回true
     */
    public static boolean isNotEmpty(Iterable<?> iterable) {
        return !isEmpty(iterable);
    }

    /**
     * 将 {@link Object} 对象适配为 {@link Collection}。
     *
     * @param obj 对象
     * @return 集合
     */
    @SuppressWarnings("unchecked")
    public static Collection<Object> adaptObjectToCollection(Object obj) {
        return switch (obj) {
            case null -> Collections.emptyList();
            case Object[] array -> Arrays.asList(array);
            case Collection<?> coll -> (Collection<Object>) coll;
            case Iterable<?> iter -> {
                List<Object> results = CollectionUtils.newCollection(ArrayList::new);
                iter.forEach(results::add);
                yield results;
            }
            case Iterator<?> iter -> {
                List<Object> results = CollectionUtils.newCollection(ArrayList::new);
                iter.forEachRemaining(results::add);
                yield results;
            }
            default -> Collections.singletonList(obj);
        };
    }
}
