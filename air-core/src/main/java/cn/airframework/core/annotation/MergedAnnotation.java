package cn.airframework.core.annotation;

import cn.airframework.core.util.Asserts;
import cn.airframework.core.util.CollectionUtils;
import cn.airframework.core.util.ReflectUtils;
import cn.airframework.core.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>表示以一个根注解为基础与其他多个注解（通常是根注解的元注解）组合而成的一个合并注解。
 * 
 * <p>合并注解中的所有注解的属性，将根据所处的层级高低，按{@link Alias}的指定配置互相关联，
 * 当通过{@link #getAttributeValue}获取指定属性值，将根据属性间的关联关系从而参数联动或覆盖的效果。<br/>
 * 通过{@link #synthesis}将获取生成一个代理注解，
 * 从该注解获取的任何属性值均等同于调用{@link #getAttributeValue}获得。
 *
 * @author huangchengxing
 * @see AttributeMappedAnnotationMapping
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class MergedAnnotation {

    /**
     * 不存在的注解
     */
    private static final Annotation MISSED_ANNOTATION = () -> Annotation.class;

    /**
     * 根注解类型对应的缓存
     */
    @SuppressWarnings("all")
    private static final Map<CacheKey, Map<Class<? extends Annotation>, AttributeMappedAnnotationMapping>> MAPPING_FROM_ROOT_ANNOTATION_CACHES = new ConcurrentHashMap<>();

    /**
     * 根注解
     */
    @EqualsAndHashCode.Include
    @ToString.Include
    private final AttributeMappedAnnotationMapping root;

    /**
     * 元注解映射
     */
    @EqualsAndHashCode.Include
    private final Map<Class<? extends Annotation>, AttributeMappedAnnotationMapping> mappings;

    /**
     * 合成注解缓存
     */
    private final Map<Class<? extends Annotation>, Annotation> synthesizedAnnotationCaches;

    // region ===== 静态工厂方法 ====

    /**
     * 以首个注解作为根注解，按顺序合并多个注解
     *
     * @param annotations 注解
     * @return 组合注解
     */
    public static MergedAnnotation of(Annotation... annotations) {
        Asserts.isNotEmpty(annotations, "annotations must not be empty");
        Map<Class<? extends Annotation>, AttributeMappedAnnotationMapping> mappings = new LinkedHashMap<>(annotations.length);
        Annotation root = null;
        AttributeMappedAnnotationMapping mapping = null;
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            Asserts.isFalse(mappings.containsKey(annotationType), "duplicate annotation: {}", annotationType);
            mapping = new AttributeMappedAnnotationMapping(mapping, annotationType, annotation);
            if (Objects.isNull(root)) {
                root = annotation;
            }
            mappings.put(annotationType, mapping);
        }
        return new MergedAnnotation(root, Map.copyOf(mappings));
    }

    /**
     * 获取指定类型的注解层接结构对应的类型映射，
     * 默认忽略所有{@code java}和{@code javax}包下的注解。
     *
     * @param root 根注解
     * @return 组合注解
     * @see AnnotationFilter#LANG
     */
    public static MergedAnnotation from(Annotation root) {
        return from(root, AnnotationFilter.LANG);
    }

    /**
     * 获取指定类型的注解层接结构对应的类型映射
     *
     * @param root 根注解
     * @param filter 注解过滤器
     * @return 组合注解
     */
    public static MergedAnnotation from(Annotation root, AnnotationFilter filter) {
        Class<? extends Annotation> rootType = root.annotationType();
        Asserts.isTrue(filter.test(root.annotationType()), "The root cannot be filtered by filter");
        Map<Class<? extends Annotation>, AttributeMappedAnnotationMapping> mappings = MAPPING_FROM_ROOT_ANNOTATION_CACHES.computeIfAbsent(new CacheKey(rootType, filter), k -> {
            Map<Class<? extends Annotation>, AttributeMappedAnnotationMapping> accessed = new LinkedHashMap<>();
            // 按广度优先遍历注解的层级结构
            Deque<AttributeMappedAnnotationMapping> queue = CollectionUtils.newCollection(
                LinkedList::new, new AttributeMappedAnnotationMapping(null, k.annotationType(), null)
            );
            while (!queue.isEmpty()) {
                AttributeMappedAnnotationMapping source = queue.removeFirst();
                Class<? extends Annotation> sourceType = source.getAnnotationType();
                // 已经被访问过或者不满足过滤条件则跳过
                if (!k.filter().test(sourceType)) {
                    continue;
                }
                accessed.put(sourceType, source);
                for (Annotation annotation : source.getAnnotationType().getDeclaredAnnotations()) {
                    Class<? extends Annotation> type = annotation.annotationType();
                    if (!accessed.containsKey(type)) {
                        queue.add(new AttributeMappedAnnotationMapping(source, type, annotation));
                    }
                }
            }
            // 根据根注解和注解类型映射构建一个合成注解
            return Map.copyOf(accessed);
        });
        return new MergedAnnotation(root, mappings);
    }

    // endregion

    /**
     * 基于根注解和关联的注解类型映射构建一个合成注解
     *
     * @param root 根注解
     * @param mappings 注解类型映射
     */
    private MergedAnnotation(Annotation root, Map<Class<? extends Annotation>, AttributeMappedAnnotationMapping> mappings) {
        this.root = mappings.get(root.annotationType()).bound(root);
        this.mappings = mappings;
        this.synthesizedAnnotationCaches = new HashMap<>(mappings.size());
    }

    /**
     * 获取属性值
     *
     * @param attributeName 属性名称
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getAttributeValue(
        Class<? extends Annotation> annotationType, String attributeName) {
        return (T) getMapping(annotationType)
            .map(mapping -> mapping.getAttributeMapping(attributeName))
            .map(attr -> getValueFromAttributeMapping(root, attr))
            .orElse(null);
    }

    /**
     * 是否存在指定注解
     *
     * @param annotationType 注解类型
     * @return 是否
     */
    public boolean isPresent(Class<? extends Annotation> annotationType) {
        return getMapping(annotationType).isPresent();
    }

    /**
     * 是否存在指定类型的元注解
     *
     * @param annotationType 注解类型
     * @return 是否
     */
    public boolean isMetaPresent(Class<? extends Annotation> annotationType) {
        return getMapping(annotationType)
            .filter(mapping -> !mapping.isRoot())
            .isPresent();
    }

    /**
     * 获取元注解
     *
     * @param annotationType 元注解类型
     * @return 元注解
     */
    @Nullable
    public <A extends Annotation> A getMeta(Class<A> annotationType) {
        return getMapping(annotationType)
            .filter(mapping -> !mapping.isRoot())
            .map(AttributeMappedAnnotationMapping::getAnnotation)
            .map(annotationType::cast)
            .orElse(null);
    }

    /**
     * 获取根注解
     *
     * @return 根注解
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getRoot() {
        return (A) root.getAnnotation();
    }

    /**
     * 获取指定类型的注解
     *
     * @param annotationType 注解类型
     * @return 注解
     */
    public <A extends Annotation> A get(Class<A> annotationType) {
        return getMapping(annotationType)
            .map(AttributeMappedAnnotationMapping::getAnnotation)
            .map(annotationType::cast)
            .orElse(null);
    }

    /**
     * <p>根据当前映射对象，通过动态代理生成一个合成注解，该注解相对原生注解：
     * <ul>
     *     <li>支持同注解内通过{@link Alias}构建的别名机制；</li>
     *     <li>支持子注解对元注解的同名同类型属性覆盖机制；</li>
     * </ul>
     * 当{@link AttributeMappedAnnotationMapping#hasMappedAttributes()}为{@code false}时，则该方法返回被包装的原始注解对象。
     *
     * @return 所需的注解，若{@link AttributeMappedAnnotationMapping#hasMappedAttributes()}为{@code false}则返回的是原始的注解对象
     * @see #synthesisAll
     */
    public <A extends Annotation> Optional<A> synthesis(Class<A> annotationType) {
        // 双重检查保证线程安全的创建代理缓存
        Annotation synthesized = synthesizedAnnotationCaches.get(annotationType);
        if (Objects.isNull(synthesized)) {
            synthesized = synthesizedAnnotationCaches.get(annotationType);
            synchronized (this) {
                if (Objects.isNull(synthesized)) {
                    synthesized = getMapping(annotationType)
                        .map(mapping -> mapping.hasMappedAttributes() ? doSynthesis(mapping) : mapping.getAnnotation())
                        .orElse(MISSED_ANNOTATION);
                    synthesizedAnnotationCaches.put(annotationType, synthesized);
                }
            }
        }
        return Optional.of(synthesized)
            .filter(a -> a != MISSED_ANNOTATION)
            .map(annotationType::cast);
    }

    /**
     * <p>获取所有合成注解
     *
     * @return 合成注解列表
     * @see #synthesis
     */
    public List<Annotation> synthesisAll() {
        return mappings.values().stream()
            .map(mapping -> synthesis(mapping.getAnnotationType()))
            .mapMulti(Optional::ifPresent)
            .map(Annotation.class::cast)
            .toList();
    }

    /**
     * 创建一个代理对象
     *
     * @param mapping 注解映射
     * @param <A> 注解类型
     * @return 代理对象
     */
    @SuppressWarnings("unchecked")
    private <A extends Annotation> A doSynthesis(AttributeMappedAnnotationMapping mapping) {
        MergedAnnotationInvocationHandler invocationHandler = new MergedAnnotationInvocationHandler(root, mapping);
        return (A) Proxy.newProxyInstance(
            mapping.getAnnotationType().getClassLoader(),
            new Class[]{ mapping.getAnnotationType(), SynthesizedAnnotation.class },
            invocationHandler
        );
    }

    private static <T> T getValueFromAttributeMapping(
        AttributeMappedAnnotationMapping root, AttributeMappedAnnotationMapping.AttributeMapping attr) {
        if (Objects.isNull(attr)) {
            return null;
        }
        AttributeMappedAnnotationMapping mapping = attr.mapping();
        // 如果属性不取值于根注解
        if (!mapping.isRoot()) {
            Annotation annotation = mapping.getAnnotation();
            return ReflectUtils.invokeRaw(annotation, attr.method());
        }
        // 如果属性取值于根注解，则此时需要解析确认最终值
        attr = root.getAttributeMapping(attr.idx());
        Annotation annotation = root.getAnnotation();
        return ReflectUtils.invokeRaw(annotation, attr.method());
    }

    private Optional<AttributeMappedAnnotationMapping> getMapping(Class<? extends Annotation> annotationType) {
        return Optional.ofNullable(mappings.get(annotationType))
            .map(mapping -> mapping.isRoot() ? root : mapping);
    }

    /**
     * 代理注解处理器，用于为{@link MergedAnnotation}生成代理对象，当从该代理对象上获取属性值时，
     * 总是通过{@link MergedAnnotation#getAttributeValue}获取。
     *
     * @author huangchengxing
     */
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    static class MergedAnnotationInvocationHandler implements InvocationHandler {

        @EqualsAndHashCode.Include
        private final AttributeMappedAnnotationMapping root;
        @EqualsAndHashCode.Include
        private final AttributeMappedAnnotationMapping mapping;
        private final Map<String, Object> valueCaches;

        /**
         * 创建一个代理方法处理器
         *
         * @param mapping 注解映射
         */
        private MergedAnnotationInvocationHandler(AttributeMappedAnnotationMapping root, AttributeMappedAnnotationMapping mapping) {
            this.root = root;
            int methodCount = mapping.getAttributes().length;
            this.mapping = mapping;
            this.valueCaches = new ConcurrentHashMap<>(methodCount);
        }

        /**
         * 调用被代理的方法
         *
         * @param proxy  代理对象
         * @param method 方法
         * @param args   参数
         * @return 返回值
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "equals" -> Objects.equals(this, args[0]);
                case "hashCode" -> this.hashCode();
                case "annotationType" -> mapping.getAnnotationType();
                case "getAttributeValue" -> getAttributeValue((String) args[0]);
                case "toString" -> {
                    String attributes = Stream.of(mapping.getAttributes())
                        .map(attribute -> StringUtils.format("{}={}", attribute.getName(), getAttributeValue(attribute.getName())))
                        .collect(Collectors.joining(", "));
                    yield StringUtils.format("@{}({})", mapping.getAnnotationType().getName(), attributes);
                }
                default -> {
                    Object val = getAttributeValue(method.getName());
                    yield Objects.isNull(val) ? ReflectUtils.invokeRaw(this, method, args) : val;
                }
            };
        }

        private Object getAttributeValue(String attributeName) {
            return valueCaches.computeIfAbsent(
                attributeName, name -> getValueFromAttributeMapping(root, mapping.getAttributeMapping(attributeName))
            );
        }
    }

    /**
     * 表明注解是一个合成的注解
     */
    public interface SynthesizedAnnotation {
        /**
         * 获取属性值
         *
         * @param attributeName 属性名称
         * @return 属性值
         */
        Object getAttributeValue(String attributeName);
    }

    /**
     * 缓存键
     */
    private record CacheKey(Class<? extends Annotation> annotationType, AnnotationFilter filter) { }
}
