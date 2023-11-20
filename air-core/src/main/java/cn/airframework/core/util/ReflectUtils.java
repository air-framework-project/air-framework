package cn.airframework.core.util;

import cn.airframework.core.exception.AirFrameworkException;
import cn.airframework.core.support.ParameterNameFinder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 反射工具类
 *
 * @author huangchengxing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReflectUtils {

    private static final String SET_PREFIX = "set";
    private static final String IS_PREFIX = "is";
    private static final String GET_PREFIX = "get";
    private static final Object[] EMPTY_PARAMS = new Object[0];

    /**
     * 声明字段缓存
     */
// TODO 用 WeakHashMap 替换
    private static final Map<Class<?>, Field[]> DECLARED_FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * 字段缓存
     */
    private static final Map<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * 方法缓存
     */
    private static final Map<Class<?>, Method[]> DECLARED_METHOD_CACHE = new ConcurrentHashMap<>();

    /**
     * 声明方法缓存
     */
    private static final Map<Class<?>, Method[]> METHOD_CACHE = new ConcurrentHashMap<>();

    /**
     * 具有接口的声明超类
     */
    private static final Map<Class<?>, Set<Class<?>>> DECLARED_SUPER_CLASS_WITH_INTERFACE = new ConcurrentHashMap<>();

    // ====================== 方法 ======================

    /**
     * {@code source} 是否可以重写 {@code target}，需要满足以下条件：
     * <ul>
     *     <li>方法名相同</li>
     *     <li>返回值类型相同或是子类</li>
     *     <li>参数数量相同，且每个参数的类型相同或是子类</li>
     * <ul>
     * @param source 源方法
     * @param target 目标方法，即被重写的方法
     * @return 是否
     */
    public static boolean isOverrideableFrom(Method source, Method target) {
        if (!Objects.equals(source.getName(), target.getName())) {
            return false;
        }
        if (!target.getReturnType().isAssignableFrom(source.getReturnType())) {
            return false;
        }
        if (source.getParameterCount() != target.getParameterCount()) {
            return false;
        }
        Class<?>[] childParameterTypes = source.getParameterTypes();
        Class<?>[] parentParameterTypes = target.getParameterTypes();
        for (int i = 0; i < childParameterTypes.length; i++) {
            if (ClassUtils.isNotAssignable(parentParameterTypes[i], childParameterTypes[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 调用方法。
     *
     * @param object 对象
     * @param method 要调用的方法
     * @param args 参数
     * @param <T> 返回类型
     * @return 方法调用的返回值
     */
    @SuppressWarnings("all")
    public static <T> T invokeRaw(Object object, Method method, Object... args) {
        setAccessible(method);
        try {
            return (T) method.invoke(object, args);
        } catch (Throwable e) {
            e = (e instanceof InvocationTargetException) ?
                ((InvocationTargetException)e).getTargetException() : e;
            throw new AirFrameworkException(e);
        }
    }

    /**
     * 调用方法。
     *
     * @param object 对象
     * @param method 要调用的方法
     * @param args 参数
     * @param <T> 返回类型
     * @return 方法调用的返回值
     */
    public static <T> T invoke(Object object, Method method, Object... args) {
        Object[] actualArguments = resolveMethodInvocationArguments(method, args);
        return invokeRaw(object, method, actualArguments);
    }

    /**
     * 解析方法调用的参数。
     *
     * @param method 要调用的方法
     * @param args 参数
     * @return 方法调用的参数
     */
    public static Object[] resolveMethodInvocationArguments(Method method, Object... args) {
        int parameterCount = method.getParameterCount();
        if (parameterCount == 0) {
            return EMPTY_PARAMS;
        }
        // 如果 args 为 null，则返回空数组
        if (ArrayUtils.isEmpty(args)) {
            return new Object[parameterCount];
        }
        // 如果参数数量相等，则直接返回
        if (parameterCount == args.length) {
            return args;
        }
        // 如果参数数量不等，则获取解析后的实际参数
        Parameter[] parameters = method.getParameters();
        Object[] actualArguments = new Object[parameterCount];
        int newLen = Math.min(parameters.length, args.length);
        System.arraycopy(args, 0, actualArguments, 0, newLen);
        return actualArguments;
    }

    /**
     * 解析方法的参数名。
     *
     * @param finder 参数名查找器
     * @param method 方法
     * @return 参数名和参数的映射
     */
    @SuppressWarnings("all")
    public static Map<String, Parameter> resolveParameterNames(ParameterNameFinder finder, Method method) {
        Parameter[] parameters = method.getParameters();
        String[] parameterNames = finder.getParameterNames(method);
        if (ArrayUtils.isEmpty(parameters)) {
            return Collections.emptyMap();
        }
        Map<String, Parameter> parameterMap = new LinkedHashMap<>(parameters.length);
        int nameLength = ArrayUtils.length(parameterNames);
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String parameterName = nameLength <= i ? parameter.getName() : parameterNames[i];
            parameterMap.put(parameterName, parameter);
        }
        return parameterMap;
    }

    /**
     * 获取类型的声明方法。
     *
     * @param type 类型
     * @return 方法数组
     */
    public static Method[] getDeclaredMethods(Class<?> type) {
        return DECLARED_METHOD_CACHE.computeIfAbsent(
            type, t -> Stream.of(t.getDeclaredMethods())
                .filter(m -> !m.isSynthetic())
                .toArray(Method[]::new)
        );
    }

    /**
     * 根据名称和参数类型获取声明方法。
     *
     * @param type 类型
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     * @return 如果找到，返回方法；否则返回 null
     */
    @Nullable
    public static Method getDeclaredMethod(
        Class<?> type, String methodName, @Nullable Class<?>... parameterTypes) {
        return findSpecificMethod(getDeclaredMethods(type), methodName, parameterTypes);
    }

    /**
     * 获取方法。
     *
     * @param type 类型
     * @return 方法数组
     * @see Class#getMethods()
     */
    public static Method[] getMethods(Class<?> type) {
        return METHOD_CACHE.computeIfAbsent(type, curr -> {
            List<Method> methods = new ArrayList<>();
            traverseTypeHierarchy(curr, t -> methods.addAll(Arrays.asList(getDeclaredMethods(t))));
            return methods.toArray(new Method[0]);
        });
    }

    /**
     * 根据名称和参数类型获取方法。
     *
     * @param type 类型
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     * @return 如果找到，返回方法；否则返回 null
     */
    @Nullable
    public static Method getMethod(
        Class<?> type, String methodName, @Nullable Class<?>... parameterTypes) {
        return findSpecificMethod(getMethods(type), methodName, parameterTypes);
    }

    /**
     * 在方法列表中查找指定的方法，根据名称和参数类型。<br />
     * 如果 parameterTypes:
     * <ul>
     *     <li>为 null，只按名称查找方法；</li>
     *     <li>为空，按名称查找无参数的方法；</li>
     *     <li>不为空，按名称和参数查找方法；</li>
     * </ul>
     *
     * @param methods 方法列表
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     * @return 方法
     */
    private static Method findSpecificMethod(Method[] methods, String methodName, Class<?>[] parameterTypes) {
        Predicate<Method> predicate = method -> method.getName().equals(methodName);
        if (Objects.nonNull(parameterTypes)) {
            predicate = parameterTypes.length > 0 ?
                predicate.and(method -> Arrays.equals(method.getParameterTypes(), parameterTypes)) :
                predicate.and(method -> method.getParameterCount() == 0);
        }
        return Stream.of(methods)
            .filter(predicate)
            .findFirst()
            .orElse(null);
    }

    /**
     * 查找 getter 方法。
     *
     * @param beanType bean 类型
     * @param field 字段
     * @return Optional 包装的 getter 方法
     */
    public static Optional<Method> findGetterMethod(Class<?> beanType, Field field) {
        // 查找 isXXX 方法
        Class<?> fieldType = field.getType();
        if (boolean.class.equals(fieldType) || Boolean.class.equals(fieldType)) {
            String booleanGetterName = StringUtils.upperFirstAndAddPrefix(field.getName(), IS_PREFIX);
            return Optional.ofNullable(getMethod(beanType, booleanGetterName));
        }

        // 查找 getXXX 方法
        String getterName = StringUtils.upperFirstAndAddPrefix(field.getName(), GET_PREFIX);
        Method method = getMethod(beanType, getterName);
        if (Objects.nonNull(method)) {
            return Optional.of(method);
        }

        // 查找 fluent 方法
        getterName = field.getName();
        return Optional.ofNullable(getMethod(beanType, getterName));
    }

    /**
     * 查找 getter 方法。
     *
     * @param beanType bean 类型
     * @param fieldName 字段名
     * @return Optional 包装的 getter 方法
     */
    public static Optional<Method> findGetterMethod(Class<?> beanType, String fieldName) {
        // 查找 getXXX 方法
        String getterName = StringUtils.upperFirstAndAddPrefix(fieldName, GET_PREFIX);
        Method method = getMethod(beanType, getterName);
        if (Objects.nonNull(method)) {
            return Optional.of(method);
        }

        // 查找 fluent 方法
        Optional<Method> fluentGetter = Optional.ofNullable(getMethod(beanType, fieldName));
        if (fluentGetter.isPresent()) {
            return fluentGetter;
        }

        // 查找 isXXX 方法
        String booleanGetterName = StringUtils.upperFirstAndAddPrefix(fieldName, IS_PREFIX);
        return Optional.ofNullable(getMethod(beanType, booleanGetterName));
    }

    /**
     * 查找 setter 方法。
     *
     * @param beanType bean 类型
     * @param field 字段
     * @return Optional 包装的 setter 方法
     */
    public static Optional<Method> findSetterMethod(Class<?> beanType, Field field) {
        // 查找 setXXX 方法
        Class<?> fieldType = field.getType();
        String setterName = StringUtils.upperFirstAndAddPrefix(field.getName(), SET_PREFIX);
        Method method = getMethod(beanType, setterName, fieldType);
        if (Objects.nonNull(method)) {
            return Optional.of(method);
        }

        // 查找 fluent 方法
        setterName = field.getName();
        method = getMethod(beanType, setterName, fieldType);
        if (Objects.nonNull(method)) {
            return Optional.of(method);
        }

        // 查找 isXXX 方法
        String booleanSetterName = StringUtils.upperFirstAndAddPrefix(field.getName(), IS_PREFIX);
        return Optional.ofNullable(getMethod(beanType, booleanSetterName, fieldType));
    }

    /**
     * 查找 setter 方法。
     *
     * @param beanType  bean 类型
     * @param fieldName 字段名
     * @return Optional 包装的 setter 方法
     */
    public static Optional<Method> findSetterMethod(Class<?> beanType, String fieldName) {
        // 查找 setXXX 方法
        String setterName = StringUtils.upperFirstAndAddPrefix(fieldName, SET_PREFIX);
        Optional<Method> method = findMethod(beanType, setterName, 1);
        if (method.isPresent()) {
            return method;
        }

        // 查找 fluent 方法
        Optional<Method> fluentSetter = findMethod(beanType, fieldName, 1);
        if (fluentSetter.isPresent()) {
            return fluentSetter;
        }

        // 查找 isXXX 方法
        String booleanSetterName = StringUtils.upperFirstAndAddPrefix(fieldName, IS_PREFIX);
        return findMethod(beanType, booleanSetterName, 1);
    }

    /**
     * 查找类中具备指定参数数量的方法
     *
     * @param beanType 类
     * @param methodName 方法名
     * @param parameterCount 方法参数数量
     * @return 方法
     */
    public static Optional<Method> findMethod(Class<?> beanType, String methodName, int parameterCount) {
        Method[] methods = getMethods(beanType);
        return Stream.of(methods)
            .filter(m -> m.getName().equals(methodName))
            .filter(m -> m.getParameterCount() == parameterCount)
            .findFirst();
    }

    // ====================== 注解 ======================

    /**
     * 判断 {@code element} 是否为 JDK 创建的元素。
     *
     * @param element 元素
     * @return boolean
     */
    public static boolean isJdkElement(AnnotatedElement element) {
        Class<?> checkedClass = element.getClass();
        if (element instanceof Class) {
            checkedClass = (Class<?>)element;
            if (checkedClass.isPrimitive()) {
                return true;
            }
        } else if (element instanceof Member member) {
            checkedClass = member.getDeclaringClass();
        }
        // 然后检查包名是否以 "javax." 或 "java." 开头
        return ClassUtils.isJdkClass(checkedClass);
    }

    // ====================== 类 ======================

    /**
     * 获取声明的超类和接口。
     *
     * @param type 类型
     * @return 声明的超类和接口
     */
    public static Set<Class<?>> getDeclaredSuperClassWithInterface(Class<?> type) {
        return DECLARED_SUPER_CLASS_WITH_INTERFACE.computeIfAbsent(type, k -> {
            Set<Class<?>> result = new LinkedHashSet<>();
            Class<?> superClass = type.getSuperclass();
            if (superClass != null) {
                result.add(superClass);
            }
            result.addAll(Arrays.asList(type.getInterfaces()));
            return result;
        });
    }

    /**
     * 遍历类型层次结构。
     *
     * @param beanType bean 类型
     * @param consumer 每个类型的操作
     */
    public static void traverseTypeHierarchy(Class<?> beanType, Consumer<Class<?>> consumer) {
        traverseTypeHierarchy(beanType, true, consumer);
    }

    /**
     * 遍历类型层次结构。
     *
     * @param beanType bean 类型
     * @param includeRoot 是否包括根类型
     * @param consumer 每个类型的操作
     */
    public static void traverseTypeHierarchy(Class<?> beanType, boolean includeRoot, Consumer<Class<?>> consumer) {
        traverseTypeHierarchyWhile(beanType, includeRoot, t -> {
            consumer.accept(t);
            return false;
        });
    }

    /**
     * 遍历类型层次结构，直到遇到第一个不匹配的类型。
     *
     * @param beanType bean类型
     * @param includeRoot 是否包括根类型
     * @param consumer 每个类型的操作
     */
    public static void traverseTypeHierarchyWhile(Class<?> beanType, boolean includeRoot, Predicate<Class<?>> consumer) {
        Set<Class<?>> accessed = new HashSet<>();
        Deque<Class<?>> typeQueue = new LinkedList<>();
        typeQueue.add(beanType);
        while (!typeQueue.isEmpty()) {
            Class<?> type = typeQueue.removeFirst();
            accessed.add(type);
            // 对当前类型进行操作
            if ((includeRoot || type != beanType) && (consumer.test(type))) {
                    return;
            }
            // 然后找到超类和接口
            Set<Class<?>> declaredSuperClassWithInterface = getDeclaredSuperClassWithInterface(type);
            declaredSuperClassWithInterface.remove(Object.class);
            declaredSuperClassWithInterface.removeAll(accessed);
            CollectionUtils.addAll(typeQueue, declaredSuperClassWithInterface);
        }
    }

    // ====================== field ======================

    /**
     * 根据名称获取声明的字段。
     *
     * @param type 类型
     * @param fieldName 字段名称
     * @return 指定的字段，如果找不到则为null
     */
    @Nullable
    public static Field getDeclaredField(Class<?> type, String fieldName) {
        return Stream.of(getDeclaredFields(type))
            .filter(f -> Objects.equals(f.getName(), fieldName))
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取声明的字段。
     *
     * @param type 类型
     * @return 字段数组
     */
    public static Field[] getDeclaredFields(Class<?> type) {
        return DECLARED_FIELD_CACHE.computeIfAbsent(type, Class::getDeclaredFields);
    }

    /**
     * 根据名称获取字段。
     *
     * @param type 类型
     * @param fieldName 字段名称
     * @return 指定的字段，如果找不到则为null
     */
    @Nullable
    public static Field getField(Class<?> type, String fieldName) {
        return Stream.of(getFields(type))
            .filter(f -> Objects.equals(f.getName(), fieldName))
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取字段。
     *
     * @param type 类型
     * @return 字段数组
     */
    public static Field[] getFields(Class<?> type) {
        return FIELD_CACHE.computeIfAbsent(type, t -> {
            List<Field> fields = new ArrayList<>();
            traverseTypeHierarchy(t, curr -> fields.addAll(Arrays.asList(getDeclaredFields(curr))));
            return fields.toArray(new Field[0]);
        });
    }

    /**
     * 获取字段值。
     *
     * @param target 目标对象
     * @param fieldName 字段名称
     * @param <T> 字段类型
     * @return 字段值
     */
    @Nullable
    public static <T> T getFieldValue(Object target, String fieldName) {
        Field field = getField(target.getClass(), fieldName);
        return Objects.isNull(field) ? null : getFieldValue(target, field);
    }

    /**
     * 获取字段值。
     *
     * @param target 目标对象
     * @param field 字段
     * @param <T> 字段类型
     * @throws NullPointerException 当字段为null时抛出
     * @return 字段值
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object target, Field field) {
        Objects.requireNonNull(field, "field must not be null");
        setAccessible(field);
        try {
            return (T) field.get(target);
        } catch (Exception e) {
            throw new AirFrameworkException(e);
        }
    }

    @SuppressWarnings("all")
    public static <T extends AccessibleObject> void setAccessible(T accessibleObject) {
        if (!accessibleObject.isAccessible()) {
            accessibleObject.setAccessible(true);
        }
    }

}
