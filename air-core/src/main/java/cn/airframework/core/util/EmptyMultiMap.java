package cn.airframework.core.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 一个空的{@link MultiMap}实现，不支持任何修改操作，且获得的所有值都是空集合。
 *
 * @author huangchengxing
 */
public class EmptyMultiMap<K, V> implements MultiMap<K, V> {

    /**
     * 空映射的单例。
     */
    @SuppressWarnings("rawtypes")
    public static final EmptyMultiMap INSTANCE = new EmptyMultiMap();

    /**
     * 获取映射中键值对的总数。
     *
     * @return 映射中键值对的总数
     */
    @Override
    public int size() {
        return 0;
    }

    /**
     * 映射是否为空。
     *
     * @return 映射是否为空
     */
    @Override
    public boolean isEmpty() {
        return true;
    }

    /**
     * 映射是否包含指定的键。
     *
     * @param o 键
     * @return 映射是否包含指定的键
     */
    @Override
    public boolean containsKey(Object o) {
        return false;
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
        throw new UnsupportedOperationException("EmptyMultiMap is immutable");
    }

    /**
     * 将指定映射中的所有键值对放入映射。
     *
     * @param k        键
     * @param iterable 值的集合
     */
    @Override
    public void putAll(K k, Iterable<? extends V> iterable) {
        throw new UnsupportedOperationException("EmptyMultiMap is immutable");
    }

    /**
     * 将指定映射中的所有键值对放入映射。
     *
     * @param multiMap 映射
     */
    @Override
    public void putAll(MultiMap<K, V> multiMap) {
        throw new UnsupportedOperationException("EmptyMultiMap is immutable");
    }

    /**
     * 移除映射中所有具有指定键的键值对。
     *
     * @param o 键
     * @return 指定键的所有值
     */
    @Override
    public Collection<V> removeAll(Object o) {
        throw new UnsupportedOperationException("EmptyMultiMap is immutable");
    }

    /**
     * 清空映射并移除所有键值对。
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException("EmptyMultiMap is immutable");
    }

    /**
     * 获取指定键的所有值，如果键不存在，则返回一个空集合。
     *
     * @param k 键
     * @return 指定键的所有值
     */
    @Override
    public Collection<V> get(K k) {
        return Collections.emptyList();
    }

    /**
     * 获取映射中的所有键。
     *
     * @return 映射中的所有键
     */
    @Override
    public Set<K> keySet() {
        return Collections.emptySet();
    }

    /**
     * 获取映射中的所有值。
     *
     * @return 映射中的所有值
     */
    @Override
    public Collection<V> values() {
        return Collections.emptyList();
    }

    /**
     * <p>获取映射中的所有键值对。<br />
     * 返回的条目是可修改的，但修改不会影响映射。
     *
     * @return 映射中的所有键值对
     */
    @Override
    public Collection<Map.Entry<K, V>> entries() {
        return Collections.emptyList();
    }

    /**
     * 获取多重映射的 Java 映射，返回的映射是可修改的。
     *
     * @return 多重映射的 Java 映射
     */
    @Override
    public Map<K, Collection<V>> asMap() {
        return Collections.emptyMap();
    }
}
