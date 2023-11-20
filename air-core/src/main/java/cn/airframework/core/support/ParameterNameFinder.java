package cn.airframework.core.support;

import java.lang.reflect.Method;

/**
 * 参数名称查找器。
 *
 * @author huangchengxing
 */
public interface ParameterNameFinder {

    /**
     * 获取参数名称。
     *
     * @param method 方法
     * @return 参数名称数组
     */
    String[] getParameterNames(Method method);
}

