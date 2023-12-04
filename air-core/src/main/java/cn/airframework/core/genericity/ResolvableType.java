package cn.airframework.core.genericity;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * 泛型解析器
 *
 * @author kairong.liu
 * @date 2023-12-04 19:09
 */
public class ResolvableType {
    private final Type type;
    private Class<?> resolved;

    private final TypeVariable typeVariable;

    private volatile ResolvableType superType;
    private volatile ResolvableType[] interfaceTypes;
    private volatile ResolvableType[] generics;
    private volatile Boolean unresolvableGenerics;

    private ResolvableType(Type type, TypeVariable typeVariable) {
        this.type = type;
        this.typeVariable = typeVariable;
    }


    static interface VariableResolver {
        Object getSource();

        ResolvableType resolveVariable(TypeVariable<?> variable);
    }


}
