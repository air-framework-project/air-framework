package cn.airframework.core.annotation;

import cn.airframework.core.util.Asserts;
import cn.airframework.core.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 合成注解代理
 *
 * @author huangchengxing
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public class ValueMapAnnotationProxy implements InvocationHandler {

    @Getter
    @EqualsAndHashCode.Include
    private final Class<? extends Annotation> type;
    @Getter
    @EqualsAndHashCode.Include
    private final Map<String, Object> memberValues;
    private volatile String stringValue = null;

    /**
     * 创建一个合成注解代理
     *
     * @param annotationType 注解类型
     * @param memberValues 注解成员值
     * @return 合成注解代理
     */
    @SuppressWarnings("all")
    public static <A extends Annotation> A of(@NonNull Class<A> annotationType, @NonNull Map<String, Object> memberValues) {
        Asserts.isNotEmpty(memberValues, "memberValues must not be empty");
        return (A) Proxy.newProxyInstance(
            annotationType.getClassLoader(),
            new Class[]{annotationType, SynthesizedAnnotation.class},
            new ValueMapAnnotationProxy(annotationType, memberValues)
        );
    }

    /**
     * 目标注解是否为合成的注解
     *
     * @param annotation 注解
     * @return 是否
     */
    public static boolean isSynthesized(Annotation annotation) {
        return annotation instanceof SynthesizedAnnotation;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String methodName = method.getName();
        return switch (method.getName()) {
            case "equals" -> {
                Object t = args[0];
                t = (t instanceof SynthesizedAnnotation sa) ? Proxy.getInvocationHandler(sa) : t;
                yield this.equals(t);
            }
            case "toString" -> this.toString();
            case "hashCode" -> this.hashCode();
            case "annotationType" -> this.type;
            default -> memberValues.get(methodName);
        };
    }

    @Override
    public String toString() {
        if (Objects.isNull(stringValue)) {
            synchronized (this) {
                if (Objects.isNull(stringValue)) {
                    String memberValueString = memberValues.entrySet().stream()
                        .map(m -> StringUtils.format("{}={}", m.getKey(), m.getValue()))
                        .collect(Collectors.joining(", "));
                    stringValue = StringUtils.format("@{}({})", getType().getName(), memberValueString);
                }
            }
        }
        return stringValue;
    }

    public interface SynthesizedAnnotation {
    }
}
