package cn.airframework.core.support;

import org.junit.Assert;
import org.junit.Test;

/**
 * test for {@link ReflectiveParameterNameFinder}
 *
 * @author huangchengxing
 */
public class ReflectiveParameterNameFinderTest {

    @Test
    public void getParameterNames() throws NoSuchMethodException {
        ReflectiveParameterNameFinder reflectiveParameterNameFinder = ReflectiveParameterNameFinder.INSTANCE;
        Assert.assertEquals(0, reflectiveParameterNameFinder.getParameterNames(null).length);
        String[] parameterNames = reflectiveParameterNameFinder.getParameterNames(
            ReflectiveParameterNameFinderTest.class.getDeclaredMethod("test", String.class, Integer.class)
        );
        Assert.assertArrayEquals(new String[]{ "arg0", "arg1" }, parameterNames);
    }

    @SuppressWarnings("unused")
    private static void test(String p1, Integer p2) {
        System.out.println("test");
    }
}
