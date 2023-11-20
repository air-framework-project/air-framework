package cn.airframework.core.util;

import cn.airframework.core.exception.AirFrameworkException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>{@link Class}工具类
 *
 * @author huangchengxing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClassUtils {

    /**
     * 基本数据类型与包装类型的映射关系
     */
    private static Map<Class<?>, Class<?>> PRIMITIVE_TYPE_TO_BOXED_TYPE;

    /**
     * 包装类型与基本数据类型的映射关系
     */
    private static Map<Class<?>, Class<?>> BOXED_TYPE_TO_PRIMITIVE_TYPE;

    static {
        // 初始化基本数据类型到包装类型的映射
        PRIMITIVE_TYPE_TO_BOXED_TYPE = new HashMap<>(8);
        PRIMITIVE_TYPE_TO_BOXED_TYPE.put(boolean.class, Boolean.class);
        PRIMITIVE_TYPE_TO_BOXED_TYPE.put(byte.class, Byte.class);
        PRIMITIVE_TYPE_TO_BOXED_TYPE.put(char.class, Character.class);
        PRIMITIVE_TYPE_TO_BOXED_TYPE.put(double.class, Double.class);
        PRIMITIVE_TYPE_TO_BOXED_TYPE.put(float.class, Float.class);
        PRIMITIVE_TYPE_TO_BOXED_TYPE.put(int.class, Integer.class);
        PRIMITIVE_TYPE_TO_BOXED_TYPE.put(long.class, Long.class);
        PRIMITIVE_TYPE_TO_BOXED_TYPE.put(short.class, Short.class);
        PRIMITIVE_TYPE_TO_BOXED_TYPE = Map.copyOf(PRIMITIVE_TYPE_TO_BOXED_TYPE);
        BOXED_TYPE_TO_PRIMITIVE_TYPE = new HashMap<>();
        PRIMITIVE_TYPE_TO_BOXED_TYPE.forEach((k, v) -> BOXED_TYPE_TO_PRIMITIVE_TYPE.put(v, k));
        BOXED_TYPE_TO_PRIMITIVE_TYPE = Map.copyOf(BOXED_TYPE_TO_PRIMITIVE_TYPE);
    }

    /**
     * 检查 {@code target} 是否可以从 {@code sourceType} 转换得到
     *
     * @param targetType 目标类型，可以是源类型的父类
     * @param sourceType 源类型
     * @return 是否可转换
     */
    public static boolean isAssignable(@NonNull Class<?> targetType, @NonNull Class<?> sourceType) {
        targetType = targetType.isPrimitive() ? PRIMITIVE_TYPE_TO_BOXED_TYPE.get(targetType) : targetType;
        sourceType = sourceType.isPrimitive() ? PRIMITIVE_TYPE_TO_BOXED_TYPE.get(sourceType) : sourceType;
        return targetType.isAssignableFrom(sourceType);
    }

    /**
     * 检查 {@code target} 是否无法通过 {@code sourceType} 转换得到
     *
     * @param targetType 目标类型，可以是源类型的父类
     * @param sourceType 源类型
     * @return 是否不可转换
     */
    public static boolean isNotAssignable(@NonNull Class<?> targetType, @NonNull Class<?> sourceType) {
        return !isAssignable(targetType, sourceType);
    }

    /**
     * 判断给定的类是否为 {@code Object} 或 {@code Void}。
     *
     * @param clazz 类
     * @return 是否为 {@code Object} 或 {@code Void}
     */
    public static boolean isObjectOrVoid(Class<?> clazz) {
        return Objects.equals(Object.class, clazz)
            || Objects.equals(Void.TYPE, clazz);
    }

    /**
     * 判断给定的类是否为 JDK 类，即包名以 "java." 或 "javax." 开头。
     *
     * @param clazz 类
     * @return 是否为 JDK 类
     */
    public static boolean isJdkClass(Class<?> clazz) {
        Objects.requireNonNull(clazz, "类名不能为空");
        final Package objectPackage = clazz.getPackage();
        // 无法确定其所在的包，可能是代理类？
        if (Objects.isNull(objectPackage)) {
            return false;
        }
        final String objectPackageName = objectPackage.getName();
        return objectPackageName.startsWith("java.")
            || objectPackageName.startsWith("javax.")
            || clazz.getClassLoader() == null;
    }

    /**
     * 根据类名获取类。
     *
     * @param className 类名
     * @return 类实例
     * @throws AirFrameworkException 如果找不到类
     */
    public static Class<?> forName(String className) throws AirFrameworkException {
        Objects.requireNonNull(className, "类名不能为空");
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new AirFrameworkException(e);
        }
    }

    /**
     * 根据类名获取类，如果找不到类，则返回默认类。
     *
     * @param className    类名，可以为 null 或空
     * @param defaultClass 默认类
     * @return 类实例或默认类
     * @throws AirFrameworkException 如果找不到由 className 指定的类
     */
    public static Class<?> forName(@Nullable String className, Class<?> defaultClass) {
        if (StringUtils.isNotEmpty(className)) {
            return forName(className);
        }
        return defaultClass;
    }

    /**
     * 创建给定类型的新实例。
     *
     * @param type 类型
     * @param <T>  类型
     * @return 新实例
     * @throws AirFrameworkException 如果创建实例失败
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(@NonNull Class<?> type) {
        Objects.requireNonNull(type, "类型不能为空");
        try {
            return (T) type.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new AirFrameworkException(e);
        }
    }

    /**
     * 将包路径转换为资源路径。<br />
     * 例如：{@code cn.crane4j.core.util.ClassUtils -> cn/crane4j/core/util/ClassUtils}
     *
     * @param packagePath 类路径
     * @return 资源路径
     */
    public static String packageToPath(String packagePath) {
        Objects.requireNonNull(packagePath, "包路径不能为空");
        return packagePath.replace(".", "/");
    }
}
