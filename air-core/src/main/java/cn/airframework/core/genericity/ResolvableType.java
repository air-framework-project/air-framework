package cn.airframework.core.genericity;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import cn.airframework.core.genericity.SerializableTypeWrapper.TypeProvider;
import cn.airframework.core.util.ObjectUtils;

/**
 * 泛型解析器
 *
 * @author kairong.liu
 * @date 2023-12-04 19:09
 */
public class ResolvableType {
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
        this.resolved = null;
    }

    public static ResolvableType forType(Type type, TypeProvider typeProvider, VariableResolver variableResolver) {
        if (type == null && typeProvider != null) {
            type = SerializableTypeWrapper.forProviderType(typeProvider);
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

}
