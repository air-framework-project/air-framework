package cn.airframework.core.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * test for {@link Streamable}
 *
 * @author huangchengxing
 */
public class StreamableTest {
    @Test
    public void testStream() {
        // Test with non-empty iterable
        Streamable<Integer> streamable1 = createStreamable(Arrays.asList(1, 2, 3, 4, 5));
        List<Integer> result1 = streamable1.stream().collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5), result1);

        // Test with empty iterable
        Streamable<Integer> streamable2 = createStreamable(List.of());
        List<Integer> result2 = streamable2.stream().collect(Collectors.toList());
        Assert.assertEquals(List.of(), result2);
    }

    @Test
    public void testParallelStream() {
        // Test with non-empty iterable
        Streamable<Integer> streamable1 = createStreamable(Arrays.asList(1, 2, 3, 4, 5));
        List<Integer> result1 = streamable1.parallelStream().collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5), result1);

        // Test with empty iterable
        Streamable<Integer> streamable2 = createStreamable(List.of());
        List<Integer> result2 = streamable2.parallelStream().collect(Collectors.toList());
        Assert.assertEquals(List.of(), result2);
    }

    private Streamable<Integer> createStreamable(Iterable<Integer> iterable) {
        return iterable::iterator;
    }
}
