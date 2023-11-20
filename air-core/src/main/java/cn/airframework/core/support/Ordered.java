package cn.airframework.core.support;

/**
 * 表示一个允许根据排序值从小到大排序的对象。
 *
 * @author huangchengxing
 */
public interface Ordered {

    /**
     * <p>获取排序值。<br />
     * 值越小，对象的优先级越高。
     *
     * @return 排序值
     */
    default int getSort() {
        return Integer.MAX_VALUE;
    }
}