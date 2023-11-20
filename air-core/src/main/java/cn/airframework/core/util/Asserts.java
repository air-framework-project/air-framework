package cn.airframework.core.util;

import cn.airframework.core.exception.AirFrameworkException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * <p>断言工具类
 *
 * @author huangchengxing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Asserts {

    /**
     * <p>断言两个对象是否相等，否则抛出异常。
     *
     * @param obj1 对象1
     * @param obj2 对象2
     * @param ex 异常提供者
     */
    public static void isEquals(Object obj1, Object obj2, Supplier<RuntimeException> ex) {
        if (!Objects.equals(obj1, obj2)) {
            throw ex.get();
        }
    }

    /**
     * <p>断言两个对象是否相等，否则抛出异常。
     *
     * @param obj1 对象1
     * @param obj2 对象2
     * @param message 异常消息
     * @param args 异常消息参数
     */
    public static void isEquals(Object obj1, Object obj2, String message, Object... args) {
        isEquals(obj1, obj2, () -> new AirFrameworkException(message, args));
    }

    /**
     * <p>断言两个对象是否不相等，否则抛出异常。
     *
     * @param obj1 对象1
     * @param obj2 对象2
     * @param ex 异常提供者
     */
    public static void isNotEquals(Object obj1, Object obj2, Supplier<RuntimeException> ex) {
        if (Objects.equals(obj1, obj2)) {
            throw ex.get();
        }
    }

    /**
     * <p>断言两个对象是否不相等，否则抛出异常。
     *
     * @param obj1 对象1
     * @param obj2 对象2
     * @param message 异常消息
     * @param args 异常消息参数
     */
    public static void isNotEquals(Object obj1, Object obj2, String message, Object... args) {
        isNotEquals(obj1, obj2, () -> new AirFrameworkException(message, args));
    }

    /**
     * <p>断言表达式是否为true，否则抛出异常。
     *
     * @param expression 表达式
     * @param ex 异常提供者
     */
    public static void isTrue(boolean expression, Supplier<RuntimeException> ex) {
        if (!expression) {
            throw ex.get();
        }
    }

    /**
     * <p>断言表达式是否为true，否则抛出异常。
     *
     * @param expression 表达式
     * @param message 异常消息
     * @param args 异常消息参数
     */
    public static void isTrue(boolean expression, String message, Object... args) {
        isTrue(expression, () -> new AirFrameworkException(message, args));
    }

    /**
     * <p>断言表达式是否为false，否则抛出异常。
     *
     * @param expression 表达式
     * @param ex 异常提供者
     */
    public static void isFalse(boolean expression, Supplier<RuntimeException> ex) {
        if (expression) {
            throw ex.get();
        }
    }

    /**
     * <p>断言表达式是否为false，否则抛出异常。
     *
     * @param expression 表达式
     * @param message 异常消息
     * @param args 异常消息参数
     */
    public static void isFalse(boolean expression, String message, Object... args) {
        isFalse(expression, () -> new AirFrameworkException(message, args));
    }

    /**
     * <p>断言对象不为空，否则抛出异常。
     *
     * @param object 对象
     * @param ex 异常提供者
     */
    public static void isNotEmpty(Object object, Supplier<RuntimeException> ex) {
        if (object == null) {
            throw ex.get();
        }
        if ("".equals(object)) {
            throw ex.get();
        }
        if (object instanceof Collection && ((Collection<?>) object).isEmpty()) {
            throw ex.get();
        }
        if (object instanceof Map && ((Map<?, ?>) object).isEmpty()) {
            throw ex.get();
        }
        if (object instanceof Object[] && ((Object[]) object).length == 0) {
            throw ex.get();
        }
    }

    /**
     * <p>断言对象不为空，否则抛出异常。
     *
     * @param object 对象
     * @param message 异常消息
     * @param args 异常消息参数
     */
    public static void isNotEmpty(Object object, String message, Object... args) {
        isNotEmpty(object, () -> new AirFrameworkException(message, args));
    }

    /**
     * <p>断言对象为空，否则抛出异常。
     *
     * @param object 对象
     * @param ex 异常提供者
     */
    public static void isEmpty(Object object, Supplier<RuntimeException> ex) {
        if (object == null) {
            return;
        }
        if ("".equals(object)) {
            return;
        }
        if (object instanceof Collection && ((Collection<?>) object).isEmpty()) {
            return;
        }
        if (object instanceof Map && ((Map<?, ?>) object).isEmpty()) {
            return;
        }
        if (object instanceof Object[] && ((Object[]) object).length == 0) {
            return;
        }
        throw ex.get();
    }

    /**
     * <p>断言对象为空，否则抛出异常。
     *
     * @param object 对象
     * @param message 异常消息
     * @param args 异常消息参数
     */
    public static void isEmpty(Object object, String message, Object... args) {
        isEmpty(object, () -> new AirFrameworkException(message, args));
    }

    /**
     * <p>断言对象不为null，否则抛出异常。
     *
     * @param object 对象
     * @param ex 异常提供者
     */
    public static void isNotNull(Object object, Supplier<RuntimeException> ex) {
        if (object == null) {
            throw ex.get();
        }
    }

    /**
     * <p>断言对象不为null，否则抛出异常。
     *
     * @param object 对象
     * @param message 异常消息
     * @param args 异常消息参数
     */
    public static void isNotNull(Object object, String message, Object... args) {
        isNotNull(object, () -> new AirFrameworkException(message, args));
    }

    /**
     * <p>断言对象为null，否则抛出异常。
     *
     * @param object 对象
     * @param ex 异常提供者
     */
    public static void isNull(Object object, Supplier<RuntimeException> ex) {
        if (object != null) {
            throw ex.get();
        }
    }

    /**
     * <p>断言对象为null，否则抛出异常。
     *
     * @param object 对象
     * @param message 异常消息
     * @param args 异常消息参数
     */
    public static void isNull(Object object, String message, Object... args) {
        isNull(object, () -> new AirFrameworkException(message, args));
    }
}
