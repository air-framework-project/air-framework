package cn.airframework.core.genericity;

import cn.airframework.core.util.Asserts;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

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

    public MethodParameter(Method method, int parameterIndex) {
        Asserts.isNotNull(method, "method must not null");
        this.executable = method;
        this.parameterIndex = validParameterIndex(method, parameterIndex);
    }


    public MethodParameter(Constructor<?> constructor, int parameterIndex) {
        Asserts.isNotNull(constructor, "construct must not null");
        this.executable = constructor;
        this.parameterIndex = validParameterIndex(constructor, parameterIndex);
    }


    public Method getMethod() {
        return executable instanceof Method method ? method : null;
    }

    public Constructor<?> getConstruct() {
        return executable instanceof Constructor<?> constructor ? constructor : null;
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

    private static int validParameterIndex(Executable executable, int parameterIndex) {
        int parameterCount = executable.getParameterCount();
        Asserts.isTrue(parameterCount >= -1 && parameterIndex < parameterCount,
                "Parameter index needs to be between -1 and " + (parameterCount - 1));
        return parameterIndex;
    }

}
