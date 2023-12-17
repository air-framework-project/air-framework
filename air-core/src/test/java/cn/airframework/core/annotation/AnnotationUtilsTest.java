package cn.airframework.core.annotation;

import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * test for {@link AnnotationUtils}
 *
 * @author huangchengxing
 */
public class AnnotationUtilsTest {

    @Test
    public void testFindMergedAnnotation() {
        @SampleAnnotation
        class SampleClass {}

        SampleAnnotation annotation = AnnotationUtils.findMergedAnnotation(SampleClass.class, SampleAnnotation.class);
        Assert.assertNotNull(annotation);
    }

    @Test
    public void testFindAllMergedAnnotation() {
        @SampleRepeatableAnnotation("first")
        @SampleRepeatableAnnotation("second")
        class SampleClass {}

        var annotations = AnnotationUtils.findAllMergedAnnotation(SampleClass.class, SampleRepeatableAnnotation.class);
        Assert.assertNotNull(annotations);
        Assert.assertEquals(2, annotations.length);
        Assert.assertEquals("first", annotations[0].value());
        Assert.assertEquals("second", annotations[1].value());
    }

    @Test
    public void testGetMergedAnnotation() {
        @SampleAnnotation
        class SampleClass {}

        SampleAnnotation annotation = AnnotationUtils.getMergedAnnotation(SampleClass.class, SampleAnnotation.class);
        Assert. assertNotNull(annotation);
    }

    @Test
    public void testGetAllMergedAnnotation() {
        @SampleRepeatableAnnotation("first")
        @SampleRepeatableAnnotation("second")
        class SampleClass {}

        var annotations = AnnotationUtils.getAllMergedAnnotation(SampleClass.class, SampleRepeatableAnnotation.class);
        Assert.assertNotNull(annotations);
        Assert.assertEquals(2, annotations.length);
        Assert.assertEquals("first", annotations[0].value());
        Assert.assertEquals("second", annotations[1].value());
    }

    @Test
    public void testHasMetaAnnotation() {
        @MetaAnnotation
        @SampleAnnotation
        class SampleClass {}

        Assert.assertFalse(AnnotationUtils.hasMetaAnnotation(SampleClass.class.getAnnotation(MetaAnnotation.class), MetaAnnotation.class));
        Assert.assertTrue(AnnotationUtils.hasMetaAnnotation(SampleClass.class.getAnnotation(SampleAnnotation.class), MetaAnnotation.class));
    }

    @Test
    public void testToAnnotationWithAttributeValues() {
        Map<String, Object> values = Map.of("value", "test");
        SampleAnnotation annotation = AnnotationUtils.toAnnotation(SampleAnnotation.class, values);
        Assert.assertNotNull(annotation);
        Assert.assertEquals("test", annotation.value());
        Assert.assertEquals(SampleAnnotation.class, annotation.annotationType());


        SampleAnnotation annotation2 = AnnotationUtils.toAnnotation(SampleAnnotation.class, values);
        Assert.assertEquals(annotation, annotation2);
        Assert.assertEquals(annotation.hashCode(), annotation2.hashCode());
        Assert.assertEquals(annotation.toString(), annotation2.toString());

        @SampleAnnotation("test")
        class Example {}
        SampleAnnotation annotation3 = Example.class.getDeclaredAnnotation(SampleAnnotation.class);
        Assert.assertNotNull(annotation3);
        ValueMapAnnotationProxy proxy3 = (ValueMapAnnotationProxy) Proxy.getInvocationHandler(annotation);
        Assert.assertSame(values, proxy3.getMemberValues());
        Assert.assertEquals(annotation3.annotationType(), proxy3.getType());

        ValueMapAnnotationProxy proxy2 = (ValueMapAnnotationProxy) Proxy.getInvocationHandler(annotation2);
        Assert.assertEquals(proxy2, proxy3);
        Assert.assertEquals(proxy2.hashCode(), proxy3.hashCode());
        Assert.assertEquals(proxy2.toString(), proxy3.toString());
        Assert.assertEquals(proxy2.getMemberValues(), proxy3.getMemberValues());
    }

    @Test
    public void testToAnnotationWithValue() {
        SampleAnnotation annotation = AnnotationUtils.toAnnotation(SampleAnnotation.class, "test");
        Assert.assertNotNull(annotation);
        Assert.assertEquals("test", annotation.value());
    }

    @Test
    public void testGetAttributeValues() {
        @SampleAnnotation("test")
        class SampleClass {}

        Map<String, Object> attributeValues = AnnotationUtils.getAttributeValues(SampleClass.class.getAnnotation(SampleAnnotation.class));
        Assert.assertNotNull(attributeValues);
        Assert.assertEquals("test", attributeValues.get("value"));
    }

    @Test
    public void testGetAnnotationAttributes() {
        Method[] attributes = AnnotationUtils.getAnnotationAttributes(SampleAnnotation.class);
        Assert.assertNotNull(attributes);
        Assert.assertEquals(1, attributes.length);
        Assert.assertEquals("value", attributes[0].getName());
    }

    @Test
    public void testGetDeclaredRepeatableAnnotations() {
        @SampleRepeatableAnnotation("first")
        @SampleRepeatableAnnotation("second")
        class SampleClass {}
        class NoAnnotatedClass {}

        var annotations = AnnotationUtils.getDeclaredRepeatableAnnotations(SampleClass.class, SampleRepeatableAnnotation.class);
        Assert.assertNotNull(annotations);
        Assert.assertEquals(2, annotations.size());
        Assert.assertEquals("first", annotations.get(0).value());
        Assert.assertEquals("second", annotations.get(1).value());

        Assert.assertTrue(AnnotationUtils.getDeclaredRepeatableAnnotations(NoAnnotatedClass.class, SampleRepeatableAnnotation.class).isEmpty());
    }

    @Test
    public void testGetAnnotationFromRepeatableContainer() {

        @SampleRepeatableAnnotation("first")
        @SampleRepeatableAnnotation("second")
        class SampleClass {}
        var annotations = SampleClass.class.getAnnotation(SampleRepeatableAnnotations.class);
        Assert.assertNotNull(annotations);

        // 普通注解
        Assert.assertEquals(2, AnnotationUtils.getAnnotationFromRepeatableContainer(SampleRepeatableAnnotation.class, annotations).length);

        // 基于valueMap合成的注解
        var valueMapProxy = AnnotationUtils.toAnnotation(SampleRepeatableAnnotations.class, annotations.value());
        Assert.assertEquals(2, AnnotationUtils.getAnnotationFromRepeatableContainer(SampleRepeatableAnnotation.class, valueMapProxy).length);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface MetaAnnotation {
    }

    @MetaAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface SampleAnnotation {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Repeatable(SampleRepeatableAnnotations.class)
    public @interface SampleRepeatableAnnotation {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface SampleRepeatableAnnotations {
        SampleRepeatableAnnotation[] value();
    }
}
