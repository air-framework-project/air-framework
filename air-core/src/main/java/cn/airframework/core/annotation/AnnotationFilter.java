package cn.airframework.core.annotation;

import cn.airframework.core.util.Asserts;
import lombok.RequiredArgsConstructor;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

/**
 * 注解过滤器，用于扫描注解层级结构的过程中忽略某个类型的注解和它的元注解
 *
 * @author huangchengxing
 */
@FunctionalInterface
public interface AnnotationFilter {

    /**
     * 过滤所有{@link java.lang}和{@link org.checkerframework}包下的注解
     */
    AnnotationFilter LANG = forTypeNamePrefix("java.lang", "org.checkerframework");

    /**
     * 过滤所有{@link java}和{@link javax}包下的注解
     */
    AnnotationFilter JAVA = forTypeNamePrefix("java", "javax");

    /**
     * 过滤所有{@link cn.airframework}包下的注解
     */
    AnnotationFilter AIR_FRAMEWORK = forTypeNamePrefix("cn.airframework");

    /**
     * 不过滤任何注解
     */
    AnnotationFilter NONE = t -> true;

    /**
     * 过滤任何注解
     */
    AnnotationFilter ALL = t -> false;

    /**
     * 返回一个注解过滤器，仅当所有的过滤器均匹配时才返回{@code true}
     *
     * @param filters 过滤器
     * @return 过滤器
     */
    static AnnotationFilter allMatch(AnnotationFilter... filters) {
        Asserts.isNotEmpty(filters, "filters must not empty!");
        return t -> Stream.of(filters).allMatch(f -> f.test(t));
    }

    /**
     * 返回一个注解过滤器，当任意过滤器匹配时即返回{@code true}
     *
     * @param filters 过滤器
     * @return 过滤器
     */
    static AnnotationFilter anyMatch(AnnotationFilter... filters) {
        Asserts.isNotEmpty(filters, "filters must not empty!");
        return t -> Stream.of(filters).allMatch(f -> f.test(t));
    }

    static AnnotationFilter forTypeNamePrefix(String... prefixes) {
        Asserts.isNotEmpty(prefixes, "prefixes must not empty!");
        return new TypeNameFilter(prefixes);
    }

    /**
     * 是否忽略指定注解
     *
     * @param annotationType 注解类型
     */
    boolean test(Class<? extends Annotation> annotationType);

    /**
     * 按注解类型名称进行匹配的过滤器
     */
    @RequiredArgsConstructor
    class TypeNameFilter implements AnnotationFilter {

        /**
         * 类型前缀
         */
        private final String[] prefixes;

        /**
         * 是否忽略指定注解
         *
         * @param annotationType 注解类型
         */
        @Override
        public boolean test(Class<? extends Annotation> annotationType) {
            String typeName = annotationType.getName();
            return Stream.of(prefixes)
                .noneMatch(typeName::startsWith);
        }
    }
}
