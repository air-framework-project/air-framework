package cn.airframework.core.genericity;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.*;

import cn.airframework.core.genericity.SerializableTypeWrapper.TypeProvider;
import cn.airframework.core.util.ObjectUtils;

import static com.sun.beans.finder.ClassFinder.resolveClass;

/**
 * 泛型解析器
 *
 * @author kairong.liu
 * @date 2023-12-04 19:09
 */
public class ResolvableType {

    private final static ResolvableType NONE = new ResolvableType(EmptyType.INSTANCE, null, null, 0);

    private final Type type;
    private Class<?> resolved;
    private final Integer hash;
    private ResolvableType componentType;
    private final TypeProvider typeProvider;
    private final VariableResolver variableResolver;
    private volatile ResolvableType superType;
    private volatile ResolvableType[] interfaceTypes;
    private volatile ResolvableType[] generics;
    private volatile Boolean unresolvableGenerics;

    private ResolvableType(Type type, TypeProvider typeProvider, VariableResolver variableResolver) {
        this.type = type;
        this.componentType = null;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.hash = calculateHashCode();
        this.resolved = resolveClass();
    }

    private ResolvableType(Type type, TypeProvider typeProvider, VariableResolver variableResolver, Integer hash) {
        this.type = type;
        this.componentType = null;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.hash = hash;
        this.resolved = null;
    }

    private ResolvableType(Type type, ResolvableType componentType, TypeProvider typeProvider, VariableResolver variableResolver) {
        this.type = type;
        this.componentType = componentType;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.hash = null;
        this.resolved = null;
    }

    public static ResolvableType forType(Type type, VariableResolver variableResolver) {
        return forType(type, null, variableResolver);
    }

    public static ResolvableType forType(Type type, TypeProvider typeProvider, VariableResolver variableResolver) {
        if (type == null && typeProvider != null) {
            type = SerializableTypeWrapper.forProviderType(typeProvider);
        }
        if (type == null) {
            return NONE;
        }
        if (type instanceof Class) {
            return new ResolvableType(type, null, typeProvider, variableResolver);
        }

    }


    private Class<?> resolveClass() {
        if (this.type == EmptyType.INSTANCE) {
            return null;
        }
        if (this.type instanceof Class<?> clazz) {
            return clazz;
        }
        if (this.type instanceof GenericArrayType) {
            Class<?> resolved = getComponentType().resolve();
            return resolved != null ? Array.newInstance(resolved, 0).getClass() : null;
        }
        return resolvableType().resolve();
    }

    private ResolvableType getComponentType() {
        if (this == NONE) {
            return NONE;
        }
        if (this.componentType != null) {
            return componentType;
        }
        if (this.type instanceof Class<?> clazz) {
            Type componentType = clazz.getComponentType();
            return forType(componentType, this.variableResolver);
        }
        if (this.type instanceof GenericArrayType genericArrayType) {
            return forType(genericArrayType.getGenericComponentType(), this.variableResolver);
        }
        return resolvableType().getComponentType();
    }

    public Class<?> resolve() {
        return this.resolved;
    }

    ResolvableType resolvableType() {
        if (this.type instanceof ParameterizedType parameterizedType) {
            return forType(parameterizedType.getRawType(), this.variableResolver);
        }
        if (this.type instanceof WildcardType wildcardType) {

        }
    }


    interface VariableResolver extends Serializable {
        Object getSource();

        ResolvableType resolveVariable(TypeVariable<?> variable);
    }



    private int calculateHashCode() {
        int hashcode = ObjectUtils.nullSafeHashCode(type);
        if (componentType != null) {
            hashcode = 31 * hashcode + ObjectUtils.nullSafeHashCode(componentType);
        }
        if (typeProvider != null) {
            hashcode = 31 * hashcode + ObjectUtils.nullSafeHashCode(typeProvider);
        }
        if (variableResolver  != null) {
            hashcode = 31 * hashcode + ObjectUtils.nullSafeHashCode(variableResolver);
        }
        return hashcode;
    }


    static class EmptyType implements Type, Serializable {

        static final Type INSTANCE = new EmptyType();

        @Serial
        Object readResolve() {
            return INSTANCE;
        }
    }

}
