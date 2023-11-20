package cn.airframework.core.support;

/**
 * 表示一个可以调用的方法对象
 *
 * @author huangchengxing
 */
@FunctionalInterface
public interface MethodInvoker {

    /**
     * 调用方法。
     *
     * @param target 目标对象
     * @param args 参数
     * @return 调用结果
     */
    Object invoke(Object target, Object... args);
}