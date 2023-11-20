package cn.airframework.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * 一键多值的{@link Map}集合，默认提供了三种实现：
 * <ul>
 *     <li>{@link #arrayListMultimap()} = {@link HashMap} + {@link ArrayList}</li>
 *     <li>{@link #linkedListMultimap()} = {@link LinkedHashMap} + {@link ArrayList}</li>
 *     <li>{@link #linkedHashMultimap()} = {@link LinkedHashMap} + {@link LinkedHashSet}</li>
 * </ul>
 *
 * @author huangchengxing
 * @see StandardMultiMap
 */
public interface MultiMap<K, V> {

    /**
     * 使用 {@link HashMap} 作为底层映射，{@link ArrayList} 作为集合，创建一个新的 {@link MultiMap} 实例。
     *
     * @return 一个新的 {@link MultiMap} 实例
     * @see HashMap
     * @see ArrayList
     */
    static <K, V> MultiMap<K, V> arrayListMultimap() {
        return new StandardMultiMap<>(new HashMap<>(8), ArrayList::new);
    }

    /**
     * 使用 {@link LinkedHashMap} 作为底层映射，{@link ArrayList} 作为集合，创建一个新的 {@link MultiMap} 实例。
     *
     * @return 一个新的 {@link MultiMap} 实例
     * @see LinkedHashMap
     * @see ArrayList
     */
    static <K, V> MultiMap<K, V> linkedListMultimap() {
        return new StandardMultiMap<>(new LinkedHashMap<>(), ArrayList::new);
    }

    /**
     * 使用 {@link LinkedHashMap} 作为底层映射，{@link LinkedHashSet} 作为集合，创建一个新的 {@link MultiMap} 实例。
     *
     * @return 一个新的 {@link MultiMap} 实例
     * @see LinkedHashMap
     * @see LinkedHashSet
     */
    static <K, V> MultiMap<K, V> linkedHashMultimap() {
        return new StandardMultiMap<>(new LinkedHashMap<>(), LinkedHashSet::new);
    }

    /**
     * 获取映射中键值对的总数。
     *
     * @return 映射中键值对的总数
     */
    int size();

    /**
     * 映射是否为空。
     *
     * @return 映射是否为空
     */
    boolean isEmpty();

    /**
     * 映射是否包含指定的键。
     *
     * @param o 键
     * @return 映射是否包含指定的键
     */
    boolean containsKey(Object o);

    /**
     * 将指定的键值对放入映射。
     *
     * @param k 键
     * @param v 值
     * @return 映射是否发生了变化
     */
    boolean put(K k, V v);

    /**
     * 将指定映射中的所有键值对放入映射。
     *
     * @param k 键
     * @param iterable 值的集合
     */
    void putAll(K k, Iterable<? extends V> iterable);

    /**
     * 将指定映射中的所有键值对放入映射。
     *
     * @param multiMap 映射
     */
    void putAll(MultiMap<K, V> multiMap);

    /**
     * 移除映射中所有具有指定键的键值对。
     *
     * @param o 键
     * @return 指定键的所有值
     */
    Collection<V> removeAll(Object o);

    /**
     * 清空映射并移除所有键值对。
     */
    void clear();

    /**
     * 获取指定键的所有值，如果键不存在，则返回一个空集合。
     *
     * @param k 键
     * @return 指定键的所有值
     */
    Collection<V> get(K k);

    /**
     * 获取映射中的所有键。
     *
     * @return 映射中的所有键
     */
    Set<K> keySet();

    /**
     * 获取映射中的所有值。
     *
     * @return 映射中的所有值
     */
    Collection<V> values();

    /**
     * <p>获取映射中的所有键值对。<br />
     * 返回的条目是可修改的，但修改不会影响映射。
     *
     * @return 映射中的所有键值对
     */
    Collection<Map.Entry<K,V>> entries();

    /**
     * 遍历映射中的所有键值对。
     *
     * @param action 操作
     */
    default void forEach(BiConsumer<? super K, ? super V> action) {
        asMap().forEach((k, vs) -> vs.forEach(v -> action.accept(k, v)));
    }

    /**
     * 获取多重映射的 Java 映射，返回的映射是可修改的。
     *
     * @return 多重映射的 Java 映射
     */
    Map<K, Collection<V>> asMap();

    /**
     * <p>映射是否与指定对象相等。<br />
     * 结果为 true 当且仅当指定对象也是 {@link MultiMap}，并且两个映射对于 {@link #asMap()} 返回的 Java 映射的 equals 结果相同。
     *
     * @param o 对象
     * @return 映射是否与指定对象相等
     */
    @Override
    boolean equals(Object o);

    /**
     * <p>获取映射的哈希码。<br />
     * 哈希码定义为 {@link #asMap()} 返回的 Java 映射的哈希码。
     *
     * @return 映射的哈希码
     */
    @Override
    int hashCode();
}
