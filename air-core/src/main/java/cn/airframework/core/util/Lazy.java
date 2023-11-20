package cn.airframework.core.util;

import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.function.Supplier;

/**
 * <p>懒加载工具，用于延迟初始化某个单例对象。<br/>
 * 注意，<b>如果工厂方法返回了{@code null}，则依然认为已经完成初始化</b>。
 *
 * @author huangchengxing
 * @param <T> 对象类型
 */
@RequiredArgsConstructor
public class Lazy<T> implements Supplier<T> {

    /**
     * 空对象，用于表示单例尚未初始化
     */
    private static final Object UNINITIALIZED_VALUE = new Object();

    /**
     * 单例
     */
    private volatile Object value = UNINITIALIZED_VALUE;

    /**
     * 工厂方法
     */
    @NonNull
    private final Supplier<T> supplier;

    /**
     * 懒加载获取单例
     *
     * @return the value
     */
    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        if (value == UNINITIALIZED_VALUE) {
            synchronized (this) {
                if (value == UNINITIALIZED_VALUE) {
                    value = supplier.get();
                }
            }
        }
        return (T) value;
    }

    /**
     * 移除单例
     */
    public synchronized void reset() {
        this.value = UNINITIALIZED_VALUE;
    }

    /**
     * 单例是否已经初始化
     */
    public Boolean isInitialized() {
        return value != UNINITIALIZED_VALUE;
    }
}
