package cn.airframework.core.support;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * <p>{@link ParameterNameFinder}的简单实现，基于{@link Parameter#getName()}获取参数名。<br/>
 * 在编译时若不指定保留参数名，则将获取到{@code arg0}、{@code arg1}……这种格式的参数名。
 *
 * @author huangchengxing
 * @see Parameter#getName()
 */
public class ReflectiveParameterNameFinder implements ParameterNameFinder {

    public static final ReflectiveParameterNameFinder INSTANCE = new ReflectiveParameterNameFinder();

    /**
     * 空数组
     */
    private static final String[] EMPTY_ARRAY = new String[0];

    /**
     * 获取参数名称。
     *
     * @param method 方法
     * @return 参数名称数组
     */
    @Override
    public String[] getParameterNames(Method method) {
        return Objects.isNull(method) ?
            EMPTY_ARRAY : Stream.of(method.getParameters()).map(Parameter::getName).toArray(String[]::new);
    }
}