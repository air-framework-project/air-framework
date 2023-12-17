package cn.airframework.core.annotation;

import cn.airframework.core.util.ArrayUtils;
import cn.airframework.core.util.Asserts;
import cn.airframework.core.util.ClassUtils;
import cn.airframework.core.util.ReflectUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 注解工具类
 *
 * @author huangchengxing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnnotationUtils {

    private static final String VALUE = "value";

    /**
     * <p>从{@code element}与其层级结构上获取合并注解，
     * 该方法不仅可以获得直接存在的注解，还可以获取以元注解形式间接存在的注解。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @return 注解
     * @see MergedAnnotation
     */
    @Nullable
    public static <A extends Annotation> A findMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
        return MergedHierarchicalElement.from(element)
            .getAnnotation(annotationType);
    }

    /**
     * <p>从{@code element}与其层级结构上获取指定类型的注解，如果注解是可重复注解，则一并尝试寻找容器注解。<br/>
     * 该方法不仅可以获得直接存在的注解，还可以获取以元注解形式间接存在的注解。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @return 注解
     * @see MergedAnnotation
     */
    @Nullable
    public static <A extends Annotation> A[] findAllMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
        return MergedHierarchicalElement.from(element)
            .getAnnotationsByType(annotationType);
    }

    /**
     * 从{@code element}上获取合并注解。该方法不仅可以获得直接存在的注解，还可以获取以元注解形式间接存在的注解。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @return 注解
     * @see MergedAnnotation
     */
    @Nullable
    public static <A extends Annotation> A getMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
        return MergedHierarchicalElement.from(element)
            .getDeclaredAnnotation(annotationType);
    }

    /**
     * <p>从{@code element}获取指定类型的注解，如果注解是可重复注解，则一并尝试寻找容器注解。<br/>
     * 该方法不仅可以获得直接存在的注解，还可以获取以元注解形式间接存在的注解。
     *
     * @param element 元素
     * @param annotationType 注解类型
     * @return 注解
     * @see MergedAnnotation
     */
    @Nullable
    public static <A extends Annotation> A[] getAllMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
        return MergedHierarchicalElement.from(element)
            .getDeclaredAnnotationsByType(annotationType);
    }

    /**
     * 当前注解是否包含指定类型的元注解
     *
     * @param annotation 注解
     * @param metaAnnotationType 元注解
     * @return 是否
     */
    public static boolean hasMetaAnnotation(Annotation annotation, Class<? extends Annotation> metaAnnotationType) {
        return MergedAnnotation.from(annotation)
            .isMetaPresent(metaAnnotationType);
    }

    /**
     * 基于属性值构建一个注解对象
     *
     * @param annotationType 注解类型
     * @param attributeValues 属性值
     * @return 注解对象
     * @see ValueMapAnnotationProxy
     */
    public static <A extends Annotation> A toAnnotation(
        Class<A> annotationType, Map<String, Object> attributeValues) {
        return ValueMapAnnotationProxy.of(annotationType, attributeValues);
    }

    /**
     * 构建一个仅包括{@code value}属性的注解
     *
     * @param annotationType 注解类型
     * @param value 属性值
     * @return 注解对象
     * @see ValueMapAnnotationProxy
     */
    public static <A extends Annotation> A toAnnotation(Class<A> annotationType, Object value) {
        Map<String, Object> attributes = Collections.singletonMap(VALUE, value);
        return toAnnotation(annotationType, attributes);
    }

    /**
     * 将一个注解转为属性值集合
     *
     * @param annotation 注解对象
     * @return 属性值集合
     */
    public static Map<String, Object> getAttributeValues(Annotation annotation) {
        return Stream.of(getAnnotationAttributes(annotation.annotationType()))
            .collect(Collectors.toMap(
                Method::getName, m -> ReflectUtils.invokeRaw(annotation, m)
            ));
    }

    /**
     * 获取注解属性方法
     *
     * @param annotationType 注解类型
     * @return 属性方法
     */
    public static Method[] getAnnotationAttributes(Class<? extends Annotation> annotationType) {
        return Stream.of(annotationType.getDeclaredMethods())
            .filter(m -> !ClassUtils.isObjectOrVoid(m.getReturnType()))
            .filter(m -> m.getParameterCount() == 0)
            .filter(m -> !m.isSynthetic())
            .toArray(len -> ArrayUtils.newInstance(Method.class, len));
    }

    /**
     * 从当前元素上查找指定类型的注解，若注解是可重复注解，则一并获取间接存在的注解
     *
     * @param element 注解元素
     * @param annotationType 注解类型
     * @return 注解对象
     */
    public static <A extends Annotation> List<A> getDeclaredRepeatableAnnotations(
        AnnotatedElement element, Class<A> annotationType) {
        List<A> results = new ArrayList<>();
        Optional.ofNullable(element)
            .map(e -> e.getDeclaredAnnotation(annotationType))
            .ifPresent(results::add);
        Optional.ofNullable(element)
            .map(e -> getRepeatableAnnotationFromContainer(e, annotationType))
            .filter(ArrayUtils::isNotEmpty)
            .ifPresent(annotations -> Collections.addAll(results, annotations));
        return results;
    }

    /**
     * 若获取可重复注解
     *
     * @param element 注解元素
     * @param annotationType 注解类型
     * @return 可重复注解
     */
    private static <A> A[] getRepeatableAnnotationFromContainer(AnnotatedElement element, Class<A> annotationType) {
        Annotation container = determineRepeatableContainerType(annotationType)
            .map(element::getDeclaredAnnotation)
            .orElse(null);
        if (Objects.isNull(container)) {
            return ArrayUtils.newInstance(annotationType, 0);
        }
        return getAnnotationFromRepeatableContainer(annotationType, container);
    }

    /**
     * 获取可重复注解的容器类型
     *
     * @param annotationType 注解类型
     * @return 容器类型
     */
    public static <A> Optional<Class<? extends Annotation>> determineRepeatableContainerType(Class<A> annotationType) {
        return Optional.ofNullable(annotationType)
            .map(t -> t.getDeclaredAnnotation(Repeatable.class))
            .map(Repeatable::value);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static <A> A[] getAnnotationFromRepeatableContainer(Class<A> annotationType, Annotation container) {
        // 如果是基于Map合成的注解，则直接获取属性值
        if (ValueMapAnnotationProxy.isSynthesized(container)) {
            return (A[])((ValueMapAnnotationProxy)Proxy.getInvocationHandler(container)).getMemberValues().get(VALUE);
        }
        // 如果是合成注解，则获取合成属性
        if (container instanceof MergedAnnotation.SynthesizedAnnotation synthesizedAnnotation) {
            return (A[]) synthesizedAnnotation.getAttributeValue(VALUE);
        }
        // 如果是普通的注解，则通过反射获取其属性值
        Method valueMethod = ReflectUtils.getMethod(container.annotationType(), VALUE);
        Asserts.isNotNull(
            valueMethod, "The repeatable container annotation [{}] of [{}] must have a 'value' method!",
            annotationType, container.annotationType()
        );
        if (Proxy.isProxyClass(container.getClass())) {
            return (A[]) Proxy.getInvocationHandler(container)
                .invoke(container, valueMethod, null);
        }
        // 无法确认注解类型，则直接通过反射调用其属性方法
        return ReflectUtils.invokeRaw(container, valueMethod);
    }

}
