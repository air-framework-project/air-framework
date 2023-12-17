package cn.airframework.core.annotation;

import cn.airframework.core.util.CollectionUtils;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 表示一个具备层级的结构的对象
 *
 * @author huangchengxing
 */
@ToString(onlyExplicitlyIncluded = true)
public abstract class AbstractHierarchicalElement<E, I extends AbstractHierarchicalElement<E, I>> {

    /**
     * 被包装的元素
     */
    @Getter
    @ToString.Include
    @NonNull
    protected final E root;

    /**
     * 上级元素查找器
     */
    protected final ParentElementDiscoverer<? super E> parentElementDiscoverer;

    /**
     * 上级元素缓存，在调用{@link #getParents}时触发加载。
     */
    private volatile Collection<? extends E> parents;

    /**
     * 创建一个{@link AbstractHierarchicalElement}实例
     *
     * @param root 待包装的{@link AnnotatedElement}
     * @param parentElementDiscoverer  上级元素查找器
     */
    protected AbstractHierarchicalElement(@NonNull E root, @NonNull ParentElementDiscoverer<? super E> parentElementDiscoverer) {
        this.root = root;
        this.parentElementDiscoverer = parentElementDiscoverer;
    }

    // region ===== 访问层级结构 =====

    /**
     * 创建一个{@link AbstractHierarchicalElement}实例
     *
     * @param source 待封装的{@link AnnotatedElement}
     * @return {@link AbstractHierarchicalElement}实例
     */
    protected abstract I createElement(E source);

    /**
     * 获取父级别元素
     *
     * @return 父级别元素
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public final Collection<I> getParents() {
        if (parents == null) {
            synchronized (this) {
                if (parents == null) {
                    var ps = parentElementDiscoverer.get(root);
                    parents = CollectionUtils.isNotEmpty(ps) ? (Collection<E>)ps : Collections.emptyList();
                }
            }
        }
        return parents.stream()
            .map(this::createElement)
            .toList();
    }


    /**
     * 获取包括当前元素在内，层级结构中的所有元素组成的流
     *
     * @param parallel 是否并行流，全量搜索注解时可使用并行流加速
     * @return 流
     */
    public Stream<I> hierarchyStream(boolean parallel) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(hierarchyIterator(), 0), parallel);
    }

    /**
     * 获取包括当前元素在内，层级结构中的所有元素组成的串行流
     *
     * @return 流
     */
    public Stream<I> hierarchyStream() {
        return hierarchyStream(false);
    }

    /**
     * 获取包括当前元素在内，层级结构中的所有元素
     *
     * @return 流
     */
    public List<I> hierarchies() {
        return hierarchyStream(false).toList();
    }

    /**
     * 获取层级结构迭代器，用于按广度优先迭代包括当前元素在内，层级结构中所有元素
     *
     * @return 迭代器实例
     * @see IteratorImpl
     */
    @SuppressWarnings("unchecked")
    private Iterator<I> hierarchyIterator() {
        return new IteratorImpl<>((I)this);
    }

    /**
     * 内部迭代器，用于按广度优先迭代包括当前元素在内，层级结构中的所有元素
     *
     * @author huangchengxing
     */
    protected static class IteratorImpl<E, I extends AbstractHierarchicalElement<E, I>> implements Iterator<I> {
        private final Set<E> accessed = new HashSet<>();
        private final Deque<I> queue = new LinkedList<>();
        IteratorImpl(I root) {
            queue.add(root);
        }
        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }
        @Override
        public I next() {
            I curr = queue.removeFirst();
            accessed.add(curr.getRoot());
            for (I rhe : curr.getParents()) {
                if (!accessed.contains(rhe.getRoot())) {
                    queue.add(rhe);
                }
            }
            return curr;
        }
    }

    /**
     * 上级节点查找器
     *
     * @author huangchengxing
     */
    public interface ParentElementDiscoverer<E> {

        /**
         * 获取上级元素
         *
         * @param element 元素
         * @return 上级元素
         */
        @SuppressWarnings("all")
        @NonNull Collection<? extends E> get(E element);
    }
}
