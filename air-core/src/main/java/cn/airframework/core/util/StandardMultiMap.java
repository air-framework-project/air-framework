package cn.airframework.core.util;

import lombok.RequiredArgsConstructor;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * {@link MultiMap}的标准实现
 *
 * @author huangchengxing
 */
@RequiredArgsConstructor
public class StandardMultiMap<K, V, C extends Collection<V>> implements MultiMap<K, V> {

    /**
     * 集合容器
     */
    private final Map<K, C> rawMap;

    /**
     * 值集合工厂
     */
    private final Supplier<C> collectionFactory;

    /**
     * 获取映射中键值对的总数。
     *
     * @return 映射中键值对的总数
     */
    @Override
    public int size() {
        return rawMap.size();
    }

    /**
     * 映射是否为空。
     *
     * @return 映射是否为空
     */
    @Override
    public boolean isEmpty() {
        return rawMap.isEmpty();
    }

    /**
     * 映射是否包含指定的键。
     *
     * @param o 键
     * @return 映射是否包含指定的键
     */
    @SuppressWarnings("all")
    @Override
    public boolean containsKey(Object o) {
        return rawMap.containsKey(o);
    }

    /**
     * 将指定的键值对放入映射。
     *
     * @param k 键
     * @param v 值
     * @return 映射是否发生了变化
     */
    @Override
    public boolean put(K k, V v) {
        return rawMap.computeIfAbsent(k, key -> collectionFactory.get()).add(v);
    }

    /**
     * 将指定映射中的所有键值对放入映射。
     *
     * @param k 键
     * @param iterable 值的集合
     */
    @Override
    public void putAll(K k, Iterable<? extends V> iterable) {
        iterable.forEach(v -> put(k, v));
    }

    /**
     * 将指定映射中的所有键值对放入映射。
     *
     * @param multiMap 映射
     */
    @Override
    public void putAll(MultiMap<K, V> multiMap) {
        multiMap.asMap().forEach(this::putAll);
    }

    /**
     * 移除映射中所有具有指定键的键值对。
     *
     * @param o 键
     * @return 指定键的所有值
     */
    @SuppressWarnings("all")
    @Override
    public Collection<V> removeAll(Object o) {
        C values = rawMap.remove(o);
        return values == null ? Collections.emptyList() : values;
    }

    /**
     * 清空映射并移除所有键值对。
     */
    @Override
    public void clear() {
        rawMap.clear();
    }

    /**
     * 获取指定键的所有值，如果键不存在，则返回一个空集合。
     *
     * @param k 键
     * @return 指定键的所有值
     */
    @Override
    public Collection<V> get(K k) {
        return rawMap.getOrDefault(k, collectionFactory.get());
    }

    /**
     * 获取映射中的所有键。
     *
     * @return 映射中的所有键
     */
    @Override
    public Set<K> keySet() {
        return rawMap.keySet();
    }

    /**
     * 获取映射中的所有值。
     *
     * @return 映射中的所有值
     */
    @Override
    public Collection<V> values() {
        return rawMap.values().stream()
            .flatMap(Collection::stream)
            .toList();
    }

    /**
     * <p>获取映射中的所有键值对。<br />
     * 返回的条目是可修改的，但修改不会影响映射。
     *
     * @return 映射中的所有键值对
     */
    @Override
    public Collection<Map.Entry<K, V>> entries() {
        return rawMap.entrySet().stream()
            .flatMap(e -> e.getValue().stream().map(v -> new AbstractMap.SimpleEntry<>(e.getKey(), v)))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 获取多重映射的 Java 映射，返回的映射是可修改的。
     *
     * @return 多重映射的 Java 映射
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<K, Collection<V>> asMap() {
        return (Map<K, Collection<V>>)rawMap;
    }
}
