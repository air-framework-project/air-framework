package cn.airframework.core.genericity;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinWorkerThread;

import cn.airframework.core.genericity.SerializableTypeWrapper.TypeProvider;
import cn.airframework.core.support.Ordered;
import cn.airframework.core.util.Asserts;
import cn.airframework.core.util.ObjectUtils;
import lombok.NonNull;

/**
 * 泛型解析器
 *
 * @author kairong.liu
 * @date 2023-12-04 19:09
 */
public class ResolvableType {

    private final static ResolvableType NONE = new ResolvableType(EmptyType.INSTANCE, null, null, 0);

    private final static ResolvableType[] EMPTY_TYPE_ARRAY = new ResolvableType[0];

    private final static ConcurrentMap<ResolvableType, ResolvableType> cache = new ConcurrentHashMap<>();

    private final Type type;
    private Class<?> resolved;
    private final Integer hash;
    private ResolvableType componentType;
    private final TypeProvider typeProvider;
    private final VariableResolver variableResolver;
    private volatile ResolvableType superType;
    private volatile ResolvableType[] interfaceTypes;
    private volatile ResolvableType[] generics;

    private ResolvableType(Type type, TypeProvider typeProvider, VariableResolver variableResolver) {
        this.type = type;
        this.componentType = null;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.hash = calculateHashCode();
        this.resolved = null;
    }

    private ResolvableType(Type type, TypeProvider typeProvider, VariableResolver variableResolver, Integer hash) {
        this.type = type;
        this.componentType = null;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.hash = hash;
        this.resolved = resolveClass();
    }

    private ResolvableType(Type type, ResolvableType componentType, TypeProvider typeProvider, VariableResolver variableResolver) {
        this.type = type;
        this.componentType = componentType;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.hash = null;
        this.resolved = resolveClass();
    }

    private ResolvableType(Class<?> clazz) {
        this.resolved = clazz != null ? clazz : Object.class;
        this.type = resolved;
        this.hash = null;
        this.componentType = null;
        this.variableResolver = null;
        this.typeProvider = null;
    }

    public static ResolvableType forType(Type type) {
        return forType(type, null, null);
    }

    public static ResolvableType forType(Type type, VariableResolver variableResolver) {
        return forType(type, null, variableResolver);
    }

    public static ResolvableType forType(Type type, ResolvableType owner) {
        return owner != null ? forType(type, owner.asVariableResolver()) : forType(type);
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
        ResolvableType resolvableType = new ResolvableType(type, typeProvider, variableResolver);
        ResolvableType cachedValue = cache.get(resolvableType);
        if (cachedValue == null) {
            cachedValue = new ResolvableType(type, typeProvider, variableResolver, resolvableType.hash);
            cache.put(cachedValue, cachedValue);
        }
        return cachedValue;
    }

    public static ResolvableType forField(Field field, ResolvableType implementResolvableType) {
        ResolvableType owner = implementResolvableType != null ? implementResolvableType : NONE;
        owner = owner.as(field.getDeclaringClass());
        return new ResolvableType(null, new SerializableTypeWrapper.FieldTypeProvider(field), owner.asVariableResolver());
    }

    public static ResolvableType forField(Field field, int nestingLevel) {
        return forField(field).nestingLevel(nestingLevel);
    }

    public static ResolvableType forField(@NonNull Field field) {
        return forType(null, new SerializableTypeWrapper.FieldTypeProvider(field), null);
    }

    public static ResolvableType forConstructParameter(Constructor<?> constructor, int index) {
        return forMethodParameter(new MethodParameter(constructor, index));
    }

    public static ResolvableType forMethodParameter(Method method, int index) {
        Asserts.isNotNull(method, "method param not null");
        return forMethodParameter(new MethodParameter(method, index));
    }

    public static ResolvableType forMethodParameter(MethodParameter methodParameter) {
        return forMethodParameter(methodParameter, null);
    }

    public static ResolvableType forMethodParameter(MethodParameter methodParameter, Type targetType) {
        return forMethodParameter(methodParameter, targetType, methodParameter.getNestingLevel());
    }

    public static ResolvableType forMethodParameter(MethodParameter methodParameter, Type targetType, int nestingLevel) {
        Class<?> containingClass = methodParameter.getContainingClass();
        ResolvableType owner = forType(containingClass);
        return forType(targetType, new SerializableTypeWrapper.MethodParameterTypeProvider(methodParameter), owner.asVariableResolver())
                .nestingLevel(nestingLevel, methodParameter.getPreNestingLevel());
    }

    public static ResolvableType forMethodReturnType(Method method) {
        MethodParameter methodParameter = new MethodParameter(method, -1);
        return forMethodParameter(methodParameter);
    }

    private Class<?> resolveClass() {
        if (this.type == EmptyType.INSTANCE) {
            return null;
        }
        if (this.type instanceof Class<?> clazz) {
            return clazz;
        }
        if (this.type instanceof GenericArrayType) {
            Class<?> resolve = getComponentType().resolve();
            return resolve != null ? Array.newInstance(resolve, 0).getClass() : null;
        }
        return resolveType().resolve();
    }

