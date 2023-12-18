package cn.airframework.core.genericity;

import cn.airframework.core.util.Asserts;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @author kairong.liu
 * @description
 * @date 2023-12-18 20:56
 */
public class MethodParameter {

    private final Executable executable;
    private final int parameterIndex;

    public MethodParameter(Method method, int parameterIndex) {
        Asserts.isNotNull(method, "method must not null");
        this.executable = method;
        this.parameterIndex = validParameterIndex(method, parameterIndex);
    }


    public MethodParameter(Constructor constructor, int parameterIndex) {
        Asserts.isNotNull(constructor, "construct must not null");
        this.executable = constructor;
        this.parameterIndex = validParameterIndex(constructor, parameterIndex);
    }

    private static int validParameterIndex(Executable executable, int parameterIndex) {
        int parameterCount = executable.getParameterCount();
        Asserts.isTrue(parameterCount >= -1 && parameterIndex < parameterCount,
                "Parameter index needs to be between -1 and " + (parameterCount - 1));
        return parameterIndex;
    }

}
