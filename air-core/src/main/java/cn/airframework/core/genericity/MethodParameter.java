package cn.airframework.core.genericity;

import cn.airframework.core.util.Asserts;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author kairong.liu
 * @description
 * @date 2023-12-18 20:56
 */
@Getter
public class MethodParameter {

    private final Executable executable;
    private final int parameterIndex;
    private Type parameterType;
    private volatile int nestingLevel;
    private volatile Map<Integer, Integer> typeIndexesPerLevel;

    public MethodParameter(Method method, int parameterIndex) {
        Asserts.isNotNull(method, "method must not null");
        this.executable = method;
        this.parameterIndex = validParameterIndex(method, parameterIndex);
    }

    public MethodParameter(MethodParameter original) {
        this.executable = original.executable;
        this.parameterIndex = original.parameterIndex;
        this.nestingLevel = original.nestingLevel;
        this.typeIndexesPerLevel = original.typeIndexesPerLevel;
        this.parameterType = original.parameterType;
    }


    public MethodParameter(Constructor<?> constructor, int parameterIndex, int nestingLevel) {
        Asserts.isNotNull(constructor, "construct must not null");
        this.executable = constructor;
        this.parameterIndex = validParameterIndex(constructor, parameterIndex);
        this.nestingLevel = nestingLevel;
    }

    public MethodParameter(Constructor<?> constructor, int parameterIndex) {
        this(constructor, parameterIndex, 1);
    }


    public Method getMethod() {
        return executable instanceof Method method ? method : null;
    }

    public Constructor<?> getConstruct() {
        return executable instanceof Constructor<?> constructor ? constructor : null;
    }

    private MethodParameter nested(int nestingLevel, Integer typeIndex) {
        MethodParameter copy = clone();
        copy.nestingLevel = nestingLevel;
        if (this.typeIndexesPerLevel != null) {
            copy.typeIndexesPerLevel = new HashMap<>(this.typeIndexesPerLevel);
        }
        if (typeIndex != null) {
            copy.getTypeIndexesPerLevel().put(copy.nestingLevel, typeIndex);
        }
        copy.parameterType = null;
        return copy;
    }

    public Map<Integer, Integer> getPreNestingLevel() {
        if (this.typeIndexesPerLevel == null) {
            this.typeIndexesPerLevel = new HashMap<>(4);
        }
        return this.typeIndexesPerLevel;
    }

    @Override
    public MethodParameter clone() {
        return new MethodParameter(this);
    }

    public Class<?> getDeclaringlClass() {
        return this.executable.getDeclaringClass();
    }

    public Type getGenericParameterType() {
        Type parameterType = this.parameterType;
        if (parameterType == null) {
            if (parameterIndex < 0) {
                Method method = getMethod();
                parameterType = method == null ? void.class : method.getGenericReturnType();
            } else {
                Type[] genericParameterTypes = this.executable.getGenericParameterTypes();
                parameterType = genericParameterTypes[parameterIndex];
            }
            this.parameterType = parameterType;
        }
        return parameterType;
    }

    public Class<?> getContainingClass() {
        return executable.getDeclaringClass();
    }

    private static int validParameterIndex(Executable executable, int parameterIndex) {
        int parameterCount = executable.getParameterCount();
        Asserts.isTrue(parameterCount >= -1 && parameterIndex < parameterCount,
                "Parameter index needs to be between -1 and " + (parameterCount - 1));
        return parameterIndex;
    }

}