    public Class<?> resolve() {
        return this.resolved;
    }

    public Class<?> resolve(Class<?> fallback) {
        return this.resolved != null ? this.resolved : fallback;
    }

    public Class<?> resolveGeneric(int index) {
        return getGeneric(index).resolve();
    }

    ResolvableType resolveType() {
        if (this.type instanceof ParameterizedType parameterizedType) {
            return forType(parameterizedType.getRawType(), this.variableResolver);
        }
        if (this.type instanceof WildcardType wildcardType) {
            Type boundType = resolveBound(wildcardType.getLowerBounds());
            if (boundType == null) {
                boundType = resolveBound(wildcardType.getUpperBounds());
            }
            return forType(boundType, this.variableResolver);
        }
        if (this.type instanceof TypeVariable<?> typeVariable) {
            if (this.variableResolver != null) {
                ResolvableType resolvableType = variableResolver.resolveVariable(typeVariable);
                if (resolvableType != null) {
                    return resolvableType;
                }
            }
            return forType(resolveBound(typeVariable.getBounds()), this.variableResolver);
        }
        return NONE;
    }


    ResolvableType resolveVariable(TypeVariable<?> typeVariable) {
        if (this.type instanceof TypeVariable<?>) {
            return resolveType().resolveVariable(typeVariable);
        }
        if (this.type instanceof ParameterizedType parameterizedType) {
            Class<?> resolve = resolve();
            if (resolve == null) {
                return null;
            }
            Type[] parameters = resolve.getTypeParameters();
            for (int index = 0; index < parameters.length; index++) {
                if (parameters[index].getTypeName().equals(typeVariable.getTypeName())) {
                    Type argument = parameterizedType.getActualTypeArguments()[index];
                    return forType(argument, this.variableResolver);
                }
            }
            Type ownerType = parameterizedType.getOwnerType();
            if (ownerType != null) {
                return forType(ownerType, this.variableResolver).resolveVariable(typeVariable);
            }
        }
        if (this.type instanceof WildcardType) {
            ResolvableType result = resolveType().resolveVariable(typeVariable);
            if (result != null) {
                return result;
            }
        }
        if (this.variableResolver != null) {
            return this.variableResolver.resolveVariable(typeVariable);
        }
        return null;
    }

    private Type resolveBound(Type[] bounds) {
        if (bounds == null || bounds.length == 0 || bounds[0] == Object.class) {
            return null;
        }
        return bounds[0];
    }


    public ResolvableType asCollection() {
        return as(Collection.class);
    }

    public ResolvableType asMap() {
        return as(Map.class);
    }

    public ResolvableType as(Class<?> type) {
        if (this == NONE) {
            return NONE;
        }
        Class<?> resolve = resolve();
        if (resolve == null || resolve == type) {
            return this;
        }
        for (ResolvableType interfaceType : getInterfaceTypes()) {
            ResolvableType resolvableType = interfaceType.as(type);
            if (resolvableType != NONE) {
                return resolvableType;
            }
        }
        return getSuperType().as(type);
    }

    private ResolvableType getComponentType() {
        if (this == NONE) {
            return NONE;
        }
        if (this.componentType != null) {
            return componentType;
        }
        if (this.type instanceof Class<?> clazz) {
            return forType(clazz.getComponentType(), this.variableResolver);
        }
        if (this.type instanceof GenericArrayType genericArrayType) {
            return forType(genericArrayType.getGenericComponentType(), this.variableResolver);
        }
        return resolveType().getComponentType();
    }

    public ResolvableType getSuperType() {
        Class<?> resolve = resolve();
        if (resolve == null) {
            return NONE;
        }
        try {
            Type genericSuperclass = resolve.getGenericSuperclass();
            if (genericSuperclass == null) {
                return NONE;
            }
            if (this.superType == null) {
                this.superType = forType(genericSuperclass, this);
            }
            return this.superType;
        } catch (Exception e) {
            return NONE;
        }
    }

    public ResolvableType[] getInterfaceTypes() {
        Class<?> resolve = resolve();
        if (resolve == null) {
            return EMPTY_TYPE_ARRAY;
        }
        if (this.interfaceTypes != null) {
            return this.interfaceTypes;
        }
        this.interfaceTypes = batchCreateTypes(resolve.getGenericInterfaces(), this);
        return interfaceTypes;
    }


