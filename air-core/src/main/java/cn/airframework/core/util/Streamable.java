package cn.airframework.core.util;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 表示一个可以提供由内部元素组成的流的容器对象
 *
 * @author huangchengxing
 */
public interface Streamable<T> extends Iterable<T> {

    /**
     * 获取流
     *
     * @return 流
     */
    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * 获取并行流
     *
     * @return 流
     */
    default Stream<T> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }
}
