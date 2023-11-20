package cn.airframework.core.support;

import org.junit.Assert;
import org.junit.Test;

/**
 * test for {@link Ordered}
 *
 * @author huangchengxing
 */
public class OrderedTest {

    @Test
    public void getSort() {
        Assert.assertEquals(Integer.MAX_VALUE, new Foo().getSort());
    }

    private static class Foo implements Ordered {
    }
}