    public ResolvableType[] getGenerics() {
        if (this == NONE) {
            return EMPTY_TYPE_ARRAY;
        }
        ResolvableType[] resolvableTypes;
        if (this.generics == null) {
            if (this.type instanceof Class<?> clazz) {
                Type[] parameters = clazz.getTypeParameters();
                resolvableTypes = batchCreateTypes(parameters, this);
            } else if (this.type instanceof ParameterizedType parameterizedType) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                resolvableTypes = batchCreateTypes(typeArguments, this);
            } else {
                resolvableTypes = resolveType().getGenerics();
            }
            this.generics = resolvableTypes;
        }
        return this.generics;
    }

    public ResolvableType getGeneric(int... indexs) {
        ResolvableType[] generics = getGenerics();
        if (indexs == null || indexs.length == 0) {
            return generics.length == 0 ? NONE : generics[0];
        }
        ResolvableType resolvableType = this;
        for (int index : indexs) {
            generics = resolvableType.getGenerics();
            if (index < 0 || generics.length <= index) {
                return NONE;
            }
            resolvableType = generics[index];
        }
        return resolvableType;
    }

    public Object getSource() {
        Object source = this.typeProvider != null ? this.typeProvider.getSource() : null;
        return source != null ? source : this.type;
    }

    public Class<?> getRawClass() {
        if (this.type == this.resolved) {
            return this.resolved;
        }
        Type rawType = this.type;
        if (this.type instanceof ParameterizedType parameterizedType) {
            rawType = parameterizedType.getRawType();
        }
        return rawType instanceof Class<?> clazz ? clazz : null;
    }

    public Class<?> toClass() {
        return resolve(Object.class);
    }

    public boolean isArray() {
        if (this == NONE) {
            return false;
        }
        if (this.type instanceof Class<?> clazz) {
            return clazz.isArray();
        }
        return this.type instanceof GenericArrayType || resolveType().isArray();
    }

    public boolean hasGeneric() {
        return getGenerics().length > 0;
    }

    public ResolvableType nestingLevel(int level) {
        return nestingLevel(level, null);
    }

    public ResolvableType nestingLevel(int level, Map<Integer, Integer> nestingPreLavelMap) {
        ResolvableType result = this;
        for (int i = 2; i < level; i++) {
            if (result.isArray()) {
                result = result.getComponentType();
            } else {
                while (result != NONE && !result.hasGeneric()) {
                    result = getSuperType();
                }
                Integer preIndex = nestingPreLavelMap != null ? nestingPreLavelMap.get(i) : null;
                int index = preIndex != null ? preIndex : result.getGenerics().length - 1;
                result = result.getGeneric(index);
            }
        }
        return result;
    }

    private VariableResolver asVariableResolver() {
        if (this == NONE) {
            return null;
        }
        return new DefaultVariableResolver(this);
    }

    private int calculateHashCode() {
        int hashcode = ObjectUtils.nullSafeHashCode(type);
        if (componentType != null) {
            hashcode = 31 * hashcode + ObjectUtils.nullSafeHashCode(componentType);
        }
        if (typeProvider != null) {
            hashcode = 31 * hashcode + ObjectUtils.nullSafeHashCode(typeProvider);
        }
        if (variableResolver != null) {
            hashcode = 31 * hashcode + ObjectUtils.nullSafeHashCode(variableResolver);
        }
        return hashcode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || this.type.getClass() != other.getClass()) {
            return false;
        }
        ResolvableType otherType = (ResolvableType) other;
        if (!this.equalsType(otherType)) {
            return false;
        }
        if (this.typeProvider != otherType.typeProvider &&
                (this.typeProvider == null || otherType.typeProvider == null ||
                        !ObjectUtils.nullSafeEquals(this.typeProvider.getType(), otherType.typeProvider.getType()))) {
            return false;
        }
        if (this.variableResolver != otherType.variableResolver &&
                (this.variableResolver == null || otherType.variableResolver == null ||
                        !ObjectUtils.nullSafeEquals(this.variableResolver.getSource(), otherType.variableResolver.getSource()))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return hash != null ? hash : calculateHashCode();
    }

    public boolean equalsType(ResolvableType other) {
        return ObjectUtils.nullSafeEquals(this.type, other.type) && ObjectUtils.nullSafeEquals(this.componentType, other.componentType);
    }

    private ResolvableType[] batchCreateTypes(Type[] types, ResolvableType owner) {
        ResolvableType[] resolvableTypes = new ResolvableType[types.length];
        for (int index = 0; index < types.length; index++) {
            resolvableTypes[index] = forType(types[index], owner);
        }
        return resolvableTypes;
    }

    interface VariableResolver extends Serializable {
        Object getSource();

        ResolvableType resolveVariable(TypeVariable<?> variable);
    }

    class DefaultVariableResolver implements VariableResolver {

        final ResolvableType provider;

        DefaultVariableResolver(ResolvableType provider) {
            this.provider = provider;
        }

        @Override
        public Object getSource() {
            return this.provider;
        }

        @Override
        public ResolvableType resolveVariable(TypeVariable<?> variable) {
            return this.provider.resolveVariable(variable);
        }
    }

    static class EmptyType implements Type, Serializable {

        static final Type INSTANCE = new EmptyType();

        @Serial
        Object readResolve() {
            return INSTANCE;
        }
    }

}
