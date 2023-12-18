package cn.airframework.core.genericity;

import cn.airframework.core.util.ObjectUtils;
import cn.airframework.core.util.ReflectUtils;

import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 *
 * @author kairong.liu
 * @date 2023-12-04 20:35
 */
public final class SerializableTypeWrapper {

    private static final Class<?>[] SUPPORTED_SERIALIZABLE_TYPES ={
            GenericArrayType.class, ParameterizedType.class, TypeVariable.class, WildcardType.class
    };

    // 暂时使用concurrent hash map, 后续存在以软引用为 key 的 map、专门的 cache map 则改正
    static final ConcurrentHashMap<TypeProvider, Type> cache = new ConcurrentHashMap<>(256);

    private SerializableTypeWrapper() {

    }


    public static <T extends Type> T unwrap(T type) {
        Type unwrapped = null;
        if (type instanceof SerializableTypeProxy proxy) {
            unwrapped = proxy.getTypeProvider().getType();
        }
        return unwrapped == null ? type : (T) unwrapped;
    }

    public static Type forProviderType(TypeProvider typeProvider) {
        Type providerType = typeProvider.getType();
        if (providerType == null || providerType instanceof Serializable) {
            return providerType;
        }

        Type cached = cache.get(typeProvider);
        if (cached != null) {
            return cached;
        }

        for (Class<?> type : SUPPORTED_SERIALIZABLE_TYPES) {
            if (type.isInstance(providerType)) {
                ClassLoader classLoader = providerType.getClass().getClassLoader();
                Class<?>[] interfaces = new Class[] {type, SerializableTypeProvider.class, Serializable.class};
                TypeProxyInvocationHandler handler = new TypeProxyInvocationHandler(typeProvider);
                cached = (Type) Proxy.newProxyInstance(classLoader, interfaces, handler);
                cache.put(typeProvider, cached);
                return cached;
            }
        }
        throw new IllegalArgumentException("Unsupported Type Class: " + providerType.getClass().getName());
    }

    interface SerializableTypeProvider {
        TypeProvider getTypeProvider();
    }

    static class TypeProxyInvocationHandler implements InvocationHandler, Serializable {

        private final TypeProvider typeProvider;

        public TypeProxyInvocationHandler(TypeProvider typeProvider) {
            this.typeProvider = typeProvider;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "equals":
                    Object other = args[0];
                    if (other instanceof Type otherType) {
                        other = unwrap(otherType);
                        return ObjectUtils.nullSafeEquals(this.typeProvider.getType(), other);
                    }
                case "hashCode":
                    return ObjectUtils.nullSafeHashCode(this.typeProvider.getType());
                case "getTypeProvider":
                    return this.typeProvider;
            }

            if (Type.class == method.getReturnType() && ObjectUtils.isEmpty(args)) {
                return forProviderType(new MethodInvokeTypeProvider(typeProvider, method, -1));
            } else if (Type[].class == method.getReturnType() && ObjectUtils.isEmpty(args)) {
                Object returnValue = ReflectUtils.invoke(this.typeProvider.getType(), method);
                if (returnValue == null) {
                    return null;
                }
                Type[] result = new Type[((Type[]) returnValue).length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = forProviderType(new MethodInvokeTypeProvider(this.typeProvider, method, i));
                }
                return result;
            }
            return ReflectUtils.invoke(typeProvider, method, args);
        }
    }

    interface SerializableTypeProxy {
        TypeProvider getTypeProvider();
    }

    public interface TypeProvider extends Serializable {
        Type getType();
        default Object getSource() {
            return null;
        }
    }

    static class FieldTypeProvider implements TypeProvider {
        private Field field;
        private final String filedName;
        private final Class<?> declaringClass;

        public FieldTypeProvider(Field field) {
            this.field = field;
            this.filedName = field.getName();
            this.declaringClass = field.getDeclaringClass();
        }

        @Override
        public Type getType() {
            return this.field.getGenericType();
        }

        @Override
        public Object getSource() {
            return this.field;
        }

    }

    static class MethodInvokeTypeProvider implements TypeProvider {

        private final TypeProvider typeProvider;
        private final Method method;
        private final int index;
        private final String methodName;
        private final Class<?> declaringClass;

        private transient volatile Object result;

        public MethodInvokeTypeProvider(TypeProvider typeProvider, Method method, int index) {
            this.typeProvider = typeProvider;
            this.method = method;
            this.index = index;
            this.methodName = method.getName();
            this.declaringClass = method.getDeclaringClass();
        }

        @Override
        public Type getType() {
            Object result = this.result;
            if (result == null) {
                result = ReflectUtils.invoke(typeProvider, method);
            }
            return result instanceof Type[] types ? types[index] : (Type) result;
        }

        @Override
        public Object getSource() {
            return null;
        }

        @Serial
        private void readObject(ObjectInputStream inputStream){

        }
    }

    static class MethodParameterTypeProvider implements TypeProvider {

        private Method method;
        private int index;

        @Override
        public Type getType() {
            return null;
        }

        @Override
        public Object getSource() {
            return TypeProvider.super.getSource();
        }
    }
}
